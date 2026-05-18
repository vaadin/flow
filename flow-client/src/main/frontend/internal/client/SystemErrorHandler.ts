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

interface PopoverElement extends Element {
  showPopover?: () => void;
}

interface ServerHookedElement extends Element {
  $server?: { disconnected?: () => void };
}

/**
 * Browser-touching helpers migrated from
 * `com.vaadin.client.SystemErrorHandler`. Reached from GWT code via the
 * `NativeSystemErrorHandler` JsType shim. The rendering and error-routing
 * logic stays in `SystemErrorHandler.java`.
 */
export const SystemErrorHandler = {
  recreateNodes(elementName: string): void {
    const elements = document.getElementsByTagName(elementName);
    for (const elem of Array.from(elements) as ServerHookedElement[]) {
      if (elem.$server) {
        // Mock disconnected callback to avoid TypeError when the placeholder
        // is replaced before the real $server has been installed.
        elem.$server.disconnected = () => {};
      }
      elem.parentNode?.replaceChild(elem.cloneNode(false), elem);
    }
  },

  showPopover(el: Element | null | undefined): void {
    const candidate = el as PopoverElement | null | undefined;
    if (candidate && typeof candidate.showPopover === 'function') {
      candidate.showPopover();
    }
  },

  getShadowRootElement(host: Element): Element | null {
    return ((host as unknown as { shadowRoot: ShadowRoot | null }).shadowRoot as unknown as Element | null) ?? null;
  }
};
