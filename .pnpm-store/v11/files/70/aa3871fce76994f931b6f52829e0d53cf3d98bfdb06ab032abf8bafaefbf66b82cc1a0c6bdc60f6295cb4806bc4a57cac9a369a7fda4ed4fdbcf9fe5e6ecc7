/**
 * @license
 * Copyright (c) 2026 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

const consumedEvents = new WeakSet();

/**
 * Marks the event as consumed by a component, e.g. a click that closed an overlay.
 * Unlike `preventDefault()`, this does not cancel the default action of the event.
 *
 * @param {Event} event
 */
export function markEventConsumed(event) {
  consumedEvents.add(event);
}

/**
 * Returns true if the event was marked as consumed with `markEventConsumed()`.
 * Only check it where a click that closed an overlay must be ignored, e.g. a backdrop.
 * Do not use it in regular click handlers, so that synthetic clicks keep working.
 *
 * @param {Event} event
 * @return {boolean}
 */
export function isEventConsumed(event) {
  return consumedEvents.has(event);
}
