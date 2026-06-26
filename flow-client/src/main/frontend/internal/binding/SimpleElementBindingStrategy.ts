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
// client class, so it is built up across several build-alongside slices, each a
// labelled section below: event-data resolution, closest-state-node lookups,
// styling binding, attribute binding, creation & identity, and visibility
// binding. The methods are currently standalone functions; they get assembled
// into the BindingStrategy<Element> class once bind() and its remaining
// DOM-structure/event/shadow machinery (which need the BindingContext type) are
// ported. The Polymer model-property bridge stays in
// internal/SimpleElementBindingStrategy.ts (window-registered) and is imported
// at cutover.

import { wrap } from '../dom/DomApi';
import { NodeFeatures, NodeProperties } from '../nodefeature/NodeFeatures';
import { isInShadowRoot } from '../PolymerUtils';
import { Reactive, type Computation, type EventRemover } from '../reactive/reactive';
import { getJsProperty, isAbsoluteUrl, updateAttribute as setElementAttribute } from '../WidgetUtil';
import type { BinderContext } from './BindingStrategy';
import { Debouncer } from './Debouncer';

// com.vaadin.client.flow.binding.SimpleElementBindingStrategy.HIDDEN_ATTRIBUTE
const HIDDEN_ATTRIBUTE = 'hidden';

// com.vaadin.flow.shared.JsonConstants tokens used by handleDomEvent.
const EVENT_DATA_PHASE = 'for';
const SYNCHRONIZE_PROPERTY_TOKEN = '}';
const MAP_STATE_NODE_EVENT_DATA = ']';

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

// --- Slice 5: element creation & identity ----------------------------------
// The standalone, BindingContext-free parts of the strategy: creating the DOM
// element for a state node, the applicability/tag/visibility checks and the
// rebind check. They are assembled into the BindingStrategy<Element> class once
// bind() (and its DOM-structure/event machinery) is ported.

/** The slice of StateNode that creation & identity read. */
interface CreationNode {
  getMap(featureId: number): { getProperty(name: string): { getValue(): unknown } };
  hasFeature(featureId: number): boolean;
  getParent(): CreationNode | null;
  getDomNode(): Node | null;
  getTree(): { getRootNode(): CreationNode; isVisible(node: CreationNode): boolean } | null;
}

function readElementData(node: CreationNode, property: string): unknown {
  return node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(property).getValue();
}

/** The element tag for the state node; mirrors PolymerUtils.getTag / getTag. */
export function getTag(node: CreationNode): string | null {
  return (readElementData(node, NodeProperties.TAG) as string | null) ?? null;
}

/** The element namespace for the state node, if any; mirrors getNamespace. */
export function getNamespace(node: CreationNode): string | null {
  return (readElementData(node, NodeProperties.NAMESPACE) as string | null) ?? null;
}

/**
 * Creates the DOM element for the state node, using the node's namespace, then
 * the parent element's namespace, then no namespace. Mirrors create.
 */
export function create(node: CreationNode): Element {
  const tag = getTag(node) as string;
  const namespace = getNamespace(node);
  if (namespace !== null) {
    return document.createElementNS(namespace, tag);
  }
  const parent = node.getParent();
  if (parent !== null) {
    const namespaceURI = (parent.getDomNode() as Element | null)?.namespaceURI ?? null;
    if (namespaceURI !== null) {
      return document.createElementNS(namespaceURI, tag);
    }
  }
  return document.createElement(tag);
}

/** Whether this strategy applies to the state node; mirrors isApplicable. */
export function isApplicable(node: CreationNode): boolean {
  if (node.hasFeature(NodeFeatures.ELEMENT_DATA)) {
    return true;
  }
  const tree = node.getTree();
  return tree !== null && node === tree.getRootNode();
}

/** Whether the element's tag matches the node's required tag; mirrors hasSameTag. */
export function hasSameTag(node: CreationNode, element: Element): boolean {
  const nsTag = getTag(node);
  return nsTag === null || element.tagName.toLowerCase() === nsTag.toLowerCase();
}

/**
 * Whether the node needs a re-bind. Absence of value or "true" means no
 * re-bind; only an explicit false means a re-bind is needed. Mirrors needsRebind.
 */
