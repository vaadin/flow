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
 * Client-side helpers for keyboard shortcuts, installed when the client loads
 * so that the listeners ShortcutRegistration adds can call them. Provides the
 * popover/modal origin guards (#24974) and the keydown delegate used when a
 * shortcut listens on a browser-only element.
 */

/**
 * A keydown event relayed by registerKeydownDelegate, which remembers the
 * popover/modal scope the original event came from.
 */
type RelayedKeyboardEvent = KeyboardEvent & { _vaadinShortcutOriginScope?: Element | null };

function isOpenOverlay(node: unknown): node is Element {
  return node instanceof Element && (node.matches(':popover-open') || node.matches(':modal'));
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.shortcut = {
  // Nearest open popover/modal ancestor of the given node in the flattened
  // (composed) tree, so slotted light-DOM content resolves to the overlay in a
  // component's shadow root.
  _scopeOf(node: Node | null): Element | null {
    let current: Node | null = node;
    while (current) {
      if (isOpenOverlay(current)) {
        return current;
      }
      current =
        (current as Element).assignedSlot ?? current.parentNode ?? (current as unknown as ShadowRoot).host ?? null;
    }
    return null;
  },

  // Nearest open popover/modal ancestor of the event target.
  _eventScope(event: Event): Element | null {
    return (event.composedPath().find(isOpenOverlay) as Element | undefined) ?? null;
  },

  // Delegate path: suppress when an open popover/modal sits between the event
  // target and the boundary element the listener is attached to. Returns true
  // when the shortcut is allowed to fire. Fails open on error.
  eventWithinBoundary(event: Event, boundary: Element): boolean {
    try {
      const path = event.composedPath();
      const boundaryIndex = path.indexOf(boundary);
      if (boundaryIndex < 0) {
        return true;
      }
      return !path.slice(0, boundaryIndex).some(isOpenOverlay);
    } catch {
      return true;
    }
  },

  // Popover/modal scope the event actually originated from.
  //
  // A relayed clone (see registerKeydownDelegate) carries the real origin scope
  // in _vaadinShortcutOriginScope, because its own composedPath points at the
  // listenOn element and no longer reflects where the keydown happened.
  _originScope(event: RelayedKeyboardEvent): Element | null {
    return '_vaadinShortcutOriginScope' in event
      ? event._vaadinShortcutOriginScope ?? null
      : $wnd.Vaadin.Flow.shortcut._eventScope(event);
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
      return $wnd.Vaadin.Flow.shortcut._originScope(event) === $wnd.Vaadin.Flow.shortcut._scopeOf(owner);
    } catch {
      return true;
    }
  },

  // Normal path for a shortcut owned by the UI: the owner is <body>, which can
  // never sit inside an open popover/modal, so sharing its scope simply means
  // the event did not originate inside one. Expressed without an owner selector
  // so that every UI-owned shortcut yields the same filter expression.
  eventInTopLevelScope(event: RelayedKeyboardEvent): boolean {
    try {
      return $wnd.Vaadin.Flow.shortcut._originScope(event) === null;
    } catch {
      return true;
    }
  },

  // Relays keydown events from a browser-only element (found by the JS locator)
  // to the listenOn component. When the given matcher accepts the event a clone
  // is re-dispatched to listenOn so the server-side shortcut listener fires.
  // The parameters are the arguments of the call ShortcutRegistration sends.
  // eslint-disable-next-line @typescript-eslint/max-params
  registerKeydownDelegate(
    listenOn: Element,
    delegate: Element | null,
    matches: (event: KeyboardEvent, delegate: Element) => boolean,
    resetFocus: boolean,
    allowDefault: boolean
  ): void {
    if (!delegate) {
      throw new Error('Shortcut listenOn element not found with the given JS locator');
    }
    delegate.addEventListener('keydown', (event) => {
      const keyboardEvent = event as KeyboardEvent;
      if (matches(keyboardEvent, delegate)) {
        if (resetFocus) {
          $wnd.Vaadin.Flow.resetFocus();
        }
        const EventType = keyboardEvent.constructor as typeof KeyboardEvent;
        const clone: RelayedKeyboardEvent = new EventType(keyboardEvent.type, keyboardEvent);
        // Remember where the keydown actually originated: the clone is
        // re-targeted at listenOn, so its composedPath can no longer tell a
        // downstream owner-scope guard that the event came from this overlay.
        clone._vaadinShortcutOriginScope = $wnd.Vaadin.Flow.shortcut._eventScope(keyboardEvent);
        listenOn.dispatchEvent(clone);
        if (!allowDefault) {
          keyboardEvent.preventDefault();
        }
        keyboardEvent.stopPropagation();
      }
    });
  }
};
