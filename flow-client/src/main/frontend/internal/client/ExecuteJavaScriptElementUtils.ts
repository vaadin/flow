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
import { Console } from './Console';
import { PolymerUtils } from './PolymerUtils';
import { Reactive } from './flow/reactive/Reactive';

// Mirrors NodeFeatures + NodeProperties constants the helpers need.
const ELEMENT_DATA = 0;
const ELEMENT_PROPERTIES = 1;
const ELEMENT_CHILDREN = 2;
const TAG = 'tag';

// Mirrors UpdatableModelProperties.NODE_DATA_KEY so Java + TS callers reach
// the same slot via StateNode.setNodeData / getNodeData.
const UPDATABLE_MODEL_PROPERTIES_KEY = 'UpdatableModelProperties';

interface CustomElementWithProperties {
  constructor?: {
    properties?: Record<string, { value?: unknown } | undefined>;
  };
}

type StateNodeLike = {
  getId(): number;
  getDomNode(): unknown;
  getList(featureId: number): { length(): number; get(index: number): unknown };
  getMap(featureId: number): {
    hasPropertyValue(name: string): boolean;
    getProperty(name: string): { getValue(): unknown; setValue(v: unknown): void; syncToServer(v: unknown): void };
  };
  getTree(): {
    sendExistingElementAttachToServer(
      parent: StateNodeLike,
      requestedId: number,
      assignedId: number,
      tagName: string,
      index: number
    ): void;
    getRegistry(): { getExistingElementMap(): ExistingElementMapLike };
  };
  getNodeData<T = unknown>(key: string): T | null;
  setNodeData(key: string, value: unknown): void;
  addUnregisterListener(listener: (event: unknown) => void): void;
};

// Per-state-node map of initializer-id -> cleanup callback. Mirrors the Java
// @JsOverlay `initializerCleanups` map; lookup keys must match Java's double
// representation, hence a plain number.
const initializerCleanups = new Map<StateNodeLike, Map<number, () => void>>();

type ExistingElementMapLike = {
  getId(element: Element): number | null;
  add(serverSideId: number, element: Element): void;
};

type UpdatableModelPropertiesLike = { isUpdatableProperty(property: string): boolean };

type UpdatableModelPropertiesCtor = new (properties: string[]) => UpdatableModelPropertiesLike;

declare global {
  interface Window {
    Vaadin: {
      Flow?: {
        internal?: {
          client?: {
            flow?: { model?: { UpdatableModelProperties?: UpdatableModelPropertiesCtor } };
          };
        };
      };
    };
  }
}

function getUpdatableModelPropertiesCtor(): UpdatableModelPropertiesCtor {
  const ctor = window.Vaadin?.Flow?.internal?.client?.flow?.model?.UpdatableModelProperties;
  if (!ctor) {
    throw new Error('UpdatableModelProperties constructor is not registered in the GWT bridge');
  }
  return ctor;
}

/**
 * Helpers used by `ExecuteJavaScriptProcessor`'s per-execution context. The
 * Java class is a pure `@JsType(isNative=true)` facade onto this module.
 *
 * The `attachExistingElement` / `populateModelProperties` /
 * `registerUpdatableModelProperties` methods all reach into TS-migrated
 * collaborators (`StateNode`, `StateTree`, `MapProperty`, `PolymerUtils`,
 * `Reactive`, `ExistingElementMap`, `UpdatableModelProperties`) and use the
 * `UPDATABLE_MODEL_PROPERTIES_KEY` string constant that mirrors
 * `UpdatableModelProperties.NODE_DATA_KEY` so Java callers that use the same
 * constant find the same slot.
 */