export function needsRebind(node: CreationNode): boolean {
  return readElementData(node, NodeProperties.VISIBILITY_BOUND_PROPERTY) === false;
}

/** Whether the node is visible; mirrors isVisible. */
export function isVisible(node: CreationNode): boolean {
  const tree = node.getTree();
  return tree !== null && tree.isVisible(node);
}

// --- Slice 6: visibility binding -------------------------------------------
// The element-visibility helpers of bind(): they capture the element's initial
// hidden attribute and inline display once, hide an invisible element, restore
// the captured state when it becomes visible again, and apply structural
// attributes (the "slot") even while invisible. The bindVisibility/
// updateVisibility wiring (which needs BindingContext, remove and doBind) lands
// with the bind() core.

/** The slice of MapProperty the visibility helpers read and write. */
interface VisibilityProperty {
  hasValue(): boolean;
  getValue(): unknown;
  setValue(value: unknown): void;
}

/** The slice of the ELEMENT_DATA NodeMap the visibility helpers use. */
interface VisibilityNodeMap {
  getProperty(name: string): VisibilityProperty;
  getNode(): { getTree(): { getRegistry(): { getApplicationConfiguration(): AttributeConfiguration } } };
}

/** The slice of StateNode applyStructuralAttributes reads. */
interface StructuralAttributesNode {
  hasFeature(featureId: number): boolean;
  getMap(featureId: number): {
    hasPropertyValue(name: string): boolean;
    getProperty(name: string): AttributeMapProperty;
  };
}

/**
 * Captures the element's initial `hidden` attribute and (in a shadow root) its
 * inline display into the visibility data, once. Mirrors
 * storeInitialHiddenAttribute.
 */
export function storeInitialHiddenAttribute(element: Element, visibilityData: VisibilityNodeMap): void {
  const initialVisibility = visibilityData.getProperty(NodeProperties.VISIBILITY_HIDDEN_PROPERTY);
  if (!initialVisibility.hasValue()) {
    initialVisibility.setValue(element.getAttribute(HIDDEN_ATTRIBUTE));
  }

  const initialDisplay = visibilityData.getProperty(NodeProperties.VISIBILITY_STYLE_DISPLAY_PROPERTY);
  if (isInShadowRoot(element) && !initialDisplay.hasValue()) {
    initialDisplay.setValue((element as HTMLElement).style.display);
  }
}

/**
 * Restores the element's captured initial hidden attribute and inline display.
 * Mirrors restoreInitialHiddenAttribute.
 */
export function restoreInitialHiddenAttribute(element: Element, visibilityData: VisibilityNodeMap): void {
  storeInitialHiddenAttribute(element, visibilityData);
  const configuration = visibilityData.getNode().getTree().getRegistry().getApplicationConfiguration();

  const initialVisibility = visibilityData.getProperty(NodeProperties.VISIBILITY_HIDDEN_PROPERTY);
  if (initialVisibility.hasValue()) {
    updateAttributeValue(configuration, element, HIDDEN_ATTRIBUTE, initialVisibility.getValue());
  }

  const initialDisplay = visibilityData.getProperty(NodeProperties.VISIBILITY_STYLE_DISPLAY_PROPERTY);
  if (initialDisplay.hasValue()) {
    (element as HTMLElement).style.display = String(initialDisplay.getValue());
  }
}

/**
 * Hides the element: stores its initial state, sets `hidden`, and (in a shadow
 * root) sets display:none. Mirrors setElementInvisible.
 */
export function setElementInvisible(element: Element, visibilityData: VisibilityNodeMap): void {
  storeInitialHiddenAttribute(element, visibilityData);
  const configuration = visibilityData.getNode().getTree().getRegistry().getApplicationConfiguration();
  updateAttributeValue(configuration, element, HIDDEN_ATTRIBUTE, true);
  if (isInShadowRoot(element)) {
    (element as HTMLElement).style.display = 'none';
  }
}

/**
 * Applies structural attributes (the "slot") to the element even while it is
 * invisible, preserving CSS selectors without exposing backend data. Mirrors
 * applyStructuralAttributes.
 */
