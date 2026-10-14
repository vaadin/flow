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

// Lets code outside the engine (window.Vaadin.Flow.ready(), inlined by the
// server bootstrap) react to the client becoming idle instead of polling
// client.isActive().

/**
 * The window event dispatched when the result of a client's isActive() may
 * have changed.
 */
export const ACTIVITY_CHANGED_EVENT = 'vaadin-flow-activity-changed';

let notificationPending = false;

/**
 * Notifies listeners that the result of a client's isActive() may have
 * changed.
 *
 * Calls made during the same task are coalesced into a single event, which is
 * dispatched in a microtask so that listeners see the state after the current
 * task has finished updating it.
 */
export function notifyActivityChanged(): void {
  if (notificationPending) {
    return;
  }
  notificationPending = true;
  queueMicrotask(() => {
    notificationPending = false;
    window.dispatchEvent(new Event(ACTIVITY_CHANGED_EVENT));
  });
}
