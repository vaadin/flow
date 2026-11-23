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
 * Client-side helpers for keyboard shortcuts, called by the declared listener
 * filter and keydown delegate of ShortcutRegistration (ShortcutFilterJs and
 * ShortcutJs). They get the keys, modifiers and options of a shortcut as data,
 * so the JavaScript is the same for every shortcut. Also provides the
 * popover/modal origin guards (#24974).
 */

// The key and modifiers of a shortcut. Modifier names are those of
// KeyboardEvent.getModifierState, separated by spaces.
interface KeyCombination {
  keys: string[];
  requiredModifiers: string;
  forbiddenModifiers: string;
}

// The options of the keydown listener filter of a shortcut.
interface ListenerOptions extends KeyCombination {
  // Whether the keydown has to originate from the popover/modal scope of the
  // shortcut owner, located by ownerSelector, or for a null selector from the
  // top level, outside any open popover/modal.
  checkOwnerScope: boolean;
  ownerSelector: string | null;
  preventDefault: boolean;
  stopPropagation: boolean;
  resetFocus: boolean;
}

// The options of a keydown delegate.
interface DelegateOptions extends KeyCombination {
  // Whether a keydown from an open popover/modal inside the delegate element
  // is ignored.
  checkBoundary: boolean;
  resetFocus: boolean;
  allowDefault: boolean;
}

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

function splitModifiers(modifiers: string): string[] {
  return modifiers.split(' ').filter((modifier) => modifier.length > 0);
}

// Whether the keydown is the key of the shortcut, given as a key code or a key
// value, with exactly the modifiers the shortcut asks for.
function matchesKeyCombination(event: KeyboardEvent, combination: KeyCombination): boolean {
  return (
    (combination.keys.includes(event.code) || combination.keys.includes(event.key)) &&
    splitModifiers(combination.requiredModifiers).every((modifier) => event.getModifierState(modifier)) &&
    !splitModifiers(combination.forbiddenModifiers).some((modifier) => event.getModifierState(modifier))
  );
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
const shortcut = {
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

  // Listener filter of a shortcut: true when the keydown is the shortcut and,
  // when asked for, originates from the scope of the shortcut owner. Only a
  // keydown that passes gets the side effects, so typing is not blocked.
  matchKeydown(event: RelayedKeyboardEvent, options: ListenerOptions): boolean {
    if (!matchesKeyCombination(event, options)) {
      return false;
    }
    // Checked before the side effects, so that an event from a nested
    // popover/modal is left untouched.
    if (options.checkOwnerScope) {
      const inScope =
        options.ownerSelector === null
          ? shortcut.eventInTopLevelScope(event)
          : shortcut.eventInOwnerScope(event, options.ownerSelector);
      if (!inScope) {
        return false;
      }
    }
    if (options.preventDefault) {
      event.preventDefault();
    }
    if (options.stopPropagation) {
      event.stopPropagation();
    }
    if (options.resetFocus) {
      $wnd.Vaadin.Flow.resetFocus();
    }
    return true;
  },

  // Relays keydown events from a browser-only element, returned by
  // locateDelegate called with listenOn as this, to the listenOn component.
  // When a keydown is the shortcut, a clone is re-dispatched to listenOn so the
  // server-side shortcut listener fires.
  registerKeydownDelegate(
    listenOn: Element,
    locateDelegate: (this: Element) => EventTarget | null | undefined,
    options: DelegateOptions
  ): void {
    const delegate = locateDelegate.call(listenOn);
    if (!delegate) {
      throw new Error('Shortcut listenOn element not found with the given JS locator');
    }
    delegate.addEventListener('keydown', (event) => {
      const keyEvent = event as KeyboardEvent;
      // The guard runs on the original event: the clone loses its origin.
      if (
        matchesKeyCombination(keyEvent, options) &&
        (!options.checkBoundary || shortcut.eventWithinBoundary(keyEvent, delegate))
      ) {
        if (options.resetFocus) {
          $wnd.Vaadin.Flow.resetFocus();
        }
        const clone: RelayedKeyboardEvent = new (keyEvent.constructor as typeof KeyboardEvent)(keyEvent.type, keyEvent);
        // Remember where the keydown actually originated: the clone is
        // re-targeted at listenOn, so its composedPath can no longer tell a
        // downstream owner-scope guard that the event came from this overlay.
        clone._vaadinShortcutOriginScope = eventScope(keyEvent);
        listenOn.dispatchEvent(clone);
        if (!options.allowDefault) {
          keyEvent.preventDefault();
        }
        keyEvent.stopPropagation();
      }
    });
  }
};
$wnd.Vaadin.Flow.shortcut = shortcut;

// Empty export to ensure TypeScript emits this as an ES module, which is
// required for Vite to load it via import.
export {};
