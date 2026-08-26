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
// client class; its members follow the Java declaration order, and the
// module-local functions are assembled into the exported
// BindingStrategy<Element> class at the bottom of the module. The Polymer
// model-property bridge -- the JSNI parts of the Java class, which patch Polymer
// internals -- lives in PolymerModelBinding.ts beside this module and is called
// from bind().
//
// SimpleElementBindingStrategy.java also declares a private INITIAL_CHANGE
// constant and a private invokeWhenNodeIsConstructed method; both are unused in
// the Java source, so neither is ported.

import { assert } from '../../../assert';
import { getElementById, getElementByName, hasTag } from '../../ElementUtil';
import { isLitElement, whenRendered } from '../../LitUtils';
import { UpdatableModelProperties } from '../model/UpdatableModelProperties';
import { JsonConstants } from '../../../flow/shared/JsonConstants';
import { NodeFeatures } from '../../../flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../../../flow/internal/nodefeature/NodeProperties';
import { createModelTree } from '../../PolymerModelTree';
import {
  addReadyListener,
  fireReadyEvent,
  getCustomElement,
  getDomRoot,
  getTag as polymerGetTag,
  isInShadowRoot,
  isReady
} from '../../PolymerUtils';
import { addReadyCallback, isInitialized } from '../../ReactUtils';
import type { EventRemover } from '../../../EventRemover';
import type { Computation } from '../reactive/Computation';
import { Reactive } from '../reactive/Reactive';
import { bindPolymerModelProperties } from './PolymerModelBinding';
import { StateNode } from '../StateNode';
import type { ApplicationConfiguration, StateTree } from '../StateTree';
import type { MapProperty } from '../nodefeature/MapProperty';
import type { NodeList } from '../nodefeature/NodeList';
import type { NodeMap } from '../nodefeature/NodeMap';
import type { ListSpliceEvent } from '../nodefeature/ListSpliceEvent';
import {
  deleteJsProperty,
  equalsInJS,
  getJsProperty,
  getKeys,
  hasOwnJsProperty,
  isAbsoluteUrl,
  isUndefined,
  setJsProperty,
  updateAttribute as setElementAttribute
} from '../../WidgetUtil';
import type { BinderContext } from './BinderContext';
import type { BindingStrategy } from './BindingStrategy';
import { Debouncer } from './Debouncer';
import { bindServerEventHandlerNames } from './ServerEventHandlerBinder';
import { Console } from '../../Console';

// com.vaadin.client.flow.binding.SimpleElementBindingStrategy.HIDDEN_ATTRIBUTE
const HIDDEN_ATTRIBUTE = 'hidden';

// com.vaadin.client.flow.binding.SimpleElementBindingStrategy.ELEMENT_ATTACH_ERROR_PREFIX
const ELEMENT_ATTACH_ERROR_PREFIX = 'Element addressed by the ';

// com.vaadin.flow.shared.JsonConstants tokens used by handleDomEvent, referenced
// by name rather than re-declaring their literal values.
const EVENT_DATA_PHASE = JsonConstants.EVENT_DATA_PHASE;
const SYNCHRONIZE_PROPERTY_TOKEN = JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN;
const MAP_STATE_NODE_EVENT_DATA = JsonConstants.MAP_STATE_NODE_EVENT_DATA;

// The callback sending an event to the server for a given debounce phase (null
// when sent outside any debounce). Compatible with Debouncer's send command.
type SendCommand = (phase: string | null) => void;

// A synchronization command run before an event is sent.
type Command = () => void;

/**
 * Callback interface for an event expression parsed using new Function() in
 * JavaScript.
 *
 * @param event - Event to expand
 * @param element - target Element
 * @returns Result of evaluated function
 */
type EventExpression = (event: Event, element: Element) => unknown;

let expressionCache: Map<string, EventExpression> | null = null;

/**
 * This is used as a weak set. Only keys are important so that they are weakly
 * referenced
 */
const boundNodes = new WeakMap<object, boolean>();

/**
 * Just a context class whose instance is passed as a parameter between the
 * operations of various kind to be able to access the data like listeners, node
 * and element which they operate on.
 *
 * It's used to avoid having methods with a long numbers of parameters and
 * because the strategy instance is stateless.
 */
class BindingContext {
  // Java declares these private and reaches them from the enclosing class. A
  // JavaScript #private is class-scoped, and here the enclosing class is the
  // module, so the fields and the constructor are readonly-public instead.
  readonly htmlNode: Node;

  readonly node: StateNode;

  readonly binderContext: BinderContext;

  readonly listenerBindings = new Map<string, Computation>();

  readonly listenerRemovers = new Map<string, EventRemover>();

  constructor(node: StateNode, htmlNode: Node, binderContext: BinderContext) {
    this.node = node;
    this.htmlNode = htmlNode;
    this.binderContext = binderContext;
  }
}

/**
 * Holds the deferred initial property update for a node: the command is run once
 * (via execute) and then the holder removes itself from the node's data. Mirrors
 * the InitialPropertyUpdate inner class.
 */
class InitialPropertyUpdate {
  // setCommand and execute are private in Java, reached from the enclosing
  // class; as above, the module is the enclosing scope here, so they are public.
  #command: (() => void) | null = null;

  readonly #node: StateNode;

  constructor(node: StateNode) {
    this.#node = node;
  }

  setCommand(command: () => void): void {
    this.#command = command;
  }

  execute(): void {
    this.#command?.();
    this.#node.clearNodeData(this);
  }
}

function readElementData(node: StateNode, property: string): unknown {
  return node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(property).getValue();
}

/** The element namespace for the state node, if any; mirrors getNamespace. */
function getNamespace(node: StateNode): string | null {
  return (readElementData(node, NodeProperties.NAMESPACE) as string | null) ?? null;
}

/**
 * The element tag for the state node.
 *
 * Java has a single implementation, PolymerUtils.getTag, which this strategy
 * calls; delegate to the ported one rather than reading ELEMENT_DATA again.
 */
function getTag(node: StateNode): string | null {
  return polymerGetTag(node) ?? null;
}

/**
 * Creates the DOM element for the state node, using the node's namespace, then
 * the parent element's namespace, then no namespace. Mirrors create.
 */
