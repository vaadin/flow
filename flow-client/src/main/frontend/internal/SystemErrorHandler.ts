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

/**
 * Builds and shows the system error notification for an unrecoverable error.
 * Each provided part (caption, message, details) becomes a labelled div and is
 * also reported through the logError callback (which keeps the Java Console's
 * production-mode gating). When a querySelector is given the notification is
 * placed inside the matching element (its shadow root if it has one); otherwise
 * it is appended to the document body. The container is always shown and
 * returned, even when a querySelector matched no element (it is then left
 * unattached), matching the original behaviour the caller relies on.
 */
// eslint-disable-next-line @typescript-eslint/max-params -- positional JSNI delegation mirrors the Java signature plus the log callback
export function handleError(
  caption: string | null,
  message: string | null,
  details: string | null,
  querySelector: string | null,
  logError: (text: string) => void
): Element {
  const systemErrorContainer = document.createElement('div');
  // Set the popover attribute for native popovers.
  systemErrorContainer.setAttribute('popover', 'manual');
  systemErrorContainer.className = 'v-system-error';

  const appendPart = (text: string | null, partClassName: string): void => {
    if (text !== null) {
      const partDiv = document.createElement('div');
      partDiv.className = partClassName;
      partDiv.textContent = text;
      systemErrorContainer.appendChild(partDiv);
      logError(text);
    }
  };
  appendPart(caption, 'caption');
  appendPart(message, 'message');
  appendPart(details, 'details');

  if (querySelector !== null) {
    const baseElement = document.querySelector(querySelector);
    // If the querySelector matches no element on the page the notification is
    // left unattached (and thus not displayed), but is still returned.
    if (baseElement !== null) {
      // If the base element has a shadow root, add the notification to the
      // shadow root; otherwise add it to the base element.
      (getShadowRootElement(baseElement) ?? baseElement).appendChild(systemErrorContainer);
    }
  } else {
    document.body.appendChild(systemErrorContainer);
  }
  showPopover(systemErrorContainer);

  return systemErrorContainer;
}
