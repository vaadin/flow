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

// Per-element stash for active temporary modifications. Hung off the
// stash element via a Symbol so multiple "apply X / revert X" actions on
// the same element with distinct keys do not collide, and the key space
// is private to this module (no clashes with element properties set by
// custom elements or user code).
const stashSymbol: unique symbol = Symbol('vaadin.flow.triggerActions.tempStash');

interface TempEntry {
  original: unknown;
  timer: number;
}

type StashHost = { [stashSymbol]?: Map<string, TempEntry> };

interface TemporaryAction {
  // Reads the value to stash; only called on the first fire of a cycle.
  snapshot: () => unknown;
  // Performs the temporary change; called on every fire.
  apply: (event: unknown) => void;
  // Restores the stashed value; called once per cycle when the timer expires.
  revert: (original: unknown) => void;
  // Delay before reverting, in milliseconds (counted from each fire).
  timeoutMs: number;
}

/**
 * Applies a transient change and schedules its reversion. Coalesces rapid
 * re-fires so the value captured on the first fire is the one restored at
 * the end of the cycle — re-fires within the timeout window keep the same
 * original and reset the timer, they do not re-snapshot the (already-
 * modified) current value.
 *
 * - {@code stashElement} hangs the per-element stash; typically the modified
 *   target so two actions on the same element with the same {@code key}
 *   share state.
 * - {@code key} separates unrelated temporary modifications on the same
 *   element (e.g. property name).
 * - {@code action} bundles the three payload functions plus the timeout.
 * - {@code event} is the trigger's event, passed through to {@code apply}.
 */
function applyTemporarily(stashElement: StashHost, key: string, action: TemporaryAction, event: unknown): void {
  const stash = (stashElement[stashSymbol] ??= new Map<string, TempEntry>());
  let entry = stash.get(key);
  if (entry) {
    // Re-fire inside the window: keep the original from the first fire,
    // cancel the pending revert so we can replace it.
    clearTimeout(entry.timer);
  } else {
    entry = { original: action.snapshot(), timer: 0 };
    stash.set(key, entry);
  }
  action.apply(event);
  const current = entry;
  current.timer = window.setTimeout(() => {
    action.revert(current.original);
    // Only delete if this entry is still the active one — defensive against
    // future changes that might let a new fire replace the entry between
    // the timer scheduling and its callback execution.
    if (stash.get(key) === current) {
      stash.delete(key);
    }
  }, action.timeoutMs);
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.triggerActions = {
  applyTemporarily: applyTemporarily
};

// Empty export to ensure TypeScript emits this as an ES module,
// which is required for Vite to load it via import.
export {};