function create(node: StateNode): Element {
  const tag = getTag(node);
  assert(tag !== null, 'New child must have a tag');
  const namespace = getNamespace(node);
  if (namespace !== null) {
    return document.createElementNS(namespace, tag);
  }
  const parent = node.getParent();
  if (parent !== null) {
    // Java dereferences the parent's DOM node unconditionally; mirror that with
    // a non-null assertion rather than an optional chain so a missing DOM node
    // fails here instead of silently falling through to a non-namespaced tag.
    const namespaceURI = (parent.getDomNode()! as Element).namespaceURI;
    if (namespaceURI !== null) {
      return document.createElementNS(namespaceURI, tag);
    }
  }
  return document.createElement(tag);
}

/** Whether this strategy applies to the state node; mirrors isApplicable. */
function isApplicable(node: StateNode): boolean {
  if (node.hasFeature(NodeFeatures.ELEMENT_DATA)) {
    return true;
  }
  const tree = node.getTree();
  return tree !== null && node === tree.getRootNode();
}

// Mirrors GWT Scheduler.scheduleDeferred: run after the current task.
function scheduleDeferred(command: () => void): void {
  setTimeout(command, 0);
}

/**
 * Schedules the deferred initial property update: stores an InitialPropertyUpdate
 * on the node and, after the initial reactive flush, runs it (unless
 * handlePropertiesChanged already cleared it). Mirrors scheduleInitialExecution.
 */
function scheduleInitialExecution(stateNode: StateNode): void {
  const update = new InitialPropertyUpdate(stateNode);
  stateNode.setNodeData(update);
  // Run after all initial reactive work, so initial JS runs before this update.
  Reactive.addPostFlushListener(() =>
    scheduleDeferred(() => {
      // cleared if handlePropertiesChanged already ran
      stateNode.getNodeData(InitialPropertyUpdate)?.execute();
    })
  );
}

/**
 * Syncs a dom-repeat list-item property change to the server. The tree (a
 * singleton) is passed explicitly because this runs from a replaced prototype
 * method without the binding closure's context. Mirrors
 * handleListItemPropertyChange.
 */
// eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java handleListItemPropertyChange signature
function handleListItemPropertyChange(
  nodeId: number,
  host: Element,
  property: string,
  value: unknown,
  tree: StateTree
): void {
  // Warning : it's important that `tree` is passed as an
  // argument instead of StateNode or Element ! We have replaced a method
  // in the prototype which means that it may not use the context from the
  // hookUpPolymerElement method. Only a tree may be use as a context
  // since StateTree is a singleton.
  // Java dereferences the looked-up node unguarded, so a missing node throws
  // there; the non-null assertion keeps that contract instead of silently
  // returning.
  const node = tree.getNode(Math.trunc(nodeId))!;
  if (!node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
    return;
  }

  assert(
    checkParent(node, host),
    'Host element is not a parent of the node whose property has changed. ' +
      'This is an implementation error. ' +
      'Most likely it means that there are several StateTrees on the same page ' +
      '(might be possible with portlets) and the target StateTree should not be passed ' +
      'into the method as an argument but somehow detected from the host element. ' +
      'Another option is that host element is calculated incorrectly.'
  );

  // TODO: this code doesn't care about "security feature" which prevents sending
  // data from the client side to the server side if property is not
  // "updatable". See `handlePropertyChange` and UpdatableModelProperties.
  // It should be aware of that. The current issue is that we don't know
  // the full property path (dot separated) to the property which is a
  // property for the `host` StateNode and not
  // for the `node` below. It's tricky to calculate FQN
  // property name at this point though the `host` element which is
  // the template element could be used for that: a StateNode of
  // `host` is an ancestor of the `node` and it
  // should be possible to calculate FQN using this info. Also at the moment
  // AllowClientUpdates ignores bean properties in
  // lists ( if "list" is a property name of list type property and
  // "name" is a property of a bean then
  // "list.name" is not in the UpdatableModelProperties ).
  node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty(property).syncToServer(value);
}

/**
 * Whether supposedParent is an ancestor of the node, walking the state-node
 * parents; mirrors checkParent.
 */
function checkParent(node: StateNode, supposedParent: Element): boolean {
  let parent: StateNode | null = node;
  for (;;) {
    parent = parent.getParent();
    if (parent === null) {
      return false;
    }
    if (supposedParent === parent.getDomNode()) {
      return true;
    }
  }
}

/**
 * Handles the set of changed property paths from a Polymer element, running the
 * updates now or deferring them until the initial update if one is pending.
 * Mirrors handlePropertiesChanged.
 */
function handlePropertiesChanged(changedPropertyPathsToValues: object, node: StateNode): void {
  const keys = getKeys(changedPropertyPathsToValues);

  const runnable = (): void => {
    for (const propertyName of keys) {
      handlePropertyChange(
        propertyName,
        () => getJsProperty(changedPropertyPathsToValues as Record<string, unknown>, propertyName),
        node
      );
    }
  };

  const initialUpdate = node.getNodeData(InitialPropertyUpdate);
  if (initialUpdate === null) {
    runnable();
  } else {
    initialUpdate.setCommand(runnable);
  }
}

/**
 * Handles a single changed property path, sending the update to the server only
 * if the property is in the node's updatable-properties "security feature" and
 * isn't a model/list node. Mirrors handlePropertyChange.
 */
function handlePropertyChange(fullPropertyName: string, valueProvider: () => unknown, node: StateNode): void {
  const updatableProperties = node.getNodeData(UpdatableModelProperties);
  if (updatableProperties === null || !updatableProperties.isUpdatableProperty(fullPropertyName)) {
    // not an updatable property/sub-property: do nothing
    return;
  }

  // Walk the dot-separated path; this resolves the parent node of the property.
  const subProperties = fullPropertyName.split('.');
  let model: StateNode = node;
  let mapProperty: MapProperty | null = null;
  const size = subProperties.length;
  let i = 0;
  for (const subProperty of subProperties) {
    const elementProperties = model.getMap(NodeFeatures.ELEMENT_PROPERTIES);
    if (!elementProperties.hasPropertyValue(subProperty) && i < size - 1) {
      Console.debug(`Ignoring property change for property '${fullPropertyName}' which isn't defined from server`);
      return;
    }

    mapProperty = elementProperties.getProperty(subProperty);
    if (mapProperty.getValue() instanceof StateNode) {
      model = mapProperty.getValue() as StateNode;
    }
    i++;
  }

  if (mapProperty!.getValue() instanceof StateNode) {
    // Don't send updates for list nodes
    const nodeValue = mapProperty!.getValue() as StateNode;
    // Java reads the value through WidgetUtil.crazyJsCast, a GWT-compiler-only
    // unchecked cast with no port; the value is used directly here.
    const obj = valueProvider() as Record<string, unknown>;
    if (obj.nodeId === undefined || nodeValue.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
      return;
    }
  }
  mapProperty!.syncToServer(valueProvider());
}

