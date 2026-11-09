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

/**
 * Event-data expressions that are plain property paths, such as
 * `event.detail.value`, read without compiling them, so that they work under a
 * Content Security Policy without `unsafe-eval`. The server does not report
 * these paths as JavaScript sent as a string, see `ElementListenerMap`.
 */

/**
 * `event` or `element`, followed by any number of `.name` or `?.name`
 * segments. The same pattern as `ElementListenerMap.PROPERTY_PATH` on the
 * server.
 */
const PROPERTY_PATH = /^(event|element)((?:\??\.[A-Za-z_$][\w$]*)*)$/;

const SEGMENT = /(\?)?\.([A-Za-z_$][\w$]*)/g;

/**
 * Creates a function that reads the given property path from the event or the
 * element, or returns `undefined` when the expression is not such a path. The
 * function returns what the compiled expression would: like it, it throws a
 * `TypeError` when a `.name` segment is read from `null` or `undefined`, and
 * returns `undefined` for the whole path when a `?.name` segment is.
 */
export function createPropertyPathExpression(
  expression: string
): ((event: unknown, element: unknown) => unknown) | undefined {
  const match = PROPERTY_PATH.exec(expression);
  if (match === null) {
    return undefined;
  }
  const fromEvent = match[1] === 'event';
  const segments = [...match[2].matchAll(SEGMENT)].map(([, optional, name]) => ({ optional: optional === '?', name }));
  return (event, element) => {
    let value = fromEvent ? event : element;
    for (const { optional, name } of segments) {
      if (optional && (value === null || value === undefined)) {
        return undefined;
      }
      value = (value as Record<string, unknown>)[name];
    }
    return value;
  };
}
