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

import { hasOwnJsProperty } from '../../WidgetUtil';

/**
 * Access to what the generated bundle registers for the JavaScript that
 * `@JsDefinition` interfaces declare: a function per method that runs its
 * JavaScript when called, a function per method that declares a snippet, and,
 * outside production, what a developer wrote for each of them.
 */

type DeclaredFunction = (this: unknown, ...args: unknown[]) => unknown;

type DeclaredJavaScript = {
  jsDefinitions?: Record<string, DeclaredFunction>;
  jsSnippets?: Record<string, DeclaredFunction>;
  jsDefinitionNames?: Record<string, string>;
};

/**
 * Reads an entry of a registry by the identifier the server sent, only when
 * the registry has it as its own property, so that an identifier such as
 * `constructor` or `__proto__` does not resolve to something inherited.
 */
function getOwnEntry<T>(registry: Record<string, T> | undefined, functionId: string): T | undefined {
  return registry !== undefined && hasOwnJsProperty(registry, functionId) ? registry[functionId] : undefined;
}

function getDeclaredJavaScript(): DeclaredJavaScript {
  return (window as unknown as { Vaadin?: { Flow?: DeclaredJavaScript } }).Vaadin?.Flow ?? {};
}

/**
 * Looks up the function that the build generated for a method that runs its
 * JavaScript when called.
 */
export function findDeclaredFunction(functionId: string): DeclaredFunction | undefined {
  return getOwnEntry(getDeclaredJavaScript().jsDefinitions, functionId);
}

/**
 * Looks up the function that the build generated for a method that declares a
 * snippet, which takes the captures and returns the value of the snippet.
 */
export function findDeclaredSnippet(functionId: string): DeclaredFunction | undefined {
  return getOwnEntry(getDeclaredJavaScript().jsSnippets, functionId);
}

/**
 * What to call a function in a message: what a developer wrote, which a
 * development bundle registers next to the function itself, and the identifier
 * of the function when it does not, as in production.
 */
export function getNameOf(functionId: string): string {
  return getOwnEntry(getDeclaredJavaScript().jsDefinitionNames, functionId) ?? functionId;
}

/**
 * The message for a function that the bundle does not have.
 */
export function describeMissingFunction(functionId: string): string {
  return `No JavaScript in the bundle for ${getNameOf(functionId)}. The JavaScript definition is annotated with @JsDefinition, but the build did not collect it.`;
}
