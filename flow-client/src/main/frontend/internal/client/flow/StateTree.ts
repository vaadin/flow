/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// TypeScript port of com.vaadin.client.flow.StateTree, on top of the TS state
// node and node features. The slices of the Registry and server-communication
// layer that StateTree needs are declared here as contracts that the
// Registry/connector satisfy. ServerEventObject lookup (used only during resync)
// is injected, defaulting to "no server event object".

import { assert } from '../../assert';
import type { MapProperty } from './nodefeature/MapProperty';
import { NodeList } from './nodefeature/NodeList';
import type { NodeMap } from './nodefeature/NodeMap';
import { NodeFeatures } from '../../flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../../flow/internal/nodefeature/NodeProperties';
import { StateNode } from './StateNode';
import { Console } from '../Console';

/** The slice of ServerConnector that StateTree uses. */
export interface ServerConnector {
  sendEventMessage(node: StateNode, eventType: string, eventData: unknown): void;
  sendNodeSyncMessage(node: StateNode, mapId: number, name: string, value: unknown): void;
  sendTemplateEventMessage(node: StateNode, methodName: string, args: unknown[], promiseId: number): void;
  sendExistingElementAttachToServer(
    parent: StateNode,
    requestedId: number,
    assignedId: number,
    tagName: string,
    index: number
  ): void;
  sendExistingElementWithIdAttachToServer(parent: StateNode, requestedId: number, assignedId: number, id: string): void;
  sendReturnChannelMessage(stateNodeId: number, channelId: number, args: unknown[]): void;
}

/** The slice of InitialPropertiesHandler that StateTree uses. */
export interface InitialPropertiesHandler {
  flushPropertyUpdates(): void;
  nodeRegistered(node: StateNode): void;
  handlePropertyUpdate(property: MapProperty): boolean;
}

/** The slice of Registry that StateTree uses. */
export interface Registry {
  getInitialPropertiesHandler(): InitialPropertiesHandler;
  getServerConnector(): ServerConnector;
}

/** Looks up a server event object attached to a DOM node; mirrors ServerEventObject.getIfPresent. */
export type ServerEventObjectAccess = (dom: Node) => { rejectPromises(): void } | null;

/**
 * A client-side representation of a server-side state tree.
 */
export class StateTree {
  readonly #idToNode = new Map<number, StateNode>();

  readonly #registry: Registry;

  readonly #serverEventObjectAccess: ServerEventObjectAccess;

  readonly #rootNode: StateNode;

  #nodeFeatureDebugName: Map<number, string> | null = null;

  #updateInProgress = false;

