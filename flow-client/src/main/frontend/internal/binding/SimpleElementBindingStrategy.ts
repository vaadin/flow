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

// TypeScript port of com.vaadin.client.flow.binding.SimpleElementBindingStrategy,
// the binding strategy for a simple (non-template) Element. It is the largest
// client class and pulls in the DOM-abstraction and configuration layers that
// are not ported yet, so it is built up across several build-alongside slices.
//
// This first slice ports the self-contained DOM-event-data resolution helpers
// (the event-expression cache plus filter/debounce resolution) used by
// handleDomEvent. They depend only on the already-ported Debouncer. The strategy
// class itself (create/isApplicable/bind and the DOM-binding methods) and the
// Polymer model-property bridge (the window-registered bindPolymerModelProperties
// in internal/SimpleElementBindingStrategy.ts) are folded in by later slices.

import { Debouncer } from './Debouncer';

// The callback sending an event to the server for a given debounce phase (null
// when sent outside any debounce). Compatible with Debouncer's send command.
type SendCommand = (phase: string | null) => void;

// A synchronization command run before an event is sent.
type Command = () => void;

// An event expression parsed via `new Function`, evaluated against the DOM event
// and target element; mirrors the EventExpression @JsFunction.
type EventExpression = (event: Event, element: Element) => unknown;

let expressionCache: Map<string, EventExpression> | null = null;

/**
 * Parses an event-data expression into a function `(event, element) => value`,
 * caching the result per expression string; mirrors getOrCreateExpression.
 */
export function getOrCreateExpression(expressionString: string): EventExpression {
  if (expressionCache === null) {
    expressionCache = new Map();
  }
  let expression = expressionCache.get(expressionString);

  if (expression === undefined) {
    // Mirrors NativeFunction.create; the server controls these expressions.
    expression = new Function('event', 'element', `return (${expressionString})`) as EventExpression;
    expressionCache.set(expressionString, expression);
  }

  return expression;
}

/**
 * Resolves the debounce settings for one event filter. Each entry in
 * debounceList is `[timeout, phase1, phase2, ...]`; a zero timeout is eager.
 * Returns true if at least one debounce is eager (should be sent now). Mirrors
 * resolveDebounces.
 */
// eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java resolveDebounces signature
export function resolveDebounces(
  element: Node,
  debouncerId: string,
  debounceList: unknown[][],
  sendCommand: SendCommand,
  commands: Map<string, Command>
): boolean {
  let atLeastOneEager = false;

  for (const debounceSettings of debounceList) {
    const timeout = debounceSettings[0] as number;

    if (timeout === 0) {
      atLeastOneEager = true;
      continue;
    }

    const phases = new Set<string>();
    for (let j = 1; j < debounceSettings.length; j++) {
      phases.add(debounceSettings[j] as string);
    }

    const eager = Debouncer.getOrCreate(element, debouncerId, timeout).trigger(phases, sendCommand, commands);

    atLeastOneEager = atLeastOneEager || eager;
  }

  return atLeastOneEager;
}

/**
 * Resolves the event filters for an event type. Returns true if there are no
 * filters or at least one filter matched (so the event should be sent). Mirrors
 * resolveFilters.
 */
// eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java resolveFilters signature
export function resolveFilters(
  element: Node,
  eventType: string,
  expressionSettings: Record<string, unknown>,
  eventData: Record<string, unknown> | null,
  sendCommand: SendCommand,
  commands: Map<string, Command>
): boolean {
  let noFilters = true;
  let atLeastOneFilterMatched = false;

  for (const expression of Object.keys(expressionSettings)) {
    const settings = expressionSettings[expression];

    const hasDebounce = Array.isArray(settings);

    if (!hasDebounce && !(settings as boolean)) {
      continue;
    }
    noFilters = false;

    let filterMatched = eventData !== null && Boolean(eventData[expression]);
    if (hasDebounce && filterMatched) {
      const debouncerId = `on-${eventType}:${expression}`;

      // Count as a match only if at least one debounce is eager
      filterMatched = resolveDebounces(element, debouncerId, settings as unknown[][], sendCommand, commands);
    }

    atLeastOneFilterMatched = atLeastOneFilterMatched || filterMatched;
  }

  return noFilters || atLeastOneFilterMatched;
}
