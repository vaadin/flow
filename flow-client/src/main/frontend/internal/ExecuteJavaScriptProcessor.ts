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

// The executeJs context-object builder migrated from
// ExecuteJavaScriptProcessor.java, registered on
// window.Vaadin.Flow.internal.ExecuteJavaScriptProcessor by registerInternals;
// the Java method delegates here. The callbacks are supplied from the Java side
// already wrapped in $entry (so exceptions thrown asynchronously from the
// executed script still reach GWT's uncaught-exception handler). The element ->
// node resolution (getNode) keeps its $entry boundary on the Java side; here we
// only assemble the object the executed script runs against. Also bundled to
// ES5 for the HtmlUnit used by GwtTests.

// The $entry-wrapped callbacks the executed script invokes via its context.
// attachExistingElement / populateModelProperties / registerUpdatableModelProperties
// take a resolved StateNode (looked up via getNode); the rest take their
// arguments directly.
interface ContextCallbacks {
  getNode: (element: unknown) => unknown;
  attachExistingElement: (node: unknown, previousSibling: unknown, tagName: unknown, id: unknown) => void;
  populateModelProperties: (node: unknown, properties: unknown) => void;
  registerUpdatableModelProperties: (node: unknown, properties: unknown) => void;
  stopApplication: () => void;
  registerInitializer: (node: unknown, id: unknown, cleanup: unknown) => void;
  disposeInitializer: (node: unknown, id: unknown) => void;
}

/**
 * Builds the object the executed JavaScript runs against (its `this`). The
 * application id has its trailing per-UI suffix (`-<number>`) stripped so the
 * script sees the stable app id.
 */
export function getContextExecutionObject(
  appId: string,
  registry: unknown,
  callbacks: ContextCallbacks
): Record<string, unknown> {
  const object: Record<string, unknown> = {};
  object.getNode = callbacks.getNode;
  object.$appId = appId.replace(/-\d+$/, '');
  object.registry = registry;
  object.attachExistingElement = (parent: unknown, previousSibling: unknown, tagName: unknown, id: unknown): void =>
    callbacks.attachExistingElement(callbacks.getNode(parent), previousSibling, tagName, id);
  object.populateModelProperties = (element: unknown, properties: unknown): void =>
    callbacks.populateModelProperties(callbacks.getNode(element), properties);
  object.registerUpdatableModelProperties = (element: unknown, properties: unknown): void =>
    callbacks.registerUpdatableModelProperties(callbacks.getNode(element), properties);
  object.stopApplication = callbacks.stopApplication;
  object.registerInitializer = callbacks.registerInitializer;
  object.disposeInitializer = callbacks.disposeInitializer;
  return object;
}