  #resync = false;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   * @param serverEventObjectAccess - looks up a server event object attached to
   *          a DOM node during resync; port deviation for the not-yet-ported
   *          ServerEventObject, defaulting to "no server event object"
   */
  constructor(registry: Registry, serverEventObjectAccess: ServerEventObjectAccess = () => null) {
    this.#registry = registry;
    this.#serverEventObjectAccess = serverEventObjectAccess;
    this.#rootNode = new StateNode(1, this);
    this.registerNode(this.#rootNode);
  }

  /**
   * Mark this tree as being updated.
   *
   * @param updateInProgress - `true` if the tree is being updated, `false` if
   *          not
   * @see {@link isUpdateInProgress}
   */
  setUpdateInProgress(updateInProgress: boolean): void {
    assert(
      this.#updateInProgress !== updateInProgress,
      `Inconsistent state tree updating status, expected ${updateInProgress ? 'no ' : ''} updates in progress.`
    );
    this.#updateInProgress = updateInProgress;
    this.getRegistry().getInitialPropertiesHandler().flushPropertyUpdates();
  }

  /**
   * Returns whether this tree is currently being updated by
   * `TreeChangeProcessor.processChanges`.
   *
   * @returns `true` if being updated, `false` if not
   */
  isUpdateInProgress(): boolean {
    return this.#updateInProgress;
  }

  /**
   * Registers a node with this tree.
   *
   * @param node - the node to register
   */
  registerNode(node: StateNode): void {
    assert(node.getTree() === this, 'Node is not created for this tree');
    assert(!node.isUnregistered(), "Can't re-register a node");
    assert(!this.#idToNode.has(node.getId()), `Node ${node.getId()} is already registered`);

    this.#idToNode.set(node.getId(), node);

    if (this.isUpdateInProgress()) {
      this.getRegistry().getInitialPropertiesHandler().nodeRegistered(node);
    }
  }

  /**
   * Unregisters a node from this tree. Once the node has been unregistered, it
   * can't be registered again.
   *
   * @param node - the node to unregister
   */
  unregisterNode(node: StateNode): void {
    assert(this.#assertValidNode(node), 'Invalid node');
    assert(node !== this.#rootNode, "Root node can't be unregistered");
    this.#idToNode.delete(node.getId());
    node.unregister();
  }

  /**
   * Unregisters all nodes except root from this tree, and clears the root's
   * features. Use to reset the tree in preparation for rebuilding it in in a
   * resynchronization response.
   */
  prepareForResync(): void {
    this.#rootNode.getList(NodeFeatures.VIRTUAL_CHILDREN).forEach((sn) => this.#clearLists(sn as StateNode));
    this.#clearLists(this.#rootNode);

    this.#idToNode.forEach((node) => {
      if (node !== this.#rootNode) {
        const dom = node.getDomNode();
        if (dom !== null) {
          const serverEventObject = this.#serverEventObjectAccess(dom);
          if (serverEventObject !== null) {
            // reject any promise waiting on this node
            serverEventObject.rejectPromises();
          }
        }
        this.unregisterNode(node);
        node.setParent(null);
      }
    });
    this.setResync(true);
  }

  /**
   * Check if tree is resynchronizing after a {@link prepareForResync}
   *
   * @returns true if resync called
   */
  isResync(): boolean {
    return this.#resync;
  }

  /**
   * Set the resynchronization state for the StateTree.
   *
   * @param resync - resynchronization state to set
   */
  setResync(resync: boolean): void {
    this.#resync = resync;
  }

  /**
   * Returns the state node in the tree for the given dom node or `null` if none
   * found.
   *
   * Comparison is done with Node.isSameNode() method which is same as `===`
   * comparison.
   *
   * @param domNode - the dom node to find state node for
   * @returns the state node or null
   */
  getStateNodeForDomNode(domNode: Node): StateNode | null {
    for (const stateNode of this.#idToNode.values()) {
      if (domNode.isSameNode(stateNode.getDomNode())) {
        return stateNode;
      }
    }
    return null;
  }

  #clearLists(stateNode: StateNode): void {
    stateNode.forEachFeature((feature, featureId) => {
      if (feature instanceof NodeList) {
        if (featureId === NodeFeatures.ELEMENT_CHILDREN) {
          // splice() instead of clear() to preserve auxiliary DOM nodes
          // (loading indicator and <noscript>)
          feature.splice(0, feature.length());
        } else {
          feature.clear();
        }
      }
    });
  }

  /**
   * Verifies that the provided node is not null and properly registered with
   * this state tree.
   *
   * @param node - the node to test
   * @returns always `true`, for use with the `assert` helper
   */
  #assertValidNode(node: StateNode | null): boolean {
    assert(node !== null, 'Node is null');
    assert(node.getTree() === this, 'Node is not created for this tree');
    assert(node === this.getNode(node.getId()), 'Node id is not registered with this tree');

    return true;
  }