/**
 * Binds the element's shadow root: attaches it now and re-attaches whenever the
 * SHADOW_ROOT_DATA feature gains the shadow-root node. Mirrors bindShadowRoot.
 */
function bindShadowRoot(context: BindingContext): EventRemover {
  assert(context.htmlNode instanceof Element, 'Cannot bind shadow root to a Node');
  const map = context.node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
  attachShadow(context);
  return map.addPropertyAddListener(() => Reactive.addFlushListener(() => attachShadow(context)));
}

function attachShadow(context: BindingContext): void {
  const map = context.node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
  const shadowRootNode = map.getProperty(NodeProperties.SHADOW_ROOT).getValue() as StateNode | null;
  if (shadowRootNode !== null) {
    const element = context.htmlNode as Element;
    const shadowRoot = element.shadowRoot ?? element.attachShadow({ mode: 'open' });

    if (shadowRootNode.getDomNode() === null) {
      shadowRootNode.setDomNode(shadowRoot);
    }

    bindChildren(new BindingContext(shadowRootNode, shadowRoot, context.binderContext));
  }
}

/**
 * Creates a fresh per-feature computation map and tracks it in the collection
 * (used to stop the computations on rebind). Mirrors createComputations.
 */
function createComputations(computationsCollection: Array<Map<string, Computation>>): Map<string, Computation> {
  const computations = new Map<string, Computation>();
  computationsCollection.push(computations);
  return computations;
}

/** Whether the element's tag matches the node's required tag; mirrors hasSameTag. */
function hasSameTag(node: StateNode, element: Element): boolean {
  const nsTag = getTag(node);
  return nsTag === null || element.tagName.toLowerCase() === nsTag.toLowerCase();
}

/**
 * Binds every property of the node's feature map to the user, applying current
 * properties eagerly and observing later additions. Mirrors bindMap.
 */
function bindMap(
  featureId: number,
  user: (property: MapProperty) => void,
  bindings: Map<string, Computation>,
  node: StateNode
): EventRemover {
  const map = node.getMap(featureId);
  // Run eagerly to apply initial property values.
  map.forEachProperty((property) => bindProperty(user, property, bindings).recompute());

  return map.addPropertyAddListener((event) => bindProperty(user, event.getProperty(), bindings));
}

/**
 * Binds the node's visibility: records the current bound state, applies it, and
 * re-applies whenever the VISIBLE property changes. Mirrors bindVisibility.
 */
function bindVisibility(
  listeners: EventRemover[],
  context: BindingContext,
  computationsCollection: Array<Map<string, Computation>>,
  nodeFactory: BinderContext
): EventRemover {
  assert(
    context.htmlNode instanceof Element,
    `The HTML node for the StateNode with id=${context.node.getId()} is not an Element`
  );
  const visibilityData = context.node.getMap(NodeFeatures.ELEMENT_DATA);

  visibilityData.getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY).setValue(isVisible(context.node));
  updateVisibility(listeners, context, computationsCollection, nodeFactory);

  return visibilityData
    .getProperty(NodeProperties.VISIBLE)
    .addChangeListener(() => updateVisibility(listeners, context, computationsCollection, nodeFactory));
}

/** Whether the node is visible; mirrors isVisible. */
function isVisible(node: StateNode): boolean {
  return node.getTree().isVisible(node);
}

function updateVisibility(
  listeners: EventRemover[],
  context: BindingContext,
  computationsCollection: Array<Map<string, Computation>>,
  nodeFactory: BinderContext
): void {
  assert(
    context.htmlNode instanceof Element,
    `The HTML node for the StateNode with id=${context.node.getId()} is not an Element`
  );
  const element = context.htmlNode;
  const node = context.node;
  const visibilityData = node.getMap(NodeFeatures.ELEMENT_DATA);

  if (needsRebind(node) && isVisible(node)) {
    remove(listeners, context, computationsCollection);
    Reactive.addFlushListener(() => {
      restoreInitialHiddenAttribute(element, visibilityData);
      doBind(node, nodeFactory);
    });
  } else if (isVisible(node)) {
    visibilityData.getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY).setValue(true);
    restoreInitialHiddenAttribute(element, visibilityData);
  } else {
    setElementInvisible(element, visibilityData);
  }
}

/**
 * Hides the element: stores its initial state, sets `hidden`, and (in a shadow
 * root) sets display:none. Mirrors setElementInvisible.
 */
function setElementInvisible(element: Element, visibilityData: NodeMap): void {
  storeInitialHiddenAttribute(element, visibilityData);
  const configuration = visibilityData.getNode().getTree().getRegistry().getApplicationConfiguration();
  updateAttributeValue(configuration, element, HIDDEN_ATTRIBUTE, true);
  if (isInShadowRoot(element)) {
    (element as HTMLElement).style.display = 'none';
  }
}

/**
 * Applies structural attributes (like "slot") to the element even when it is
 * initially invisible. This preserves CSS selectors that depend on these
 * attributes without exposing backend data.
 */
function applyStructuralAttributes(stateNode: StateNode, element: Element): void {
  if (stateNode.hasFeature(NodeFeatures.ELEMENT_ATTRIBUTES)) {
    const attributeMap = stateNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);
    if (attributeMap.hasPropertyValue(NodeProperties.SLOT_ATTRIBUTE)) {
      updateAttribute(attributeMap.getProperty(NodeProperties.SLOT_ATTRIBUTE), element);
    }
  }
}

/**
 * Restores the element's captured initial hidden attribute and inline display.
 * Mirrors restoreInitialHiddenAttribute.
 */
function restoreInitialHiddenAttribute(element: Element, visibilityData: NodeMap): void {
  storeInitialHiddenAttribute(element, visibilityData);
  const initialVisibility = visibilityData.getProperty(NodeProperties.VISIBILITY_HIDDEN_PROPERTY);
  if (initialVisibility.hasValue()) {
    updateAttributeValue(
      visibilityData.getNode().getTree().getRegistry().getApplicationConfiguration(),
      element,
      HIDDEN_ATTRIBUTE,
      initialVisibility.getValue()
    );
  }

  const initialDisplay = visibilityData.getProperty(NodeProperties.VISIBILITY_STYLE_DISPLAY_PROPERTY);
  if (initialDisplay.hasValue()) {
    const initialValue = String(initialDisplay.getValue());
    (element as HTMLElement).style.display = initialValue;
  }
}

