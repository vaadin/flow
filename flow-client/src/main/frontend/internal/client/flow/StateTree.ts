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
import { StateNode } from './StateNode';

type RegistryLike = any;

type DomNodeLike = any;

// NodeFeatures ids that prepareForResync / getFeatureDebugName need; mirror
// the constants in com.vaadin.flow.internal.nodefeature.NodeFeatures.
const NF = {
  ELEMENT_DATA: 0,
  ELEMENT_PROPERTIES: 1,
  ELEMENT_CHILDREN: 2,
  ELEMENT_ATTRIBUTES: 3,
  ELEMENT_LISTENERS: 4,
  UI_PUSHCONFIGURATION: 5,
  UI_PUSHCONFIGURATION_PARAMETERS: 6,
  TEXT_NODE: 7,
  POLL_CONFIGURATION: 8,
  RECONNECT_DIALOG_CONFIGURATION: 9,
  LOADING_INDICATOR_CONFIGURATION: 10,
  CLASS_LIST: 11,
  ELEMENT_STYLE_PROPERTIES: 12,
  COMPONENT_MAPPING: 13,
  TEMPLATE_MODELLIST: 14,
  POLYMER_SERVER_EVENT_HANDLERS: 15,
  POLYMER_EVENT_LISTENERS: 16,
  CLIENT_DELEGATE_HANDLERS: 17,
  SHADOW_ROOT_DATA: 18,
  SHADOW_ROOT_HOST: 19,
  ATTACH_EXISTING_ELEMENT: 20,
  VIRTUAL_CHILDREN: 21,
  BASIC_TYPE_VALUE: 22
};
const VISIBLE_PROP = 'visible';

const FEATURE_DEBUG_NAMES: Record<number, string> = {
  [NF.ELEMENT_DATA]: 'elementData',
  [NF.ELEMENT_PROPERTIES]: 'elementProperties',
  [NF.ELEMENT_CHILDREN]: 'elementChildren',
  [NF.ELEMENT_ATTRIBUTES]: 'elementAttributes',
  [NF.ELEMENT_LISTENERS]: 'elementListeners',
  [NF.UI_PUSHCONFIGURATION]: 'pushConfiguration',
  [NF.UI_PUSHCONFIGURATION_PARAMETERS]: 'pushConfigurationParameters',
  [NF.TEXT_NODE]: 'textNode',
  [NF.POLL_CONFIGURATION]: 'pollConfiguration',
  [NF.RECONNECT_DIALOG_CONFIGURATION]: 'reconnectDialogConfiguration',
  [NF.LOADING_INDICATOR_CONFIGURATION]: 'loadingIndicatorConfiguration',
  [NF.CLASS_LIST]: 'classList',
  [NF.ELEMENT_STYLE_PROPERTIES]: 'elementStyleProperties',
  [NF.COMPONENT_MAPPING]: 'componentMapping',
  [NF.TEMPLATE_MODELLIST]: 'modelList',
  [NF.POLYMER_SERVER_EVENT_HANDLERS]: 'polymerServerEventHandlers',
  [NF.POLYMER_EVENT_LISTENERS]: 'polymerEventListenerMap',
  [NF.CLIENT_DELEGATE_HANDLERS]: 'clientDelegateHandlers',
  [NF.SHADOW_ROOT_DATA]: 'shadowRootData',
  [NF.SHADOW_ROOT_HOST]: 'shadowRootHost',
  [NF.ATTACH_EXISTING_ELEMENT]: 'attachExistingElementFeature',
  [NF.VIRTUAL_CHILDREN]: 'virtualChildrenList',
  [NF.BASIC_TYPE_VALUE]: 'basicTypeValue'
};

/**
 * Client-side representation of a server-side state tree. Migrated from
 * `com.vaadin.client.flow.StateTree`. State (idToNode, rootNode flags) is
 * here in TS; methods that dispatch into the Java Registry / ServerConnector
 * stay on the Java side as @JsOverlay helpers.
 */
export class StateTree {
  private readonly idToNode = new Map<number, StateNode>();
  private readonly rootNode: StateNode;
  readonly registry: RegistryLike;
  private updateInProgressFlag = false;
  private resyncFlag = false;

  constructor(registry: RegistryLike) {
    this.registry = registry;
    this.rootNode = new StateNode(1, this);
    this.registerNodeStateOnly(this.rootNode);
  }

  isUpdateInProgress(): boolean {
    return this.updateInProgressFlag;
  }

  setUpdateInProgressStateOnly(updateInProgress: boolean): void {
    this.updateInProgressFlag = updateInProgress;
  }

  registerNodeStateOnly(node: StateNode): void {
    const key = node.getId();
    this.idToNode.set(key, node);
  }

  unregisterNodeStateOnly(node: StateNode): void {
    this.idToNode.delete(node.getId());
  }

  isResync(): boolean {
    return this.resyncFlag;
  }

  setResync(resync: boolean): void {
    this.resyncFlag = resync;
  }

  getNode(id: number): StateNode | null {
    return this.idToNode.get(id) ?? null;
  }

  getRootNode(): StateNode {
    return this.rootNode;
  }

  getStateNodeForDomNode(domNode: DomNodeLike): StateNode | null {
    for (const stateNode of this.idToNode.values()) {
      const candidate = stateNode.getDomNode();
      if (candidate != null && candidate === domNode) {
        return stateNode;
      }
      if (
        candidate != null &&
        typeof (candidate as { isSameNode?: (n: unknown) => boolean }).isSameNode === 'function' &&
        (candidate as { isSameNode(n: unknown): boolean }).isSameNode(domNode)
      ) {
        return stateNode;
      }
    }
    return null;
  }

  forEachNode(callback: (node: StateNode) => void): void {
    this.idToNode.forEach((node) => callback(node));
  }

  isValidNode(node: StateNode | null): boolean {
    if (node == null) return false;
    if (node.getTree() !== this) return false;
    if (this.getNode(node.getId()) !== node) return false;
    return true;
  }

  isVisible(node: StateNode): boolean {
    if (!node.hasFeature(NF.ELEMENT_DATA)) {
      return true;
    }
    const visibilityMap = node.getMap(NF.ELEMENT_DATA);
    const visibility = visibilityMap.getProperty(VISIBLE_PROP).getValue();
    return visibility !== false;
  }

  isActive(node: StateNode): boolean {
    const visible = this.isVisible(node);
    if (!visible || node.getParent() == null) {
      return visible;
    }
    return this.isActive(node.getParent() as StateNode);
  }

  getFeatureDebugName(id: number): string {
    return FEATURE_DEBUG_NAMES[id] ?? `Unknown node feature: ${id}`;
  }

  getRegistry(): RegistryLike {
    return this.registry;
  }
}
