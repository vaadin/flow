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

import { wrap } from '../dom/DomApi';
import { NodeFeatures, NodeProperties } from '../nodefeature/NodeFeatures';
import type { EventRemover } from '../reactive/reactive';
import { isAbsoluteUrl, updateAttribute as setElementAttribute } from '../WidgetUtil';
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

// --- Slice 2: closest-state-node lookups -----------------------------------
// Used by handleDomEvent to map an event target / a DOM node returned by a JS
// expression to the nearest bound state node id (the MAP_STATE_NODE_EVENT_DATA
// event data). DOM parents are walked with native parentNode/isSameNode; the
// shadow-tree-aware DomApi wrapping (not ported yet) replaces the raw traversal
// at cutover.

/** The slice of StateNode that the closest-node lookups read. */
interface ClosestLookupNode {
  getId(): number;
  getDomNode(): Node | null;
  getList(featureId: number): { forEach(callback: (child: unknown) => void): void };
}

/** The slice of StateTree that getClosestStateNodeIdToDomNode reads. */
interface ClosestLookupTree {
  getStateNodeForDomNode(domNode: Node): { getId(): number } | null;
}

/**
 * Finds the id of the state node closest to the event target: a breadth-first
 * search of the state-node tree for a direct DOM match, then a bottom-up DOM
 * walk from the target's parent. Returns -1 if none is found. Mirrors
 * getClosestStateNodeIdToEventTarget.
 */
export function getClosestStateNodeIdToEventTarget(topNode: ClosestLookupNode, target: EventTarget | null): number {
  if (target === null) {
    return -1;
  }
  try {
    const stack: ClosestLookupNode[] = [topNode];

    // collect children and test eagerly for direct match; the stack grows as
    // children are pushed during iteration (breadth-first)
    // eslint-disable-next-line @typescript-eslint/prefer-for-of -- index loop: stack is mutated during iteration
    for (let i = 0; i < stack.length; i++) {
      const stateNode = stack[i];
      if ((target as unknown as Node).isSameNode(stateNode.getDomNode())) {
        return stateNode.getId();
      }
      // NOTE: for now not looking at virtual children on purpose.
      stateNode.getList(NodeFeatures.ELEMENT_CHILDREN).forEach((child) => stack.push(child as ClosestLookupNode));
    }
    // no direct match: bottom-up search from the target's parent
    return getStateNodeForElement(stack, (target as unknown as Node).parentNode);
  } catch (e) {
    // not going to let event handling fail; just report nothing found
    console.debug(
      `An error occurred when Flow tried to find a state node matching the element ${String(
        target
      )}, which was the event.target. Error: ${(e as Error).message}`
    );
  }
  return -1;
}

/**
 * Walks up the DOM from targetNode and returns the id of the first state node
 * in searchStack whose DOM node matches, or -1. Mirrors getStateNodeForElement.
 */
export function getStateNodeForElement(searchStack: ClosestLookupNode[], targetNode: Node | null): number {
  let current = targetNode;
  while (current !== null) {
    for (let i = searchStack.length - 1; i > -1; i--) {
      const stateNode = searchStack[i];
      if (current.isSameNode(stateNode.getDomNode())) {
        return stateNode.getId();
      }
    }
    current = current.parentNode;
  }
  return -1;
}

/**
 * Walks up the DOM from a node reference (e.g. returned by an event data
 * expression) and returns the id of the first state node the tree maps it to,
 * or -1. Mirrors getClosestStateNodeIdToDomNode.
 */
export function getClosestStateNodeIdToDomNode(
  stateTree: ClosestLookupTree,
  domNodeReference: unknown,
  eventDataExpression: string
): number {
  if (domNodeReference === null || domNodeReference === undefined) {
    return -1;
  }
  try {
    let targetNode = domNodeReference as Node | null;
    while (targetNode !== null) {
      const stateNodeForDomNode = stateTree.getStateNodeForDomNode(targetNode);
      if (stateNodeForDomNode !== null) {
        return stateNodeForDomNode.getId();
      }
      targetNode = targetNode.parentNode;
    }
  } catch (e) {
    // not going to let event handling fail; just report nothing found
    console.debug(
      `An error occurred when Flow tried to find a state node matching the element ${String(
        domNodeReference
      )}, returned by an event data expression ${eventDataExpression}. Error: ${(e as Error).message}`
    );
  }
  return -1;
}

// --- Slice 3: styling binding ----------------------------------------------
// The class-list and style-property binding parts of bind(). Both go through
// the ported DomApi/native CSS; the property and attribute binders
// (updateProperty/updateAttribute) wait on PolymerUtils.createModelTree and
// WidgetUtil.updateAttribute, which are not ported yet.

