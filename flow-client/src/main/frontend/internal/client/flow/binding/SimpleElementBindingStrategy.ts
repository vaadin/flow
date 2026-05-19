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

import { PolymerUtils } from '../../PolymerUtils';

const DOM_REPEAT_MODIFIED_MARKER = '$propChangedModified';

interface PolymerElementInternal extends Element {
  _propertiesChanged?: (currentProps: unknown, changedProps: unknown, oldProps: unknown) => void;
  ready?: (...args: unknown[]) => void;
  root?: ParentNode | null;
}

interface DomRepeatPropertiesChanged {
  items?: Record<string, { nodeId?: number } & Record<string, unknown>>;
}

interface DomRepeatNode {
  constructor: {
    prototype: {
      _propertiesChanged?: (
        currentProps: DomRepeatPropertiesChanged,
        changedProps: Record<string, unknown>,
        oldProps: unknown
      ) => void;
      [DOM_REPEAT_MODIFIED_MARKER]?: boolean;
    };
  };
}

interface DataHostNode {
  localName?: string;
  __dataHost?: DataHostNode;
}

/**
 * Polymer integration helpers migrated from
 * `com.vaadin.client.flow.binding.SimpleElementBindingStrategy`. Reached from
 * GWT-compiled code via the `NativeSimpleElementBindingStrategy` JsType shim.
 * The Java side hands over an `onHookUp` callback and the three Java methods
 * called back from inside the Polymer prototype patches; this module owns the
 * choreography but not the binding logic.
 */
export const SimpleElementBindingStrategy = {
  bindPolymerModelProperties(element: Element, onHookUp: () => void): void {
    if (PolymerUtils.isPolymerElement(element)) {
      onHookUp();
      return;
    }
    if (!PolymerUtils.mayBePolymerElement(element)) {
      return;
    }
    try {
      const localName = (element as PolymerElementInternal).localName ?? '';
      const whenDefinedPromise = customElements.whenDefined(localName);
      // whenDefined() may never resolve for a non-custom element; the timeout
      // race makes sure the chained closure can be garbage-collected.
      const promiseTimeout = new Promise<void>((resolve) => {
        setTimeout(resolve, 1000);
      });
      void Promise.race([whenDefinedPromise, promiseTimeout]).then(() => {
        if (PolymerUtils.isPolymerElement(element)) {
          onHookUp();
        }
      });
    } catch {
      // Not a custom element — ignore.
    }
  },

  hookUpPolymerElement(
    element: Element,
    handlePropertiesChanged: (changedProps: unknown) => void,
    fireReadyEvent: () => void,
    handleListItemPropertyChange: (nodeId: number, host: unknown, propertyName: string, value: unknown) => void
  ): void {
    const elem = element as PolymerElementInternal;
    const originalPropertiesChanged = elem._propertiesChanged;
    if (originalPropertiesChanged) {
      elem._propertiesChanged = function (
        this: PolymerElementInternal,
        currentProps: unknown,
        changedProps: unknown,
        oldProps: unknown
      ) {
        handlePropertiesChanged(changedProps);
        originalPropertiesChanged.apply(this, [currentProps, changedProps, oldProps]);
      };
    }

    const originalReady = elem.ready;
    elem.ready = function (this: PolymerElementInternal, ...args: unknown[]) {
      originalReady?.apply(this, args);
      fireReadyEvent();
      installDomRepeatPropertyChangeReplacement(elem, handleListItemPropertyChange);
    };
  }
};

function installDomRepeatPropertyChangeReplacement(
  elem: PolymerElementInternal,
  handleListItemPropertyChange: (nodeId: number, host: unknown, propertyName: string, value: unknown) => void
): void {
  const replaceDomRepeatPropertyChange = function (): void {
    const domRepeat = elem.root?.querySelector('dom-repeat') as unknown as DomRepeatNode | null;
    if (domRepeat) {
      elem.removeEventListener('dom-change', replaceDomRepeatPropertyChange);
    } else {
      return;
    }
    const proto = domRepeat.constructor.prototype;
    if (proto[DOM_REPEAT_MODIFIED_MARKER]) {
      return;
    }
    proto[DOM_REPEAT_MODIFIED_MARKER] = true;
    const originalDomRepeatPropertiesChanged = proto._propertiesChanged;
    proto._propertiesChanged = function (
      this: DataHostNode,
      currentProps: DomRepeatPropertiesChanged,
      changedProps: Record<string, unknown>,
      oldProps: unknown
    ) {
      originalDomRepeatPropertiesChanged?.apply(this, [currentProps, changedProps, oldProps]);
      forwardItemChangesToServer(this, currentProps, changedProps, handleListItemPropertyChange);
    };
  };

  if (elem.root?.querySelector('dom-repeat')) {
    replaceDomRepeatPropertyChange();
  } else {
    // dom-repeat may not be in DOM yet (e.g. behind a dom-if that's currently false);
    // wait for a dom-change to retry.
    elem.addEventListener('dom-change', replaceDomRepeatPropertyChange);
  }
}

function forwardItemChangesToServer(
  domRepeat: DataHostNode,
  currentProps: DomRepeatPropertiesChanged,
  changedProps: Record<string, unknown>,
  handleListItemPropertyChange: (nodeId: number, host: unknown, propertyName: string, value: unknown) => void
): void {
  const items = 'items.';
  for (const key of Object.getOwnPropertyNames(changedProps)) {
    if (key.indexOf(items) !== 0) {
      continue;
    }
    const prop = key.substring(items.length);
    const dot = prop.indexOf('.');
    if (dot <= 0) {
      continue;
    }
    const arrayIndex = prop.substring(0, dot);
    const propertyName = prop.substring(dot + 1);
    const currentPropsItem = currentProps.items?.[arrayIndex];
    if (!currentPropsItem || currentPropsItem.nodeId == null) {
      continue;
    }
    const nodeId = currentPropsItem.nodeId;
    const value = currentPropsItem[propertyName];
    // __dataHost is a linked list whose tail is the template owning the
    // local DOM. Walk to the tail to find the template element.
    let host = domRepeat.__dataHost;
    while (host && (!host.localName || host.__dataHost)) {
      host = host.__dataHost;
    }
    handleListItemPropertyChange(nodeId, host, propertyName, value);
  }
}
