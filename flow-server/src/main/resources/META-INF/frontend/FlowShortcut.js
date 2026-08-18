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

/*
 * Client-side helpers for keyboard shortcuts. Loaded on demand by
 * ShortcutRegistration (see initShortcutClient) the same way FlowWebPush.js is
 * loaded by WebPush. Provides the popover/modal origin guards (#24974) and the
 * keydown delegate used when a shortcut listens on a browser-only element.
 */
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};

window.Vaadin.Flow.shortcut = window.Vaadin.Flow.shortcut || {
  // Open popover/modal ancestors of the given node in the flattened (composed)
  // tree, ordered nearest-first, so slotted light-DOM content resolves to the
  // overlay in a component's shadow root. The document root (null) is an
  // implicit ancestor of every entry in the chain.
  _scopeChainOf: function (node) {
    const chain = [];
    while (node) {
      if (node.nodeType === 1 && node.matches && (node.matches(':popover-open') || node.matches(':modal'))) {
        chain.push(node);
      }
      node = node.assignedSlot || node.parentNode || node.host || null;
    }
    return chain;
  },

  // Nearest open popover/modal ancestor of the event target.
  _eventScope: function (event) {
    const path = event.composedPath();
    for (let i = 0; i < path.length; i++) {
      const node = path[i];
      if (node && node.nodeType === 1 && node.matches && (node.matches(':popover-open') || node.matches(':modal'))) {
        return node;
      }
    }
    return null;
  },

  // Delegate path: suppress when an open popover/modal sits between the event
  // target and the boundary element the listener is attached to. Returns true
  // when the shortcut is allowed to fire. Fails open on error.
  eventWithinBoundary: function (event, boundary) {
    try {
      const path = event.composedPath();
      const boundaryIndex = path.indexOf(boundary);
      if (boundaryIndex < 0) {
        return true;
      }
      for (let i = 0; i < boundaryIndex; i++) {
        const node = path[i];
        if (node && node.nodeType === 1 && node.matches && (node.matches(':popover-open') || node.matches(':modal'))) {
          return false;
        }
      }
      return true;
    } catch (e) {
      return true;
    }
  },

  // Normal path: fire when the event originates in the shortcut owner's own
  // popover/modal scope or any ancestor of it (including the document root).
  // The owner is located via the given attribute selector. Returns true when
  // the shortcut is allowed to fire. Fails open on error.
  //
  // Containment rather than strict equality: an event from a scope that is
  // shallower than the owner's (e.g. a keydown on the modal dialog host itself,
  // whose overlay lives in shadow DOM, while the owner is slotted into that
  // overlay) still fires. Only events from a scope deeper than or disjoint from
  // the owner's scope are suppressed (#24974: a nested dialog's keydown must not
  // trigger a shortcut owned in a shallower scope).
  //
  // A relayed clone (see registerKeydownDelegate) carries the real origin scope
  // in _vaadinShortcutOriginScope, because its own composedPath points at the
  // listenOn element and no longer reflects where the keydown happened.
  eventInOwnerScope: function (event, ownerSelector) {
    try {
      const owner = document.querySelector(ownerSelector);
      if (!owner) {
        return true;
      }
      const eventScope =
        '_vaadinShortcutOriginScope' in event
          ? event._vaadinShortcutOriginScope
          : window.Vaadin.Flow.shortcut._eventScope(event);
      // The document root (null) is an ancestor of every scope.
      if (eventScope === null) {
        return true;
      }
      const ownerChain = window.Vaadin.Flow.shortcut._scopeChainOf(owner);
      return ownerChain.indexOf(eventScope) !== -1;
    } catch (e) {
      return true;
    }
  },

  // Relays keydown events from a browser-only element (found by the JS locator)
  // to the listenOn component. When the given matcher accepts the event a clone
  // is re-dispatched to listenOn so the server-side shortcut listener fires.
  // (Previously the inline ELEMENT_LOCATOR_JS in ShortcutRegistration.)
  registerKeydownDelegate: function (listenOn, delegate, matches, resetFocus, allowDefault) {
    if (!delegate) {
      throw 'Shortcut listenOn element not found with the given JS locator';
    }
    delegate.addEventListener('keydown', function (event) {
      if (matches(event, delegate)) {
        if (resetFocus) {
          window.Vaadin.Flow.resetFocus();
        }
        const clone = new event.constructor(event.type, event);
        // Remember where the keydown actually originated: the clone is
        // re-targeted at listenOn, so its composedPath can no longer tell a
        // downstream owner-scope guard that the event came from this overlay.
        clone._vaadinShortcutOriginScope = window.Vaadin.Flow.shortcut._eventScope(event);
        listenOn.dispatchEvent(clone);
        if (!allowDefault) {
          event.preventDefault();
        }
        event.stopPropagation();
      }
    });
  }
};
