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

// Polymer model-property binding migrated from SimpleElementBindingStrategy.java,
// registered on window.Vaadin.Flow.internal.SimpleElementBindingStrategy by
// registerInternals; the Java bindPolymerModelProperties method delegates here.
// The StateNode/StateTree-coupled callbacks (handlePropertiesChanged,
// fireReadyEvent, handleListItemPropertyChange) are supplied from the Java side
// already wrapped in $entry, with the node and tree captured in their closures;
// here we only do the Polymer-specific DOM wiring. Also bundled to ES5 for the
// HtmlUnit used by GwtTests.
//
// This code monkey-patches Polymer internals (_propertiesChanged, ready, the
// dom-repeat prototype), so it manipulates loosely-typed objects and relies on
// `this`/`arguments` inside the patched functions -- those must stay regular
// `function` expressions, not arrows.

import { isPolymerElement, mayBePolymerElement } from './PolymerUtils';

// The StateNode/StateTree-coupled actions, supplied from Java already
// $entry-wrapped with node/tree captured. fireReadyEvent receives the element;
// the others receive only the Polymer-side arguments.
interface PolymerBindingCallbacks {
  handlePropertiesChanged: (changedProps: unknown) => void;
  fireReadyEvent: (element: unknown) => void;
  handleListItemPropertyChange: (nodeId: unknown, host: unknown, propertyName: string, value: unknown) => void;
}

/**
 * Hooks the Polymer model-property bridge onto the element. If the element is
 * already a Polymer element it is wired immediately; if it may still become one
 * (a custom element that has not upgraded yet) the wiring is deferred until the
 * element is defined. A timeout races the whenDefined promise so a non-custom
 * element does not leak the chained closures.
 */
export function bindPolymerModelProperties(element: any, callbacks: PolymerBindingCallbacks): void {
  if (isPolymerElement(element)) {
    hookUpPolymerElement(element, callbacks);
  } else if (mayBePolymerElement(element)) {
    try {
      const whenDefinedPromise = window.customElements.whenDefined(element.localName);
      const promiseTimeout = new Promise(function (resolve) {
        setTimeout(resolve, 1000);
      });
      void Promise.race([whenDefinedPromise, promiseTimeout]).then(function () {
        if (isPolymerElement(element)) {
          hookUpPolymerElement(element, callbacks);
        }
      });
    } catch (e) {
      // ignore the exception: the element cannot be a custom element
    }
  }
}

function hookUpPolymerElement(element: any, callbacks: PolymerBindingCallbacks): void {
  const originalPropertiesChanged = element._propertiesChanged;

  if (originalPropertiesChanged) {
    element._propertiesChanged = function (this: any, _currentProps: any, changedProps: any, _oldProps: any): void {
      callbacks.handlePropertiesChanged(changedProps);
      originalPropertiesChanged.apply(this, arguments);
    };
  }

  const originalReady = element.ready;

  element.ready = function (this: any): void {
    originalReady.apply(this, arguments);
    callbacks.fireReadyEvent(element);

    // The _propertiesChanged method replaced above for the element does not do
    // anything for items in dom-repeat. Instead it is called with meaningful
    // info for the dom-repeat element itself, so here _propertiesChanged is
    // replaced on the dom-repeat prototype, which changes it for every
    // dom-repeat instance.
    const replaceDomRepeatPropertyChange = function (): void {
      const domRepeat = element.root.querySelector('dom-repeat');

      if (domRepeat) {
        // Once the dom-repeat element is in the DOM this listener is no longer
        // needed; the prototype replacement below covers every instance.
        element.removeEventListener('dom-change', replaceDomRepeatPropertyChange);
      } else {
        return;
      }
      // dom-repeat found => replace _propertiesChanged in the prototype and
      // mark it as replaced.
      if (!domRepeat.constructor.prototype.$propChangedModified) {
        domRepeat.constructor.prototype.$propChangedModified = true;

        const changed = domRepeat.constructor.prototype._propertiesChanged;

        domRepeat.constructor.prototype._propertiesChanged = function (
          this: any,
          currentProps: any,
          changedProps: any,
          _oldProps: any
        ): void {
          changed.apply(this, arguments);

          const props = Object.getOwnPropertyNames(changedProps);
          const items = 'items.';
          for (const propPath of props) {
            // There should be a property which starts with "items." whose next
            // token is the index of the changed item; parse that here.
            let index = propPath.indexOf(items);
            if (index === 0) {
              const prop = propPath.substring(items.length);
              index = prop.indexOf('.');
              if (index > 0) {
                // The index of the changed item.
                const arrayIndex = prop.substring(0, index);
                // The property name of the changed item.
                const propertyName = prop.substring(index + 1);
                const currentPropsItem = currentProps.items[arrayIndex];
                if (currentPropsItem && currentPropsItem.nodeId) {
                  const nodeId = currentPropsItem.nodeId;
                  const value = currentPropsItem[propertyName];

                  // Find the template element, which is not available as a
                  // context in the prototype method.
                  let host = this.__dataHost;
                  // __dataHost is an element in the local DOM which owns the
                  // changed data. Such elements form a linked list whose head is
                  // the dom-repeat (this) and whose tail is the template owning
                  // the local DOM, so walk the list to the tail template.
                  while (!host.localName || host.__dataHost) {
                    host = host.__dataHost;
                  }

                  callbacks.handleListItemPropertyChange(nodeId, host, propertyName, value);
                }
              }
            }
          }
        };
      }
    };

    // dom-repeat does not have to be in the DOM even if the template has it:
    // this happens with e.g. a dom-if that evaluates to false initially, in
    // which case dom-repeat is not in the DOM tree until the dom-if becomes
    // true.
    if (element.root && element.root.querySelector('dom-repeat')) {
      replaceDomRepeatPropertyChange();
    } else {
      // No dom-repeat yet: add a dom-change listener which is notified once the
      // local DOM changes, giving replaceDomRepeatPropertyChange a chance to run.
      element.addEventListener('dom-change', replaceDomRepeatPropertyChange);
    }
  };
}