/**
 * Captures the element's initial `hidden` attribute and (in a shadow root) its
 * inline display into the visibility data, once. Mirrors
 * storeInitialHiddenAttribute.
 */
function storeInitialHiddenAttribute(element: Element, visibilityData: NodeMap): void {
  const initialVisibility = visibilityData.getProperty(NodeProperties.VISIBILITY_HIDDEN_PROPERTY);
  if (!initialVisibility.hasValue()) {
    initialVisibility.setValue(element.getAttribute(HIDDEN_ATTRIBUTE));
  }

  const initialDisplay = visibilityData.getProperty(NodeProperties.VISIBILITY_STYLE_DISPLAY_PROPERTY);
  // Java guards on element.getStyle() != null: a node that is not an HTML
  // element carries no style object.
  const style = (element as Partial<HTMLElement>).style;
  if (isInShadowRoot(element) && !initialDisplay.hasValue() && style !== undefined) {
    initialDisplay.setValue(style.display);
  }
}

/**
 * Re-binds a node by clearing and re-setting its DOM node (which re-fires the
 * dom-node-set event so initialization logic can run) and rebinding it. Mirrors
 * doBind.
 */
function doBind(node: StateNode, nodeFactory: BinderContext): void {
  const domNode = node.getDomNode();
  // Re-fires the dom-node-set event, giving a chance to run logic that needs to
  // know when the element is completely initialized.
  node.setDomNode(null);
  node.setDomNode(domNode);
  nodeFactory.createAndBind(node);
}

/**
 * Checks whether the `node` needs re-bind.
 *
 * The node needs re-bind if it was initially invisible. As a consequence such
 * node has not be bound. It has been bound in respect to visibility feature only
 * (partially bound). Such node needs re-bind once it becomes visible.
 *
 * @param node - the node to check
 * @returns `true` if the node is not entirely bound and needs re-bind later on
 */
export function needsRebind(node: StateNode): boolean {
  /*
   * Absence of value or "true" means that the node doesn't need re-bind.
   * So only "false" means "needs re-bind".
   */
  return readElementData(node, NodeProperties.VISIBILITY_BOUND_PROPERTY) === false;
}

/**
 * Binds a single map property: re-runs the user whenever the property's
 * dependencies change, tracking the computation by property name. Mirrors
 * bindProperty.
 */
function bindProperty(
  user: (property: MapProperty) => void,
  property: MapProperty,
  bindings: Map<string, Computation>
): Computation {
  const name = property.getName();

  assert(!bindings.has(name), `There's already a binding for ${name}`);

  const computation = Reactive.runWhenDependenciesChange(() => user(property));

  bindings.set(name, computation);

  return computation;
}

/**
 * Updates the element's JS property from the map property, or removes/clears it
 * when the property has no value. Mirrors updateProperty.
 */
function updateProperty(mapProperty: MapProperty, element: Element): void {
  const name = mapProperty.getName();
  const elementObject = element as unknown as Record<string, unknown>;
  if (mapProperty.hasValue()) {
    const treeValue = mapProperty.getValue();
    const domValue = getJsProperty(elementObject, name);
    const previousDomValue = mapProperty.getPreviousDomValue();

    // The user might have modified the DOM value during the server round-trip,
    // so only update to the tree value when it differs from the pre-round-trip
    // DOM value.
    const updateToTreeValue = previousDomValue === undefined ? true : !equalsInJS(treeValue, previousDomValue);

    // Compare with the current property to avoid setting properties already
    // updated on the client side (won't work for read-only properties).
    if (updateToTreeValue && (isUndefined(domValue) || !equalsInJS(domValue, treeValue))) {
      Reactive.runWithComputation(null, () => setJsProperty(elementObject, name, createModelTree(treeValue)));
    }
  } else if (hasOwnJsProperty(element, name)) {
    deleteJsProperty(elementObject, name);
  } else {
    // Can't delete an inherited property, so just clear the value.
    setJsProperty(elementObject, name, null);
  }
  mapProperty.clearPreviousDomValue();
}

/**
 * Updates a single inline style property of the element from a map property,
 * preserving an `!important` priority, or removes it when the property has no
 * value. Mirrors updateStyleProperty.
 */
