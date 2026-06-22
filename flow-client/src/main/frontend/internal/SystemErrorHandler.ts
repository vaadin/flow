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

// Implementations migrated from SystemErrorHandler.java, registered on
// window.Vaadin.Flow.internal.SystemErrorHandler by registerInternals; the Java
// methods delegate here. Also bundled to ES5 for the HtmlUnit used by GwtTests.

/**
 * Replaces every element with the given tag name by a shallow clone, after
 * mocking its disconnected callback. Used to detach stale components without
 * triggering their server-side disconnect handling.
 */
export function recreateNodes(elementName: string): void {
  // Snapshot the live collection before mutating it.
  const elements = Array.from(document.getElementsByTagName(elementName)) as Array<
    Element & {
      $server: { disconnected: () => void };
    }
  >;
  for (const elem of elements) {
    // Mock the disconnected callback so it does not throw a TypeError.
    elem.$server.disconnected = () => {};
    elem.parentNode?.replaceChild(elem.cloneNode(false), elem);
  }
}

/** Invokes the native showPopover() of the element if it supports it. */
export function showPopover(el: Element): void {
  const fn = el && (el as Element & { showPopover?: () => void }).showPopover;
  if (typeof fn === 'function') {
    fn.call(el);
  }
}

/** Returns the shadow root of the given host element, if any. */
export function getShadowRootElement(host: Element): ShadowRoot | null {
  return host.shadowRoot;
}