  /**
   * Validates that the provided node is not null and is properly registered for
   * this state tree.
   *
   * Logs a warning if there was a problem with the node.
   *
   * @param node - node to test
   * @returns node is valid
   */
  #isValidNode(node: StateNode | null): boolean {
    let isValid = true;
    if (node === null) {
      Console.warn('Node is null');
      isValid = false;
    } else if (node.getTree() !== this) {
      Console.warn('Node is not created for this tree');
      isValid = false;
    } else if (node !== this.getNode(node.getId())) {
      Console.warn('Node id is not registered with this tree');
      isValid = false;
    }
    return isValid;
  }

  /**
   * Finds the node with the given id.
   *
   * @param id - the id
   * @returns the node with the given id, or `null` if no such node is
   *          registered.
   */
  getNode(id: number): StateNode | null {
    return this.#idToNode.get(id) ?? null;
  }

  /**
   * Gets the root node of this tree.
   *
   * @returns the root node
   */
  getRootNode(): StateNode {
    return this.#rootNode;
  }

  /**
   * Sends an event to the server.
   *
   * @param node - the node that listened to the event
   * @param eventType - the type of event
   * @param eventData - extra data associated with the event
   */
  sendEventToServer(node: StateNode, eventType: string, eventData: unknown): void {
    if (this.#isValidNode(node)) {
      this.#registry.getServerConnector().sendEventMessage(node, eventType, eventData);
    }
  }

  /**
   * Sends a map property sync to the server.
   *
   * @param property - the property that should have its value synced to the
   *          server, not `null`
   */
  sendNodePropertySyncToServer(property: MapProperty): void {
    const nodeMap = property.getMap() as NodeMap;
    const node = nodeMap.getNode() as StateNode;

    if (this.getRegistry().getInitialPropertiesHandler().handlePropertyUpdate(property) || !this.#isValidNode(node)) {
      return;
    }

    this.#registry
      .getServerConnector()
      .sendNodeSyncMessage(node, nodeMap.getId(), property.getName(), property.getValue());
  }

  /**
   * Sends a request to call server side method with `methodName` using
   * `argsArray` as argument values.
   *
   * In cases when the state tree has been changed and we receive a delayed or
   * deferred template event the event is just ignored.
   *
   * @param node - the node referring to the server side instance containing the
   *          method
   * @param methodName - the method name
   * @param argsArray - the arguments array for the method
   * @param promiseId - the promise id to use for getting the result back, or -1
   *          if no result is expected
   */
  sendTemplateEventToServer(node: StateNode, methodName: string, argsArray: unknown[], promiseId: number): void {
    if (this.#isValidNode(node)) {
      this.#registry.getServerConnector().sendTemplateEventMessage(node, methodName, argsArray, promiseId);
    }
  }

  /**
   * Sends a data for attach existing element server side callback.
   *
   * @param parent - parent of the node to attach
   * @param requestedId - originally requested id of a server side node
   * @param assignedId - identifier which should be used on the server side for
   *          the element (instead of requestedId)
   * @param tagName - the requested tagName
   * @param index - the index of the element on the server side
   */
  // eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java sendExistingElementAttachToServer signature
  sendExistingElementAttachToServer(
    parent: StateNode,
    requestedId: number,
    assignedId: number,
    tagName: string,
    index: number
  ): void {
    assert(this.#assertValidNode(parent), 'Invalid node');
    this.#registry
      .getServerConnector()
      .sendExistingElementAttachToServer(parent, requestedId, assignedId, tagName, index);
  }

  /**
   * Sends a data for attach existing element with id server side callback.
   *
   * @param parent - parent of the node to attach
   * @param requestedId - originally requested id of a server side node
   * @param assignedId - identifier which should be used on the server side for
   *          the element (instead of requestedId)
   * @param id - id of requested element
   */
  sendExistingElementWithIdAttachToServer(
    parent: StateNode,
    requestedId: number,
    assignedId: number,
    id: string
  ): void {
    assert(this.#assertValidNode(parent), 'Invalid node');
    this.#registry.getServerConnector().sendExistingElementWithIdAttachToServer(parent, requestedId, assignedId, id);
  }

  /**
   * Gets the {@link Registry} that this state tree belongs to.
   *
   * @returns the registry of this tree, not `null`
   */
  getRegistry(): Registry {
    return this.#registry;
  }

  /**
   * Returns the visibility state of the `node`.
   *
   * @param node - the node whose visibility is tested
   * @returns `true` is the node is visible, `false` otherwise
   */
  isVisible(node: StateNode): boolean {
    if (!node.hasFeature(NodeFeatures.ELEMENT_DATA)) {
      return true;
    }
    const visibilityMap = node.getMap(NodeFeatures.ELEMENT_DATA);
    const visibility = visibilityMap.getProperty(NodeProperties.VISIBLE).getValue();

    // Absence of value or "true" means that the node should be visible. So only
    // "false" means "hide".
    return visibility !== false;
  }

  /**
   * Checks whether the `node` is active.
   *
   * The node is active if it's visible and all its ancestors are visible.
   *
   * @param node - the node whose activity is tested
   * @returns `true` is the node is active, `false` otherwise
   */
  isActive(node: StateNode): boolean {
    const visible = this.isVisible(node);
    const parent = node.getParent();
    if (!visible || parent === null) {
      return visible;
    }
    return this.isActive(parent);
  }

  /**
   * Returns a human readable string for the name space with the given id.
   *
   * Package-private in Java; exported here only because TypeScript has no
   * package-private visibility and the same-package `StateNode` needs it. Not
   * public API.
   *
   * @param id - the node feature id
   * @returns a human readable string describing the node feature
   * @internal
   */
  getFeatureDebugName(id: number): string {
    if (this.#nodeFeatureDebugName === null) {
      const names = new Map<number, string>();
      names.set(NodeFeatures.ELEMENT_DATA, 'elementData');
      names.set(NodeFeatures.ELEMENT_PROPERTIES, 'elementProperties');
      names.set(NodeFeatures.ELEMENT_CHILDREN, 'elementChildren');
      names.set(NodeFeatures.ELEMENT_ATTRIBUTES, 'elementAttributes');
      names.set(NodeFeatures.ELEMENT_LISTENERS, 'elementListeners');
      names.set(NodeFeatures.UI_PUSHCONFIGURATION, 'pushConfiguration');
      names.set(NodeFeatures.UI_PUSHCONFIGURATION_PARAMETERS, 'pushConfigurationParameters');
      names.set(NodeFeatures.TEXT_NODE, 'textNode');
      names.set(NodeFeatures.POLL_CONFIGURATION, 'pollConfiguration');
      names.set(NodeFeatures.RECONNECT_DIALOG_CONFIGURATION, 'reconnectDialogConfiguration');
      names.set(NodeFeatures.LOADING_INDICATOR_CONFIGURATION, 'loadingIndicatorConfiguration');
      names.set(NodeFeatures.CLASS_LIST, 'classList');
      names.set(NodeFeatures.ELEMENT_STYLE_PROPERTIES, 'elementStyleProperties');
      names.set(NodeFeatures.COMPONENT_MAPPING, 'componentMapping');
      names.set(NodeFeatures.TEMPLATE_MODELLIST, 'modelList');
      names.set(NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS, 'polymerServerEventHandlers');
      names.set(NodeFeatures.POLYMER_EVENT_LISTENERS, 'polymerEventListenerMap');
      names.set(NodeFeatures.CLIENT_DELEGATE_HANDLERS, 'clientDelegateHandlers');
      names.set(NodeFeatures.SHADOW_ROOT_DATA, 'shadowRootData');
      names.set(NodeFeatures.SHADOW_ROOT_HOST, 'shadowRootHost');
      names.set(NodeFeatures.ATTACH_EXISTING_ELEMENT, 'attachExistingElementFeature');
      names.set(NodeFeatures.VIRTUAL_CHILDREN, 'virtualChildrenList');
      names.set(NodeFeatures.BASIC_TYPE_VALUE, 'basicTypeValue');
      this.#nodeFeatureDebugName = names;
    }
    return this.#nodeFeatureDebugName.get(id) ?? `Unknown node feature: ${id}`;
  }
}