function updateStyleProperty(mapProperty: MapProperty, element: HTMLElement): void {
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
 * Updates the named element attribute from a map property, resolving the
 * application configuration from the property's node. Mirrors updateAttribute.
 */
function updateAttribute(mapProperty: MapProperty, element: Element): void {
  updateAttributeValue(
    mapProperty.getMap().getNode().getTree().getRegistry().getApplicationConfiguration(),
    element,
    mapProperty.getName(),
    mapProperty.getValue()
  );
}

/**
 * Binds the node's children to the element, adopting existing elements where the
 * server requested attaching to one and appending the rest. Mirrors bindChildren.
 */
function bindChildren(context: BindingContext): EventRemover {
  const children = context.node.getList(NodeFeatures.ELEMENT_CHILDREN);
  if (children.hasBeenCleared()) {
    removeAllChildren(context.htmlNode);
  }

  for (let i = 0; i < children.length(); i++) {
    const childNode = children.get(i) as StateNode;

    const existingElementMap = childNode.getTree().getRegistry().getExistingElementMap();
    const child = existingElementMap.getElement(childNode.getId());
    if (child !== null) {
      existingElementMap.remove(childNode.getId());
      childNode.setDomNode(child);
      createAndBindChild(context, childNode);
    } else {
      context.htmlNode.appendChild(createAndBindChild(context, childNode));
    }
  }

  return children.addSpliceListener((e) => {
    // Handle lazily: the change giving a child its element tag may not be
    // applied yet.
    Reactive.addFlushListener(() => handleChildrenSplice(e, context));
  });
}

function createAndBindChild(context: BindingContext, childNode: StateNode): Node {
  return context.binderContext.createAndBind(childNode);
}

/**
 * Binds the node's virtual children, appending current ones and observing
 * additions. Mirrors bindVirtualChildren.
 */
function bindVirtualChildren(context: BindingContext): EventRemover {
  const children = context.node.getList(NodeFeatures.VIRTUAL_CHILDREN);

  for (let i = 0; i < children.length(); i++) {
    appendVirtualChild(context, children.get(i) as StateNode, true);
  }

  return children.addSpliceListener((e) => {
    // Handle lazily: the change giving a child its element tag may not be applied yet.
    Reactive.addFlushListener(() => {
      for (const added of e.getAdd()) {
        appendVirtualChild(context, added as StateNode, true);
      }
    });
  });
}

function appendVirtualChild(context: BindingContext, node: StateNode, reactivePhase: boolean): void {
  const object = getPayload(node);
  const type = object[NodeProperties.TYPE] as string;

  if (type === NodeProperties.IN_MEMORY_CHILD) {
    context.binderContext.createAndBind(node);
    return;
  }

  assert(context.htmlNode instanceof Element, 'Unexpected html node. The node is supposed to be a custom element');
  const element = context.htmlNode;
  if (type === NodeProperties.INJECT_BY_ID) {
    if (isLitElement(element)) {
      whenRendered(element, () => handleInjectId(context, node, object, false));
      return;
    } else if (!isReady(element)) {
      addReadyListener(element, () => handleInjectId(context, node, object, false));
      return;
    }
    handleInjectId(context, node, object, reactivePhase);
  } else if (type === NodeProperties.TEMPLATE_IN_TEMPLATE) {
    if (getDomRoot(element) === null) {
      addReadyListener(element, () => handleTemplateInTemplate(context, node, object, false));
      return;
    }
    handleTemplateInTemplate(context, node, object, reactivePhase);
  } else if (type === NodeProperties.INJECT_BY_NAME) {
    const name = object[NodeProperties.PAYLOAD] as string;
    const address = `name='${name}'`;
    const elementLookup = (): Element | null => getElementByName(element, name);

    if (!isInitialized(elementLookup)) {
      addReadyCallback(element, name, () => doAppendVirtualChild(context, node, false, elementLookup, name, address));
      return;
    }
    doAppendVirtualChild(context, node, reactivePhase, elementLookup, name, address);
  } else {
    // type is server-supplied, so this branch is reachable; mirror Java's assert false.
    assert(false, `Unexpected payload type ${type}`);
  }
}

// eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java doAppendVirtualChild signature
function doAppendVirtualChild(
  context: BindingContext,
  node: StateNode,
  reactivePhase: boolean,
  elementLookup: () => Element | null,
  id: string | null,
  address: string
): void {
  if (!verifyAttachRequest(context.node, node, id, address)) {
    return;
  }
  const element = elementLookup();
  if (verifyAttachedElement(element, node, id, address, context)) {
    if (!reactivePhase) {
      const initialPropertiesHandler = node.getTree().getRegistry().getInitialPropertiesHandler();
      initialPropertiesHandler.nodeRegistered(node);
      initialPropertiesHandler.flushPropertyUpdates();
    }
    node.setDomNode(element);
    context.binderContext.createAndBind(node);
  }
  if (!reactivePhase) {
    // Out of the reactive phase, flush() must be called explicitly for binding.
    Reactive.flush();
  }
}

function handleTemplateInTemplate(
  context: BindingContext,
  node: StateNode,
  object: Record<string, unknown>,
  reactivePhase: boolean
): void {
  const path = object[NodeProperties.PAYLOAD] as unknown[];
  const address = `path='${JSON.stringify(path)}'`;
  // getDomRoot is typed Element in Java, so the null case is dereferenced there
  // too rather than short-circuited here.
  const elementLookup = (): Element | null => getCustomElement(getDomRoot(context.htmlNode as Element)!, path);
  doAppendVirtualChild(context, node, reactivePhase, elementLookup, null, address);
}

function handleInjectId(
  context: BindingContext,
  node: StateNode,
  object: Record<string, unknown>,
  reactivePhase: boolean
): void {
  const id = object[NodeProperties.PAYLOAD] as string;
  const address = `id='${id}'`;
  const elementLookup = (): Element | null => getElementById(context.htmlNode, id);
  doAppendVirtualChild(context, node, reactivePhase, elementLookup, id, address);
}

// eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java verifyAttachedElement signature
function verifyAttachedElement(
  element: Element | null,
  attachNode: StateNode,
  id: string | null,
  address: string,
  context: BindingContext
): boolean {
  const node = context.node;
  const tag = getTag(attachNode);

  let failure = false;
  if (element === null) {
    failure = true;
    Console.warn(`${ELEMENT_ATTACH_ERROR_PREFIX}${address} is not found. The requested tag name is '${tag}'`);
  } else if (!hasTag(element, tag as string)) {
    failure = true;
    Console.warn(
      `${ELEMENT_ATTACH_ERROR_PREFIX}${address} has the wrong tag name '${element.tagName}', the requested tag name is '${tag}'`
    );
  }

  if (failure) {
    node.getTree().sendExistingElementWithIdAttachToServer(node, attachNode.getId(), -1, id);
    return false;
  }

  if (!node.hasFeature(NodeFeatures.SHADOW_ROOT_DATA)) {
    return true;
  }
  const map = node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
  const shadowRootNode = map.getProperty(NodeProperties.SHADOW_ROOT).getValue() as StateNode | null;
  if (shadowRootNode === null) {
    return true;
  }

  const list = shadowRootNode.getList(NodeFeatures.ELEMENT_CHILDREN);
  let existingId: number | null = null;
  for (let i = 0; i < list.length(); i++) {
    const stateNode = list.get(i) as StateNode;
    const domNode = stateNode.getDomNode();
    // Java calls domNode.equals(element) unguarded, so a shadow root child
    // without a DOM node throws there; mirror that deref rather than comparing
    // false, which would silently accept the input Java rejects.
    if (domNode!.isSameNode(element)) {
      existingId = stateNode.getId();
      break;
    }
  }

  if (existingId !== null) {
    Console.warn(
      `${ELEMENT_ATTACH_ERROR_PREFIX}${address} has been already attached previously via the node id='${existingId}'`
    );
    node.getTree().sendExistingElementWithIdAttachToServer(node, attachNode.getId(), existingId, id);
    return false;
  }
  return true;
}

function verifyAttachRequest(parent: StateNode, node: StateNode, id: string | null, address: string): boolean {
  // The server should not send several attach requests for the same client-side
  // element; this verifies that assumption.
  const virtualChildren = parent.getList(NodeFeatures.VIRTUAL_CHILDREN);
  for (let i = 0; i < virtualChildren.length(); i++) {
    const child = virtualChildren.get(i) as StateNode;
    if (child === node) {
      continue;
    }
    if (JSON.stringify(getPayload(node)) === JSON.stringify(getPayload(child))) {
      Console.warn(
        `There is already a request to attach element addressed by the ${address}. The existing request's node id='${child.getId()}'. Cannot attach the same element twice.`
      );
      node.getTree().sendExistingElementWithIdAttachToServer(parent, node.getId(), child.getId(), id);
      return false;
    }
  }
  return true;
}

function getPayload(node: StateNode): Record<string, unknown> {
  return node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.PAYLOAD).getValue() as Record<
    string,
    unknown
  >;
}

