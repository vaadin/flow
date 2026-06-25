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

// TypeScript port of com.vaadin.client.flow.StateTree, built alongside the Java
// version on top of the TS state node and node features. The Registry and
// server-communication layer are not ported yet, so the slices StateTree needs
// are declared here as contracts that the future TS Registry/connector will
// satisfy at cutover. ServerEventObject lookup (used only during resync) is
// injected, defaulting to "no server event object".

import { assert } from './assert';
import type { MapProperty } from './nodefeature/MapProperty';
import { NodeList } from './nodefeature/NodeList';
import type { NodeMap } from './nodefeature/NodeMap';
import { NodeFeatures, NodeProperties } from './nodefeature/NodeFeatures';
import { StateNode, type StateTree as StateTreeContract } from './StateNode';

/** Details of an existing-element-attach request; mirrors the positional args in Java. */
export interface ExistingElementAttach {
  requestedId: number;
  assignedId: number;
  tagName: string;
  index: number;
}

/** The slice of ServerConnector that StateTree uses. */
export interface ServerConnector {
  sendEventMessage(node: StateNode, eventType: string, eventData: unknown): void;
  sendNodeSyncMessage(node: StateNode, mapId: number, name: string, value: unknown): void;
  sendTemplateEventMessage(node: StateNode, methodName: string, args: unknown[], promiseId: number): void;
  sendExistingElementAttachToServer(parent: StateNode, attach: ExistingElementAttach): void;
  sendExistingElementWithIdAttachToServer(parent: StateNode, requestedId: number, assignedId: number, id: string): void;
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

/** A client-side representation of a server-side state tree; mirrors StateTree.java. */
export class StateTree implements StateTreeContract {
  private readonly idToNode = new Map<number, StateNode>();

  private readonly registry: Registry;

  private readonly serverEventObjectAccess: ServerEventObjectAccess;

  private readonly rootNode: StateNode;

  private nodeFeatureDebugName: Map<number, string> | null = null;

  private updateInProgress = false;

  private resync = false;

  constructor(registry: Registry, serverEventObjectAccess: ServerEventObjectAccess = () => null) {
    this.registry = registry;
    this.serverEventObjectAccess = serverEventObjectAccess;
    this.rootNode = new StateNode(1, this);
    this.registerNode(this.rootNode);
  }

  setUpdateInProgress(updateInProgress: boolean): void {
    this.updateInProgress = updateInProgress;
    this.getRegistry().getInitialPropertiesHandler().flushPropertyUpdates();
  }

  isUpdateInProgress(): boolean {
    return this.updateInProgress;
  }

  registerNode(node: StateNode): void {
    this.idToNode.set(node.getId(), node);

    if (this.isUpdateInProgress()) {
      this.getRegistry().getInitialPropertiesHandler().nodeRegistered(node);
    }
  }

  unregisterNode(node: StateNode): void {
    assert(node !== this.rootNode, "Root node can't be unregistered");
    this.idToNode.delete(node.getId());
    node.unregister();
  }

  prepareForResync(): void {
    this.rootNode.getList(NodeFeatures.VIRTUAL_CHILDREN).forEach((sn) => this.clearLists(sn as StateNode));
    this.clearLists(this.rootNode);

    this.idToNode.forEach((node) => {
      if (node !== this.rootNode) {
        const dom = node.getDomNode();
        if (dom !== null) {
          const serverEventObject = this.serverEventObjectAccess(dom);
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

  isResync(): boolean {
    return this.resync;
  }

  setResync(resync: boolean): void {
    this.resync = resync;
  }

  getStateNodeForDomNode(domNode: Node): StateNode | null {
    for (const stateNode of this.idToNode.values()) {
      if (domNode.isSameNode(stateNode.getDomNode())) {
        return stateNode;
      }
    }
    return null;
  }

  private clearLists(stateNode: StateNode): void {
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

  private isValidNode(node: StateNode | null): boolean {
    let isValid = true;
    if (node === null) {
      console.warn('Node is null');
      isValid = false;
    } else if (node.getTree() !== this) {
      console.warn('Node is not created for this tree');
      isValid = false;
    } else if (node !== this.getNode(node.getId())) {
      console.warn('Node id is not registered with this tree');
      isValid = false;
    }
    return isValid;
  }

  getNode(id: number): StateNode | null {
    return this.idToNode.get(id) ?? null;
  }

  getRootNode(): StateNode {
    return this.rootNode;
  }

  sendEventToServer(node: StateNode, eventType: string, eventData: unknown): void {
    if (this.isValidNode(node)) {
      this.registry.getServerConnector().sendEventMessage(node, eventType, eventData);
    }
  }

  sendNodePropertySyncToServer(property: MapProperty): void {
    const nodeMap = property.getMap() as NodeMap;
    const node = nodeMap.getNode() as StateNode;

    if (this.getRegistry().getInitialPropertiesHandler().handlePropertyUpdate(property) || !this.isValidNode(node)) {
      return;
    }

    this.registry
      .getServerConnector()
      .sendNodeSyncMessage(node, nodeMap.getId(), property.getName(), property.getValue());
  }

  sendTemplateEventToServer(node: StateNode, methodName: string, argsArray: unknown[], promiseId: number): void {
    if (this.isValidNode(node)) {
      this.registry.getServerConnector().sendTemplateEventMessage(node, methodName, argsArray, promiseId);
    }
  }

  sendExistingElementAttachToServer(parent: StateNode, attach: ExistingElementAttach): void {
    this.registry.getServerConnector().sendExistingElementAttachToServer(parent, attach);
  }

  sendExistingElementWithIdAttachToServer(
    parent: StateNode,
    requestedId: number,
    assignedId: number,
    id: string
  ): void {
    this.registry.getServerConnector().sendExistingElementWithIdAttachToServer(parent, requestedId, assignedId, id);
  }

  getRegistry(): Registry {
    return this.registry;
  }

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

  isActive(node: StateNode): boolean {
    const visible = this.isVisible(node);
    const parent = node.getParent();
    if (!visible || parent === null) {
      return visible;
    }
    return this.isActive(parent);
  }

  getFeatureDebugName(id: number): string {
    if (this.nodeFeatureDebugName === null) {
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
      this.nodeFeatureDebugName = names;
    }
    return this.nodeFeatureDebugName.get(id) ?? `Unknown node feature: ${id}`;
  }
}
