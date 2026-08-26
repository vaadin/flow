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
 * Size data passed to the observe() callback. Field names match the Java
 * Size record so the value can be Jackson-deserialised on the server when
 * forwarded through a trigger-framework input.
 */
interface Size {
  width: number;
  height: number;
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.elementResize = {
  /**
   * Installs a ResizeObserver on the given element and invokes the callback
   * with the rounded content-box width and height each time the element
   * resizes. Returns a function that disconnects the observer.
   *
   * Sub-pixel decimals from contentRect are rounded to integers to avoid
   * spamming equal-after-rounding updates back to the server.
   */
  observe(element: Element, callback: (size: Size) => void): () => void {
    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (!entry.target.isConnected) {
          continue;
        }
        callback({
          width: Math.round(entry.contentRect.width),
          height: Math.round(entry.contentRect.height)
        });
      }
    });
    observer.observe(element);
    return () => observer.disconnect();
  }
};

// Empty export to ensure TypeScript emits this as an ES module,
// which is required for Vite to load it via import.
export {};