function handleChildrenSplice(event: ListSpliceEvent, context: BindingContext): void {
  const htmlNode = context.htmlNode;
  if (event.isClear()) {
    /*
     * A full clear removes all nodes, including ones the server doesn't know
     * about.
     *
     * The state tree is already fully updated at this point, so a non-empty
     * children list means the server is replacing the contents rather than
     * only emptying them. In that case the old nodes are kept until the
     * replacements have been inserted: a container that is momentarily empty
     * loses the scroll position of the surrounding scrollable element, since
     * the browser clamps the offset to the collapsed scroll range.
     *
     * The container then holds the old and the new contents at the same time,
     * so a layout while both are attached measures roughly twice the final
     * height. That is the trade for keeping the scroll range alive, and it is
     * the harmless direction: the offset is clamped when the range shrinks,
     * never when it grows.
     */
    if (context.node.getList(NodeFeatures.ELEMENT_CHILDREN).length() === 0) {
      removeAllChildren(htmlNode);
    } else {
      removeAllChildrenAfterReplacement(context);
    }
  } else {
    for (const removed of event.getRemove()) {
      const childNode = removed as StateNode;
      const child = childNode.getDomNode();
      assert(child !== null, "Can't find element to remove");
      // If the client-side element is not inside the parent the server expected
      // (client-only DOM changes), nothing is done here.
      if (child.parentNode === htmlNode) {
        htmlNode.removeChild(child);
      }
    }
  }

  const add = event.getAdd();
  if (add.length > 0) {
    addChildren(event.getIndex(), context, add);
  }
}

function removeAllChildren(htmlNode: Node): void {
  while (htmlNode.firstChild !== null) {
    htmlNode.removeChild(htmlNode.firstChild);
  }
}

/**
 * Removes the children the node has right now, but only once the whole change
 * set has been applied so that the nodes replacing them are already in place.
 *
 * @param context - the binding context of the node whose children are replaced
 */
function removeAllChildrenAfterReplacement(context: BindingContext): void {
  // childNodes is the live DOM child list, so the nodes to remove are collected
  // before anything is inserted
  const liveChildren = context.htmlNode.childNodes;
  const replacedChildren: Node[] = [];
  // eslint-disable-next-line @typescript-eslint/prefer-for-of -- ArrayLike DOM child list, not iterable
  for (let i = 0; i < liveChildren.length; i++) {
    replacedChildren.push(liveChildren[i]);
  }

  Reactive.addPostFlushListener(() => removeReplacedChildren(context, replacedChildren));
}

function removeReplacedChildren(context: BindingContext, replacedChildren: Node[]): void {
  const htmlNode = context.htmlNode;

  const keptChildren = getMappedDomNodes(context.node.getList(NodeFeatures.ELEMENT_CHILDREN));

  for (const child of replacedChildren) {
    /*
     * A node that the server re-added to this same parent is part of the new
     * contents, and a node that the server moved to another parent now belongs
     * to that parent. Neither may be removed here.
     */
    if (!keptChildren.has(child) && child.parentNode === htmlNode) {
      htmlNode.removeChild(child);
    }
  }
}

function addChildren(index: number, context: BindingContext, add: unknown[]): void {
  const nodeChildren = context.node.getList(NodeFeatures.ELEMENT_CHILDREN);

  let beforeRef: Node | null;
  if (index === 0) {
    // Insert at the first position after the client-side-only nodes.
    beforeRef = getFirstNodeMappedAsStateNode(nodeChildren, context.htmlNode);
  } else if (index <= nodeChildren.length() && index > 0) {
    const previousSibling = getPreviousSibling(index, context);
    beforeRef = previousSibling === null ? null : previousSibling.getDomNode()!.nextSibling;
  } else {
    // Insert at the end.
    beforeRef = null;
  }

  for (const newChildObject of add) {
    const newChild = newChildObject as StateNode;

    const existingElementMap = newChild.getTree().getRegistry().getExistingElementMap();
    let childNode: Node | null = existingElementMap.getElement(newChild.getId());
    if (childNode !== null) {
      existingElementMap.remove(newChild.getId());
      newChild.setDomNode(childNode);
      createAndBindChild(context, newChild);
    } else {
      childNode = createAndBindChild(context, newChild);
      context.htmlNode.insertBefore(childNode, beforeRef);
    }

    beforeRef = childNode.nextSibling;
  }
}

function getFirstNodeMappedAsStateNode(mappedNodeChildren: NodeList, htmlNode: Node): Node | null {
  const mappedDomNodes = getMappedDomNodes(mappedNodeChildren);

  const clientList = htmlNode.childNodes;
  // eslint-disable-next-line @typescript-eslint/prefer-for-of -- ArrayLike DOM child list, not iterable
  for (let i = 0; i < clientList.length; i++) {
    const clientNode = clientList[i];
    if (mappedDomNodes.has(clientNode)) {
      return clientNode;
    }
  }
  return null;
}

/**
 * Collects the DOM nodes of the given state nodes into a set, so that the nodes
 * currently in the DOM can be matched against them without scanning the state
 * node list for each of them.
 *
 * @param stateNodes - the state nodes to collect the DOM nodes of
 * @returns the DOM nodes of the state nodes that have one
 */
function getMappedDomNodes(stateNodes: NodeList): Set<Node> {
  const domNodes = new Set<Node>();
  for (let i = 0; i < stateNodes.length(); i++) {
    const domNode = (stateNodes.get(i) as StateNode).getDomNode();
    if (domNode !== null) {
      domNodes.add(domNode);
    }
  }
  return domNodes;
}

