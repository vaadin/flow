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

// Implementations migrated from MessageHandler.java, registered on
// window.Vaadin.Flow.internal.MessageHandler by registerInternals; the Java
// methods delegate here. Also bundled to ES5 for the HtmlUnit used by GwtTests.

/** Removes the link and style elements with the given dependency id. */
export function removeStylesheetByIdFromDom(dependencyId: string): void {
  const elements = document.querySelectorAll(`link[data-id="${dependencyId}"], style[data-id="${dependencyId}"]`);
  for (const element of Array.from(elements)) {
    element.remove();
  }
}

/** Invokes the element's afterServerUpdate callback if it defines one. */
export function callAfterServerUpdates(node: Node): void {
  const target = node as unknown as { afterServerUpdate?: () => void };
  if (node && target.afterServerUpdate) {
    target.afterServerUpdate();
  }
}

/** Milliseconds from the navigation response start to now, or -1 if unknown. */
export function calculateBootstrapTime(): number {
  const perf = window.performance as (Performance & { timing?: { responseStart: number } }) | undefined;
  if (perf && perf.timing) {
    return Date.now() - perf.timing.responseStart;
  }
  return -1;
}

/** Parses a JSON UIDL response into a ValueMap-compatible object. */
export function parseJSONResponse(jsonText: string): unknown {
  return JSON.parse(jsonText);
}

/** The navigation fetchStart timestamp, or 0 if unknown. */
export function getFetchStartTime(): number {
  const perf = window.performance as (Performance & { timing?: { fetchStart?: number } }) | undefined;
  if (perf && perf.timing && perf.timing.fetchStart) {
    return perf.timing.fetchStart;
  }
  return 0;
}