export function applyStructuralAttributes(stateNode: StructuralAttributesNode, element: Element): void {
  if (stateNode.hasFeature(NodeFeatures.ELEMENT_ATTRIBUTES)) {
    const attributeMap = stateNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);
    if (attributeMap.hasPropertyValue(NodeProperties.SLOT_ATTRIBUTE)) {
      updateAttribute(attributeMap.getProperty(NodeProperties.SLOT_ATTRIBUTE), element);
    }
  }
}

// --- Slice 7: DOM event listeners ------------------------------------------
// Binds the ELEMENT_LISTENERS feature to real DOM event listeners and dispatches
// fired events to the server. handleDomEvent ties together the slice-1 filter/
// debounce resolution, the slice-2 closest-node lookups and the event-expression
// cache. This is the first slice that needs the BindingContext.

/** The slice of MapProperty the event cluster reads. */
interface EventMapProperty {
  getName(): string;
  hasValue(): boolean;
  getValue(): unknown;
  getSyncToServerCommand(newValue: unknown): () => void;
  setPreviousDomValue(value: unknown): void;
}

/** The slice of NodeMap the event cluster reads. */
interface EventNodeMap {
  getProperty(name: string): EventMapProperty;
  forEachProperty(callback: (property: EventMapProperty, name: string) => void): void;
  addPropertyAddListener(listener: (event: { getProperty(): EventMapProperty }) => void): EventRemover;
}

/** The slice of StateTree the event cluster reads. */
interface EventTree {
  getRegistry(): { getConstantPool(): { has(key: string): boolean; get<T>(key: string): T } };
  sendEventToServer(node: BindingStateNode, type: string, eventData: unknown): void;
  getStateNodeForDomNode(domNode: Node): { getId(): number } | null;
}

/** The slice of StateNode the binding context exposes. */
interface BindingStateNode {
  getId(): number;
  getDomNode(): Node | null;
  getMap(featureId: number): EventNodeMap;
  getList(featureId: number): { forEach(callback: (child: unknown) => void): void };
  getTree(): EventTree;
}

/**
 * Holds the data the binding operations pass around: the state node, its DOM
 * node, the binder context for child nodes, and the per-event-type listener
 * bookkeeping. Mirrors the BindingContext inner class.
 */
export class BindingContext {
  readonly node: BindingStateNode;

  readonly htmlNode: Node;

  readonly binderContext: BinderContext;

  readonly listenerBindings = new Map<string, Computation>();

  readonly listenerRemovers = new Map<string, EventRemover>();

  constructor(node: BindingStateNode, htmlNode: Node, binderContext: BinderContext) {
    this.node = node;
    this.htmlNode = htmlNode;
    this.binderContext = binderContext;
  }
}

function getDomEventListenerMap(node: BindingStateNode): EventNodeMap {
  return node.getMap(NodeFeatures.ELEMENT_LISTENERS);
}

/**
 * Binds the ELEMENT_LISTENERS feature to DOM event listeners, adding listeners
 * for the current handlers and tracking later additions. Mirrors
 * bindDomEventListeners.
 */
export function bindDomEventListeners(context: BindingContext): EventRemover {
  const elementListeners = getDomEventListenerMap(context.node);
  elementListeners.forEachProperty((property) => {
    // Run eagerly to add initial listeners before the element is attached.
    bindEventHandlerProperty(property, context).recompute();
  });

  return elementListeners.addPropertyAddListener((event) => bindEventHandlerProperty(event.getProperty(), context));
}

function bindEventHandlerProperty(eventHandlerProperty: EventMapProperty, context: BindingContext): Computation {
  const name = eventHandlerProperty.getName();

  const computation = Reactive.runWhenDependenciesChange(() => {
    const hasValue = eventHandlerProperty.hasValue();
    const hasListener = context.listenerRemovers.has(name);

    if (hasValue !== hasListener) {
      if (hasValue) {
        addEventHandler(name, context);
      } else {
        removeEventHandler(name, context);
      }
    }
  });

  context.listenerBindings.set(name, computation);

  return computation;
}

function removeEventHandler(eventType: string, context: BindingContext): void {
  const remover = context.listenerRemovers.get(eventType);
  context.listenerRemovers.delete(eventType);
  remover?.remove();
}

