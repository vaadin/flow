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

interface VaadinElementSize {
  w: number;
  h: number;
}

interface UiElementWithObserver extends HTMLElement {
  _elementSizeObserver?: ResizeObserver;
}

interface ObservedElement extends Element {
  _elementSizeId?: number;
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.elementSizeObserver = {
  /**
   * Creates a shared ResizeObserver on the given UI element. Size changes
   * are dispatched as "vaadin-element-resize" custom events on the UI
   * element, with a `sizes` property mapping element IDs to their new
   * width and height.
   */
  init(uiElement: UiElementWithObserver): void {
    uiElement._elementSizeObserver = new ResizeObserver((entries) => {
      const sizes: Record<number, VaadinElementSize> = {};
      for (const entry of entries) {
        const target = entry.target as ObservedElement;
        if (target.isConnected && entry.contentBoxSize && target._elementSizeId !== undefined) {
          sizes[target._elementSizeId] = {
            w: Math.round(entry.contentRect.width),
            h: Math.round(entry.contentRect.height)
          };
        }
      }
      if (Object.keys(sizes).length > 0) {
        const event = new Event('vaadin-element-resize') as Event & { sizes: typeof sizes };
        event.sizes = sizes;
        uiElement.dispatchEvent(event);
      }
    });
  },

  /**
   * Starts observing the given element with the given numeric ID.
   */
  observe(uiElement: UiElementWithObserver, element: ObservedElement, id: number): void {
    element._elementSizeId = id;
    uiElement._elementSizeObserver?.observe(element);
  }
};

// Empty export to ensure TypeScript emits this as an ES module,
// which is required for Vite to load it via import.
export {};
