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

// Implementations migrated from LitUtils.java.

/** Checks whether the given element is a LitElement (duck-typed). */
export function isLitElement(element: Node): boolean {
  const el = element as unknown as Record<string, unknown>;
  return (
    typeof el.update === 'function' &&
    el.updateComplete instanceof Promise &&
    typeof el.shouldUpdate === 'function' &&
    typeof el.firstUpdated === 'function'
  );
}

/**
 * Invokes the callback once the given Lit element has rendered at least once.
 */
export function whenRendered(element: Element, callback: () => void): void {
  const el = element as unknown as { updateComplete: Promise<unknown> };
  void el.updateComplete.then(callback);
}