export const ExecuteJavaScriptElementUtils = {
  isPropertyDefined(node: Node, property: string): boolean {
    const ctorProps = (node as CustomElementWithProperties).constructor?.properties;
    const descriptor = ctorProps?.[property];
    return !!descriptor && typeof descriptor.value !== 'undefined';
  },

  attachExistingElement(parent: StateNodeLike, previousSibling: Element | null, tagName: string, id: number): void {
    let existingElement: Element | null = null;
    const childNodes = ((parent.getDomNode() as { childNodes: ArrayLike<Node> }).childNodes ?? []) as ArrayLike<Node>;
    const indices = new Map<Node, number>();
    let afterSibling = previousSibling === null;
    let elementIndex = -1;

    for (let i = 0; i < childNodes.length; i++) {
      const node = childNodes[i];
      indices.set(node, i);
      if (node === previousSibling) {
        afterSibling = true;
      }
      if (afterSibling && hasTag(node, tagName)) {
        existingElement = node as Element;
        elementIndex = i;
        break;
      }
    }

    if (existingElement === null) {
      // Report a missing-element attach back to the server.
      parent.getTree().sendExistingElementAttachToServer(parent, id, -1, tagName, -1);
      return;
    }

    const list = parent.getList(ELEMENT_CHILDREN);
    let existingId: number | null = null;
    let childIndex = 0;
    for (let i = 0; i < list.length(); i++) {
      const stateNode = list.get(i) as StateNodeLike;
      const domNode = stateNode.getDomNode() as Node;
      const index = indices.get(domNode);
      if (index != null && index < elementIndex) {
        childIndex++;
      }
      if (domNode === existingElement) {
        existingId = stateNode.getId();
        break;
      }
    }

    existingId = getExistingIdOrUpdate(parent, id, existingElement, existingId);
    parent
      .getTree()
      .sendExistingElementAttachToServer(parent, id, existingId, (existingElement as Element).tagName, childIndex);
  },

  populateModelProperties(node: StateNodeLike, properties: string[]): void {
    const map = node.getMap(ELEMENT_PROPERTIES);
    if (node.getDomNode() == null) {
      // DOM node not ready yet -- wait for the custom element to be defined,
      // then re-attempt after the next reactive flush.
      const tag = node.getMap(ELEMENT_DATA).getProperty(TAG).getValue() as string;
      PolymerUtils.invokeWhenDefined(tag, () => {
        Reactive.addPostFlushListener(() => ExecuteJavaScriptElementUtils.populateModelProperties(node, properties));
      });
      return;
    }
    for (const property of properties) {
      populateModelProperty(node, map, property);
    }
  },

  registerUpdatableModelProperties(node: StateNodeLike, properties: string[]): void {
    if (properties.length === 0) {
      return;
    }
    const Ctor = getUpdatableModelPropertiesCtor();
    const data = new Ctor(properties);
    node.setNodeData(UPDATABLE_MODEL_PROPERTIES_KEY, data);
  },

  /**
   * Stores a cleanup callback for a JS initializer registered through
   * `Element.addJsInitializer`. If a callback was previously stored for the
   * same id it is invoked before being replaced (defensive against stale
   * state from a discarded DOM). On the first registration for a node, an
   * unregister listener is attached so any remaining cleanups are drained
   * when the node leaves the tree.
   */
  registerInitializer(node: StateNodeLike, id: number, cleanup: () => void): void {
    let entry = initializerCleanups.get(node);
    if (entry === undefined) {
      entry = new Map<number, () => void>();
      initializerCleanups.set(node, entry);
      node.addUnregisterListener(() => drainInitializers(node));
    }
    const existing = entry.get(id);
    // Install the new cleanup before invoking the previous one so a re-entrant
    // register/dispose call from inside the existing callback sees the new
    // state, not the stale entry.
    entry.set(id, cleanup);
    if (existing !== undefined) {
      invokeSafely(existing);
    }
  },

  /**
   * Disposes a previously registered JS initializer cleanup. No-op if the id
   * is unknown (e.g. the node has already been unregistered).
   */
  disposeInitializer(node: StateNodeLike, id: number): void {
    const entry = initializerCleanups.get(node);
    if (entry === undefined) {
      return;
    }
    const fn = entry.get(id);
    if (fn === undefined) {
      return;
    }
    entry.delete(id);
    invokeSafely(fn);
  }
};

function drainInitializers(node: StateNodeLike): void {
  const entry = initializerCleanups.get(node);
  if (entry === undefined) {
    return;
  }
  initializerCleanups.delete(node);
  entry.forEach((fn) => invokeSafely(fn));
}

function invokeSafely(fn: () => void): void {
  try {
    fn();
  } catch (e) {
    Console.error((e as Error).message);
  }
}

function hasTag(node: Node, tag: string): boolean {
  const tagged = node as unknown as { tagName?: string };
  return typeof tagged.tagName === 'string' && tagged.tagName.toLowerCase() === tag.toLowerCase();
}

function populateModelProperty(node: StateNodeLike, map: ReturnType<StateNodeLike['getMap']>, property: string): void {
  const domNode = node.getDomNode() as Node;
  if (!ExecuteJavaScriptElementUtils.isPropertyDefined(domNode, property)) {
    if (!map.hasPropertyValue(property)) {
      map.getProperty(property).setValue(null);
    }
    return;
  }
  const updatableProperties = node.getNodeData<UpdatableModelPropertiesLike>(UPDATABLE_MODEL_PROPERTIES_KEY);
  if (updatableProperties == null || !updatableProperties.isUpdatableProperty(property)) {
    return;
  }
  map.getProperty(property).syncToServer((domNode as unknown as Record<string, unknown>)[property]);
}

function getExistingIdOrUpdate(
  parent: StateNodeLike,
  serverSideId: number,
  existingElement: Element,
  existingId: number | null
): number {
  if (existingId !== null) {
    return existingId;
  }
  const map = parent.getTree().getRegistry().getExistingElementMap();
  const fromMap = map.getId(existingElement);
  if (fromMap == null) {
    map.add(serverSideId, existingElement);
    return serverSideId;
  }
  return fromMap;
}