function getPreviousSibling(index: number, context: BindingContext): StateNode | null {
  const nodeChildren = context.node.getList(NodeFeatures.ELEMENT_CHILDREN);

  let count = 0;
  let node: StateNode | null = null;
  for (let i = 0; i < nodeChildren.length(); i++) {
    if (count === index) {
      return node;
    }
    const child = nodeChildren.get(i) as StateNode;
    if (child.getDomNode() !== null) {
      node = child;
      count++;
    }
  }
  return node;
}

/**
 * Removes all bindings.
 */
function remove(
  listeners: EventRemover[],
  context: BindingContext,
  computationsCollection: Array<Map<string, Computation>>
): void {
  computationsCollection.forEach((collection) => collection.forEach((computation) => computation.stop()));
  context.listenerBindings.forEach((computation) => computation.stop());

  context.listenerRemovers.forEach((remover) => remover.remove());
  listeners.forEach((remover) => remover.remove());

  // Java asserts boundNodes != null here; boundNodes is a module-level constant,
  // so the check is unreachable and dropped.
  boundNodes.delete(context.node);
}

/**
 * Binds the ELEMENT_LISTENERS feature to DOM event listeners, adding listeners
 * for the current handlers and tracking later additions. Mirrors
 * bindDomEventListeners.
 */
function bindDomEventListeners(context: BindingContext): EventRemover {
  const elementListeners = getDomEventListenerMap(context.node);
  elementListeners.forEachProperty((property) => {
    // Run eagerly to add initial listeners before the element is attached.
    bindEventHandlerProperty(property, context).recompute();
  });

  return elementListeners.addPropertyAddListener((event) => bindEventHandlerProperty(event.getProperty(), context));
}

function bindEventHandlerProperty(eventHandlerProperty: MapProperty, context: BindingContext): Computation {
  const name = eventHandlerProperty.getName();
  assert(!context.listenerBindings.has(name), `There is already an event-handler binding for ${name}`);

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
  assert(remover !== undefined, 'There must be a registered DOM event listener remover to remove');
  remover.remove();
}

function addEventHandler(eventType: string, context: BindingContext): void {
  assert(!context.listenerRemovers.has(eventType), `There is already a DOM event listener for ${eventType}`);
  const handler = (event: Event): void => handleDomEvent(event, context);
  context.htmlNode.addEventListener(eventType, handler, false);
  context.listenerRemovers.set(eventType, {
    remove: () => context.htmlNode.removeEventListener(eventType, handler, false)
  });
}

function getDomEventListenerMap(node: StateNode): NodeMap {
  return node.getMap(NodeFeatures.ELEMENT_LISTENERS);
}

/**
 * Handles a fired DOM event: collects the server-requested event data
 * (expressions, synchronized properties, mapped state nodes), resolves the
 * event filters/debounces, and sends the event to the server. Mirrors
 * handleDomEvent.
 */
