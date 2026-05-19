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

export interface ContextExecutionCallbacks {
  /** Resolves a script argument (typically an element) to its StateNode. */
  nodeParameters: Map<unknown, unknown>;
  /** Pre-cleaned application id used as `$appId` on the returned object. */
  appId: string;
  /** Opaque Java Registry object exposed via `object.registry`. */
  registry: unknown;
  /** Java's `ExecuteJavaScriptElementUtils.attachExistingElement`. */
  attachExisting: (parent: unknown, previousSibling: unknown, tagName: string, id: number) => void;
  /** Java's `ExecuteJavaScriptElementUtils.populateModelProperties`. */
  populateModel: (node: unknown, properties: unknown) => void;
  /** Java's `ExecuteJavaScriptElementUtils.registerUpdatableModelProperties`. */
  registerUpdatable: (node: unknown, properties: unknown) => void;
  /** Java callback invoked when user JS calls `object.stopApplication()`. */
  stopApplication: () => void;
  /** Java's `ExecuteJavaScriptElementUtils.registerInitializer`. */
  registerInitializer: (node: unknown, id: number, cleanup: () => void) => void;
  /** Java's `ExecuteJavaScriptElementUtils.disposeInitializer`. */
  disposeInitializer: (node: unknown, id: number) => void;
}

/**
 * Builds the per-execution `this`-object that user JS sees when Flow runs
 * `executeJs` payloads. Migrated from the JSNI body of
 * `com.vaadin.client.flow.ExecuteJavaScriptProcessor#getContextExecutionObject`.
 *
 * The Java caller supplies a node-parameter `Map` and seven callbacks; the
 * object's `getNode` does the lookup, and the three element-utility methods
 * resolve `parent`/`element` to a StateNode before delegating.
 */
export const ExecuteJavaScriptProcessor = {
  // eslint-disable-next-line @typescript-eslint/max-params
  getContextExecutionObject(
    nodeParameters: Map<unknown, unknown>,
    appId: string,
    registry: unknown,
    attachExisting: (parent: unknown, previousSibling: unknown, tagName: string, id: number) => void,
    populateModel: (node: unknown, properties: unknown) => void,
    registerUpdatable: (node: unknown, properties: unknown) => void,
    stopApplication: () => void,
    registerInitializer: (node: unknown, id: number, cleanup: () => void) => void,
    disposeInitializer: (node: unknown, id: number) => void
  ): object {
    const getNode = (element: unknown): unknown => {
      const node = nodeParameters.get(element);
      if (node == null) {
        throw new ReferenceError('There is no a StateNode for the given argument.');
      }
      return node;
    };
    return {
      getNode,
      $appId: appId,
      registry,
      attachExistingElement(parent: unknown, previousSibling: unknown, tagName: string, id: number) {
        attachExisting(getNode(parent), previousSibling, tagName, id);
      },
      populateModelProperties(element: unknown, properties: unknown) {
        populateModel(getNode(element), properties);
      },
      registerUpdatableModelProperties(element: unknown, properties: unknown) {
        registerUpdatable(getNode(element), properties);
      },
      stopApplication() {
        stopApplication();
      },
      registerInitializer(node: unknown, id: number, cleanup: () => void) {
        registerInitializer(node, id, cleanup);
      },
      disposeInitializer(node: unknown, id: number) {
        disposeInitializer(node, id);
      }
    };
  }
};