function addEventHandler(eventType: string, context: BindingContext): void {
  const handler = (event: Event): void => handleDomEvent(event, context);
  context.htmlNode.addEventListener(eventType, handler, false);
  context.listenerRemovers.set(eventType, {
    remove: () => context.htmlNode.removeEventListener(eventType, handler, false)
  });
}

function getSyncPropertyCommand(propertyName: string, context: BindingContext): () => void {
  return context.node
    .getMap(NodeFeatures.ELEMENT_PROPERTIES)
    .getProperty(propertyName)
    .getSyncToServerCommand(getJsProperty(context.htmlNode as unknown as Record<string, unknown>, propertyName));
}

function sendEventToServer(
  node: BindingStateNode,
  type: string,
  eventData: Record<string, unknown> | null,
  debouncePhase: string | null
): void {
  let data = eventData;
  if (debouncePhase === null) {
    if (data !== null) {
      // eslint-disable-next-line @typescript-eslint/no-dynamic-delete -- removes the debounce-phase marker before sending
      delete data[EVENT_DATA_PHASE];
    }
  } else {
    data ??= {};
    data[EVENT_DATA_PHASE] = debouncePhase;
  }

  node.getTree().sendEventToServer(node, type, data);
}

/**
 * Handles a fired DOM event: collects the server-requested event data
 * (expressions, synchronized properties, mapped state nodes), resolves the
 * event filters/debounces, and sends the event to the server. Mirrors
 * handleDomEvent.
 */
export function handleDomEvent(event: Event, context: BindingContext): void {
  const element = context.htmlNode as Element;
  const node = context.node;
  const type = event.type;

  const listenerMap = getDomEventListenerMap(node);
  const constantPool = node.getTree().getRegistry().getConstantPool();
  const expressionConstantKey = listenerMap.getProperty(type).getValue() as string;

  const expressionSettings = constantPool.get<Record<string, unknown>>(expressionConstantKey);
  const expressions = Object.keys(expressionSettings);

  const eventData: Record<string, unknown> | null = expressions.length === 0 ? null : {};
  const synchronizeProperties = new Set<string>();

  for (const expressionString of expressions) {
    if (expressionString.startsWith(SYNCHRONIZE_PROPERTY_TOKEN)) {
      synchronizeProperties.add(expressionString.substring(SYNCHRONIZE_PROPERTY_TOKEN.length));
    } else if (expressionString === MAP_STATE_NODE_EVENT_DATA) {
      // map event.target to the closest state node
      eventData![MAP_STATE_NODE_EVENT_DATA] = getClosestStateNodeIdToEventTarget(node, event.target);
    } else if (expressionString.startsWith(MAP_STATE_NODE_EVENT_DATA)) {
      // map an element returned by JS to the closest state node
      const jsEvaluation = expressionString.substring(MAP_STATE_NODE_EVENT_DATA.length);
      const expressionValue = getOrCreateExpression(jsEvaluation)(event, element);
      eventData![expressionString] = getClosestStateNodeIdToDomNode(node.getTree(), expressionValue, jsEvaluation);
    } else {
      eventData![expressionString] = getOrCreateExpression(expressionString)(event, element);
    }
  }

  synchronizeProperties.forEach((name) => {
    const property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty(name);
    const domValue = getJsProperty(element as unknown as Record<string, unknown>, name);
    property.setPreviousDomValue(domValue);
  });

  const commands = new Map<string, () => void>();
  synchronizeProperties.forEach((name) => commands.set(name, getSyncPropertyCommand(name, context)));

  const sendCommand = (debouncePhase: string | null): void => sendEventToServer(node, type, eventData, debouncePhase);

  const sendNow = resolveFilters(element, type, expressionSettings, eventData, sendCommand, commands);

  if (sendNow) {
    // Send if there were no filters or at least one matched.
    let commandAlreadyExecuted = false;
    const flushPendingChanges = synchronizeProperties.size === 0;

    if (flushPendingChanges) {
      // Flush all debounced events so they don't arrive out of order on the server.
      commandAlreadyExecuted = Debouncer.flushAll().includes(sendCommand);
    }

    if (!commandAlreadyExecuted) {
      commands.forEach((command) => command());
      sendCommand(null);
    }
  }
}