function handleDomEvent(event: Event, context: BindingContext): void {
  // Java asserts context != null here; context is a required parameter, so the
  // check is unreachable and dropped.
  assert(context.htmlNode instanceof Element, 'Cannot handle DOM event for a Node');
  const element = context.htmlNode;
  const node = context.node;
  const type = event.type;

  const listenerMap = getDomEventListenerMap(node);
  const constantPool = node.getTree().getRegistry().getConstantPool();
  const expressionConstantKey = listenerMap.getProperty(type).getValue() as string;
  assert(expressionConstantKey !== null, 'There must be an expression constant key for the event type');
  assert(constantPool.has(expressionConstantKey), 'The constant pool must contain the expression constant key');

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

function getSyncPropertyCommand(propertyName: string, context: BindingContext): () => void {
  return context.node
    .getMap(NodeFeatures.ELEMENT_PROPERTIES)
    .getProperty(propertyName)
    .getSyncToServerCommand(getJsProperty(context.htmlNode as unknown as Record<string, unknown>, propertyName));
}

function sendEventToServer(
  node: StateNode,
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
 * Resolves the event filters for an event type. Returns true if there are no
 * filters or at least one filter matched (so the event should be sent). Mirrors
 * resolveFilters.
 */
// eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java resolveFilters signature
function resolveFilters(
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

/**
 * Resolves the debounce settings for one event filter. Each entry in
 * debounceList is `[timeout, phase1, phase2, ...]`; a zero timeout is eager.
 * Returns true if at least one debounce is eager (should be sent now). Mirrors
 * resolveDebounces.
 */
// eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java resolveDebounces signature
function resolveDebounces(
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
 * Binds the CLASS_LIST feature of the node to the element's class list,
 * applying the current classes and keeping them in sync as the list is spliced.
 * Mirrors bindClassList.
 */
function bindClassList(element: Element, node: StateNode): EventRemover {
  const classNodeList = node.getList(NodeFeatures.CLASS_LIST);

  for (let i = 0; i < classNodeList.length(); i++) {
    element.classList.add(classNodeList.get(i) as string);
  }

  return classNodeList.addSpliceListener((e) => {
    const classList = element.classList;

    e.getRemove().forEach((token) => classList.remove(token as string));
    e.getAdd().forEach((token) => classList.add(token as string));
  });
}

function bindPolymerEventHandlerNames(context: BindingContext): EventRemover {
  // Java casts context.htmlNode with WidgetUtil.crazyJsoCast, which has no port.
  return bindServerEventHandlerNames(
    () => context.htmlNode as unknown as Record<string, unknown>,
    context.node,
    NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS,
    false
  );
}

function bindClientCallableMethods(context: BindingContext): EventRemover {
  assert(context.htmlNode instanceof Element, 'Cannot bind client delegate methods to a Node');
  return bindServerEventHandlerNames(context.htmlNode, context.node);
}

/**
 * Sets an element attribute from a map-property value. A plain string (or null)
 * is applied as-is; a "uri" model object is resolved against the application
 * configuration (prefixing the service URL in web-component mode for relative
 * URIs); anything else is stringified. Mirrors updateAttributeValue.
 */
function updateAttributeValue(
  configuration: ApplicationConfiguration,
  element: Element,
  attribute: string,
  value: unknown
): void {
  if (value === null || value === undefined || typeof value === 'string') {
    setElementAttribute(element, attribute, (value ?? null) as string | null);
    // Java reinterprets the value as a JsonValue through WidgetUtil.crazyJsoCast
    // and switches on its JsonType; that cast is GWT-compiler-only and has no
    // port, so the JavaScript type is inspected directly.
  } else if (typeof value === 'object' && !Array.isArray(value)) {
    assert(
      NodeProperties.URI_ATTRIBUTE in value,
      // The "recieved" typo is carried over from the Java assert message.
      `Implementation error: JsonObject is recieved as an attribute value for '${attribute}' but it has no ${NodeProperties.URI_ATTRIBUTE} key`
    );
    const uri = (value as Record<string, unknown>)[NodeProperties.URI_ATTRIBUTE] as string;
    if (configuration.isWebComponentMode() && !isAbsoluteUrl(uri)) {
      let baseUri = configuration.getServiceUrl();
      baseUri = baseUri.endsWith('/') ? baseUri : `${baseUri}/`;
      setElementAttribute(element, attribute, baseUri + uri);
    } else {
      setElementAttribute(element, attribute, uri);
    }
  } else {
    // Java calls value.toString() on a JsonValue. An array reaches this branch
    // there too (its type is ARRAY, not OBJECT) and stringifies as its JSON
    // text, where a JavaScript array would join its elements with commas.
    setElementAttribute(element, attribute, Array.isArray(value) ? JSON.stringify(value) : String(value));
  }
}

/**
 * Parses an event-data expression into a function `(event, element) => value`,
 * caching the result per expression string; mirrors getOrCreateExpression.
 */
function getOrCreateExpression(expressionString: string): EventExpression {
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
 * Finds the id of the state node closest to the event target: a breadth-first
 * search of the state-node tree for a direct DOM match, then a bottom-up DOM
 * walk from the target's parent. Returns -1 if none is found. Mirrors
 * getClosestStateNodeIdToEventTarget.
 */
function getClosestStateNodeIdToEventTarget(topNode: StateNode, target: EventTarget | null): number {
  if (target === null) {
    return -1;
  }
  try {
    // Java casts the target with WidgetUtil.crazyJsCast, which has no port; the
    // target is treated as a Node directly.
    const stack: StateNode[] = [topNode];

    // collect children and test eagerly for direct match; the stack grows as
    // children are pushed during iteration (breadth-first)
    // eslint-disable-next-line @typescript-eslint/prefer-for-of -- index loop: stack is mutated during iteration
    for (let i = 0; i < stack.length; i++) {
      const stateNode = stack[i];
      if ((target as unknown as Node).isSameNode(stateNode.getDomNode())) {
        return stateNode.getId();
      }
      // NOTE: for now not looking at virtual children on purpose.
      stateNode.getList(NodeFeatures.ELEMENT_CHILDREN).forEach((child) => stack.push(child as StateNode));
    }
    // no direct match: bottom-up search from the target's parent
    return getStateNodeForElement(stack, (target as unknown as Node).parentNode);
  } catch (e) {
    // not going to let event handling fail; just report nothing found
    Console.debug(
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
function getStateNodeForElement(searchStack: StateNode[], targetNode: Node | null): number {
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
function getClosestStateNodeIdToDomNode(
  stateTree: StateTree,
  domNodeReference: unknown,
  eventDataExpression: string
): number {
  if (domNodeReference === null || domNodeReference === undefined) {
    return -1;
  }
  try {
    // As above, Java's WidgetUtil.crazyJsCast has no port.
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
    Console.debug(
      `An error occurred when Flow tried to find a state node matching the element ${String(
        domNodeReference
      )}, returned by an event data expression ${eventDataExpression}. Error: ${(e as Error).message}`
    );
  }
  return -1;
}

/**
 * Binding strategy for a simple (not template) {@link Element} node.
 */
export class SimpleElementBindingStrategy implements BindingStrategy<Element> {
  create(node: StateNode): Element {
    return create(node);
  }

  isApplicable(node: StateNode): boolean {
    return isApplicable(node);
  }

  getTag(node: StateNode): string | null {
    return getTag(node);
  }

  bind(stateNode: StateNode, htmlNode: Element, nodeFactory: BinderContext): void {
    const visible = isVisible(stateNode);

    assert(
      hasSameTag(stateNode, htmlNode),
      `Element tag name is '${htmlNode.tagName}', but the required tag name is ${getTag(stateNode)}`
    );

    if (boundNodes.has(stateNode)) {
      return;
    }
    boundNodes.set(stateNode, true);

    const node = stateNode;
    const context = new BindingContext(node, htmlNode, nodeFactory);

    const computationsCollection: Array<Map<string, Computation>> = [];
    const listeners: EventRemover[] = [];

    if (visible) {
      // Potential dependencies for any observer.
      listeners.push(bindClientCallableMethods(context));
      listeners.push(bindPolymerEventHandlerNames(context));

      // Flow's own event listeners.
      listeners.push(bindDomEventListeners(context));

      // DOM structure (shouldn't trigger observers synchronously).
      listeners.push(bindVirtualChildren(context));
      listeners.push(bindChildren(context));
      listeners.push(bindShadowRoot(context));

      // Styling.
      listeners.push(bindClassList(htmlNode, node));
      listeners.push(
        bindMap(
          NodeFeatures.ELEMENT_STYLE_PROPERTIES,
          (property: MapProperty) => updateStyleProperty(property, htmlNode as HTMLElement),
          createComputations(computationsCollection),
          node
        )
      );

      // The things that might actually be observed.
      listeners.push(
        bindMap(
          NodeFeatures.ELEMENT_ATTRIBUTES,
          (property: MapProperty) => updateAttribute(property, htmlNode),
          createComputations(computationsCollection),
          node
        )
      );
      listeners.push(
        bindMap(
          NodeFeatures.ELEMENT_PROPERTIES,
          (property: MapProperty) => updateProperty(property, htmlNode),
          createComputations(computationsCollection),
          node
        )
      );

      // Captured once and passed into the callbacks, for the reason spelled out
      // on handleListItemPropertyChange.
      const tree = stateNode.getTree();
      bindPolymerModelProperties(htmlNode, {
        handlePropertiesChanged: (changedProps: unknown) => handlePropertiesChanged(changedProps as object, stateNode),
        fireReadyEvent: (element: Element) => fireReadyEvent(element),
        handleListItemPropertyChange: (nodeId: number, host: Element, propertyName: string, value: unknown) =>
          handleListItemPropertyChange(nodeId, host, propertyName, value, tree)
      });

      // Prepare teardown.
      listeners.push(stateNode.addUnregisterListener(() => remove(listeners, context, computationsCollection)));
    } else {
      applyStructuralAttributes(stateNode, htmlNode);
    }
    listeners.push(bindVisibility(listeners, context, computationsCollection, nodeFactory));

    scheduleInitialExecution(stateNode);
  }
}
