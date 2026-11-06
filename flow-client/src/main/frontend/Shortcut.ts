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
 * Client-side helpers for keyboard shortcuts, referenced from the listener
 * filters and the keydown delegate ShortcutRegistration sends. Provides the
 * popover/modal origin guards (#24974) and the delegate used when a shortcut
 * listens on a browser-only element.
 */

// A keydown clone re-dispatched by registerKeydownDelegate, carrying the
// popover/modal scope the original event came from.
type RelayedKeyboardEvent = KeyboardEvent & { _vaadinShortcutOriginScope?: Element | null };

function isOpenOverlay(node: unknown): node is Element {
  const element = node as Element | null;
  return element?.nodeType === Node.ELEMENT_NODE && (element.matches(':popover-open') || element.matches(':modal'));
}

// Nearest open popover/modal ancestor of the given node in the flattened
// (composed) tree, so slotted light-DOM content resolves to the overlay in a
// component's shadow root.
function scopeOf(node: Node | null): Element | null {
  let current: Node | null = node;
  while (current) {
    if (isOpenOverlay(current)) {
      return current;
    }
    current =
      (current as Element).assignedSlot ?? current.parentNode ?? ((current as ShadowRoot).host as Node | null) ?? null;
  }
  return null;
}

// Nearest open popover/modal ancestor of the event target.
function eventScope(event: Event): Element | null {
  return event.composedPath().find(isOpenOverlay) ?? null;
}

// Popover/modal scope the event actually originated from.
//
// A relayed clone (see registerKeydownDelegate) carries the real origin scope
// in _vaadinShortcutOriginScope, because its own composedPath points at the
// listenOn element and no longer reflects where the keydown happened.
function originScope(event: RelayedKeyboardEvent): Element | null {
  return '_vaadinShortcutOriginScope' in event ? (event._vaadinShortcutOriginScope ?? null) : eventScope(event);
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.shortcut = {
  // Delegate path: suppress when an open popover/modal sits between the event
  // target and the boundary element the listener is attached to. Returns true
  // when the shortcut is allowed to fire. Fails open on error.
  eventWithinBoundary(event: Event, boundary: EventTarget): boolean {
    try {
      const path = event.composedPath();
      const boundaryIndex = path.indexOf(boundary);
      if (boundaryIndex < 0) {
        return true;
      }
      return !path.slice(0, boundaryIndex).some(isOpenOverlay);
    } catch (_e) {
      return true;
    }
  },

  // Normal path: fire only when the event and the shortcut owner (located via
  // the given attribute selector) share the same popover/modal scope. Returns
  // true when the shortcut is allowed to fire. Fails open on error.
  eventInOwnerScope(event: RelayedKeyboardEvent, ownerSelector: string): boolean {
    try {
      const owner = document.querySelector(ownerSelector);
      if (!owner) {
        return true;
      }
      return originScope(event) === scopeOf(owner);
    } catch (_e) {
      return true;
    }
  },

  // Normal path for a shortcut owned by the UI: the owner is <body>, which can
  // never sit inside an open popover/modal, so sharing its scope simply means
  // the event did not originate inside one. Expressed without an owner selector
  // so that every UI-owned shortcut yields the same filter expression.
  eventInTopLevelScope(event: RelayedKeyboardEvent): boolean {
    try {
      return originScope(event) === null;
    } catch (_e) {
      return true;
    }
  },

  // Relays keydown events from a browser-only element (found by the JS locator)
  // to the listenOn component. When the given matcher accepts the event a clone
  // is re-dispatched to listenOn so the server-side shortcut listener fires.
  // eslint-disable-next-line @typescript-eslint/max-params -- positional arguments of the delegate expression ShortcutRegistration sends
  registerKeydownDelegate(
    listenOn: EventTarget,
    delegate: EventTarget | null,
    matches: (event: KeyboardEvent, delegate: EventTarget) => boolean,
    resetFocus: boolean,
    allowDefault: boolean
  ): void {
    if (!delegate) {
      throw new Error('Shortcut listenOn element not found with the given JS locator');
    }
    delegate.addEventListener('keydown', (event) => {
      const keyEvent = event as KeyboardEvent;
      if (matches(keyEvent, delegate)) {
        if (resetFocus) {
          $wnd.Vaadin.Flow.resetFocus();
        }
        const clone: RelayedKeyboardEvent = new (keyEvent.constructor as typeof KeyboardEvent)(keyEvent.type, keyEvent);
        // Remember where the keydown actually originated: the clone is
        // re-targeted at listenOn, so its composedPath can no longer tell a
        // downstream owner-scope guard that the event came from this overlay.
        clone._vaadinShortcutOriginScope = eventScope(keyEvent);
        listenOn.dispatchEvent(clone);
        if (!allowDefault) {
          keyEvent.preventDefault();
        }
        keyEvent.stopPropagation();
      }
    });
  }
};

// Empty export to ensure TypeScript emits this as an ES module, which is
// required for Vite to load it via import.
export {};