/** The slice of MapProperty that updateStyleProperty reads. */
interface StyleMapProperty {
  getName(): string;
  hasValue(): boolean;
  getValue(): unknown;
}

/** The splice-event slice that the class-list listener reads. */
interface ClassListSpliceEvent {
  getRemove(): unknown[];
  getAdd(): unknown[];
}

/** The slice of NodeList that holds the class names. */
interface ClassNodeList {
  length(): number;
  get(index: number): unknown;
  addSpliceListener(listener: (event: ClassListSpliceEvent) => void): EventRemover;
}

/** The slice of StateNode that bindClassList reads. */
interface ClassListNode {
  getList(featureId: number): ClassNodeList;
}

/**
 * Updates a single inline style property of the element from a map property,
 * preserving an `!important` priority, or removes it when the property has no
 * value. Mirrors updateStyleProperty.
 */
export function updateStyleProperty(mapProperty: StyleMapProperty, element: HTMLElement): void {
  const name = mapProperty.getName();
  const styleElement = element.style;
  if (mapProperty.hasValue()) {
    const value = mapProperty.getValue() as string;
    let styleIsSet = false;
    if (value.includes('!important')) {
      const temp = document.createElement(element.tagName);
      const tmpStyle = temp.style;
      tmpStyle.cssText = `${name}: ${value};`;
      const priority = 'important';
      if (priority === temp.style.getPropertyPriority(name)) {
        styleElement.setProperty(name, temp.style.getPropertyValue(name), priority);
        styleIsSet = true;
      }
    }
    if (!styleIsSet) {
      styleElement.setProperty(name, value);
    }
  } else {
    styleElement.removeProperty(name);
  }
}

/**
 * Binds the CLASS_LIST feature of the node to the element's class list,
 * applying the current classes and keeping them in sync as the list is spliced.
 * Mirrors bindClassList.
 */
export function bindClassList(element: Element, node: ClassListNode): EventRemover {
  const classNodeList = node.getList(NodeFeatures.CLASS_LIST);

  for (let i = 0; i < classNodeList.length(); i++) {
    wrap(element).classList.add(classNodeList.get(i) as string);
  }

  return classNodeList.addSpliceListener((e) => {
    const classList = wrap(element).classList;

    e.getRemove().forEach((token) => classList.remove(token as string));
    e.getAdd().forEach((token) => classList.add(token as string));
  });
}

// --- Slice 4: attribute binding --------------------------------------------
// The element-attribute part of bind(). updateAttributeValue resolves a "uri"
// model object against the application configuration (web-component mode); the
// underlying attribute set/remove goes through WidgetUtil/DomApi.

/** The slice of ApplicationConfiguration that attribute binding reads. */
interface AttributeConfiguration {
  isWebComponentMode(): boolean;
  getServiceUrl(): string;
}

/** The Registry → ApplicationConfiguration chain reached from a node's tree. */
interface AttributeMapProperty {
  getName(): string;
  getValue(): unknown;
  getMap(): {
    getNode(): { getTree(): { getRegistry(): { getApplicationConfiguration(): AttributeConfiguration } } };
  };
}

/**
 * Sets an element attribute from a map-property value. A plain string (or null)
 * is applied as-is; a "uri" model object is resolved against the application
 * configuration (prefixing the service URL in web-component mode for relative
 * URIs); anything else is stringified. Mirrors updateAttributeValue.
 */
export function updateAttributeValue(
  configuration: AttributeConfiguration,
  element: Element,
  attribute: string,
  value: unknown
): void {
  if (value === null || value === undefined || typeof value === 'string') {
    setElementAttribute(element, attribute, (value ?? null) as string | null);
  } else if (typeof value === 'object' && !Array.isArray(value)) {
    const uri = (value as Record<string, unknown>)[NodeProperties.URI_ATTRIBUTE] as string;
    if (configuration.isWebComponentMode() && !isAbsoluteUrl(uri)) {
      let baseUri = configuration.getServiceUrl();
      baseUri = baseUri.endsWith('/') ? baseUri : `${baseUri}/`;
      setElementAttribute(element, attribute, baseUri + uri);
    } else {
      setElementAttribute(element, attribute, uri);
    }
  } else {
    setElementAttribute(element, attribute, String(value));
  }
}

/**
 * Updates the named element attribute from a map property, resolving the
 * application configuration from the property's node. Mirrors updateAttribute.
 */
export function updateAttribute(mapProperty: AttributeMapProperty, element: Element): void {
  updateAttributeValue(
    mapProperty.getMap().getNode().getTree().getRegistry().getApplicationConfiguration(),
    element,
    mapProperty.getName(),
    mapProperty.getValue()
  );
}
