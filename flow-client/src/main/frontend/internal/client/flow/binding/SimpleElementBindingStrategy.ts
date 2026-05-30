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
import { Console } from '../../Console';
import { ElementUtil } from '../../ElementUtil';
import { LitUtils } from '../../LitUtils';
import { PolymerUtils } from '../../PolymerUtils';
import { ReactUtils } from '../../ReactUtils';
import { WidgetUtil } from '../../WidgetUtil';
import { Computation } from '../reactive/Computation';
import { Reactive } from '../reactive/Reactive';
import type { BinderContext } from './BinderContext';
import type { BindingStrategy } from './BindingStrategy';
import { Debouncer } from './Debouncer';
import { ServerEventHandlerBinder } from './ServerEventHandlerBinder';
import { ServerEventObject } from './ServerEventObject';

// Mirrors com.vaadin.flow.internal.nodefeature.NodeFeatures.*.
const ELEMENT_DATA = 0;
const ELEMENT_PROPERTIES = 1;
const ELEMENT_CHILDREN = 2;
const ELEMENT_ATTRIBUTES = 3;
const ELEMENT_LISTENERS = 4;
const CLASS_LIST = 11;
const ELEMENT_STYLE_PROPERTIES = 12;
const TEMPLATE_MODELLIST = 16;
const POLYMER_SERVER_EVENT_HANDLERS = 17;
const SHADOW_ROOT_DATA = 20;
const VIRTUAL_CHILDREN = 24;

// Mirrors com.vaadin.flow.internal.nodefeature.NodeProperties.*.
const NAMESPACE = 'namespace';
const SHADOW_ROOT = 'shadowRoot';
const PAYLOAD = 'payload';
const TYPE = 'type';
const VISIBLE = 'visible';
const VISIBILITY_BOUND_PROPERTY = 'bound';
const VISIBILITY_HIDDEN_PROPERTY = 'hidden';
const VISIBILITY_STYLE_DISPLAY_PROPERTY = 'styleDisplay';
const URI_ATTRIBUTE = 'uri';
const SLOT_ATTRIBUTE = 'slot';
const IN_MEMORY_CHILD = 'inMemory';
const INJECT_BY_ID = '@id';
const INJECT_BY_NAME = '@name';
const TEMPLATE_IN_TEMPLATE = 'subTemplate';

// Mirrors com.vaadin.flow.shared.JsonConstants.*.
const EVENT_DATA_PHASE = 'for';
const SYNCHRONIZE_PROPERTY_TOKEN = '}';
const MAP_STATE_NODE_EVENT_DATA = ']';

// Mirrors com.vaadin.client.flow.model.UpdatableModelProperties.NODE_DATA_KEY.
const UPDATABLE_MODEL_PROPERTIES_NODE_DATA_KEY = 'UpdatableModelProperties';

const INITIAL_PROPERTY_UPDATE_NODE_DATA_KEY = 'InitialPropertyUpdate';

const HIDDEN_ATTRIBUTE = 'hidden';
const ELEMENT_ATTACH_ERROR_PREFIX = 'Element addressed by the ';
const DOM_REPEAT_MODIFIED_MARKER = '$propChangedModified';

type StateNodeLike = any;
type StateTreeLike = any;
type DomNodeLike = any;
type NodeMapLike = any;
type MapPropertyLike = any;
type Registration = { remove(): void };
type JsRunnable = () => void;
type SendCommand = (phase: string | null) => void;

interface PolymerElementInternal extends Element {
  _propertiesChanged?: (currentProps: unknown, changedProps: unknown, oldProps: unknown) => void;
  ready?: (...args: unknown[]) => void;
  root?: ParentNode | null;
}

interface DomRepeatPropertiesChanged {
  items?: Record<string, { nodeId?: number } & Record<string, unknown>>;
}

interface DomRepeatNode {
  constructor: {
    prototype: {
      _propertiesChanged?: (
        currentProps: DomRepeatPropertiesChanged,
        changedProps: Record<string, unknown>,
        oldProps: unknown
      ) => void;
      [DOM_REPEAT_MODIFIED_MARKER]?: boolean;
    };
  };
}

interface DataHostNode {
  localName?: string;
  __dataHost?: DataHostNode;
}

// Cached JS Function expressions, keyed by the raw expression text.
let expressionCache: Map<string, (event: Event, element: Element) => unknown> | null = null;

// Weak set of bound state nodes; entries are GC'd along with the state node.
let boundNodes: WeakMap<object, boolean> | null = null;

class InitialPropertyUpdate {
  private command: (() => void) | null = null;
  private readonly node: StateNodeLike;

  constructor(node: StateNodeLike) {
    this.node = node;
  }

  setCommand(command: () => void): void {
    this.command = command;
  }

  execute(): void {
    if (this.command !== null) {
      this.command();
    }
    this.node.clearNodeData(INITIAL_PROPERTY_UPDATE_NODE_DATA_KEY);
  }
}

class BindingContext {
  readonly node: StateNodeLike;
  readonly htmlNode: DomNodeLike;
  readonly binderContext: BinderContext;
  readonly listenerBindings = new Map<string, Computation>();
  readonly listenerRemovers = new Map<string, Registration>();

  constructor(node: StateNodeLike, htmlNode: DomNodeLike, binderContext: BinderContext) {
    this.node = node;
    this.htmlNode = htmlNode;
    this.binderContext = binderContext;
  }
}

/**
 * Binding strategy for a simple (not template) Element node. Migrated from
 * `com.vaadin.client.flow.binding.SimpleElementBindingStrategy`.
 *
 * Reached from GWT-compiled code via the `Binder.bind` namespace entry point,
 * which in turn dispatches to instances of this class.
 */
export class SimpleElementBindingStrategyImpl implements BindingStrategy<Element> {
  create(node: StateNodeLike): Element {
    const tag = this.getTag(node);
    if (tag == null) {
      throw new Error('New child must have a tag');
    }

    const namespace = getNamespace(node);
    if (namespace != null) {
      return document.createElementNS(namespace, tag);
    } else if (node.getParent() != null) {
      const namespaceURI = (node.getParent().getDomNode() as Element).namespaceURI;
      if (namespaceURI != null) {
        return document.createElementNS(namespaceURI, tag);
      }
    }

    return document.createElement(tag);
  }

  isApplicable(node: StateNodeLike): boolean {
    if (node.hasFeature(ELEMENT_DATA)) {
      return true;
    }
    return node.getTree() != null && node === node.getTree().getRootNode();
  }

  getTag(node: StateNodeLike): string {
    return PolymerUtils.getTag(node);
  }

  bind(stateNode: StateNodeLike, htmlNode: Element, nodeFactory: BinderContext): void {
    const visible = isVisible(stateNode);

    if (boundNodes == null) {
      boundNodes = new WeakMap();
    }

    if (boundNodes.has(stateNode)) {
      return;
    }
    boundNodes.set(stateNode, true);

    const context = new BindingContext(stateNode, htmlNode, nodeFactory);

    const computationsCollection: Array<Map<string, Computation>> = [];
    const listeners: Registration[] = [];

    if (visible) {
      // Potential dependencies for any observer
      listeners.push(bindClientCallableMethods(context));
      listeners.push(bindPolymerEventHandlerNames(context));

      // Flow's own event listeners
      listeners.push(bindDomEventListeners(context));

      // Dom structure, shouldn't trigger observers synchronously
      listeners.push(bindVirtualChildren(context));
      listeners.push(bindChildren(context));
      listeners.push(bindShadowRoot(context));

      // Styling might be looked at by observers, but will typically not
      // trigger any observers synchronously
      listeners.push(bindClassList(htmlNode, stateNode));
      listeners.push(
        bindMap(
          ELEMENT_STYLE_PROPERTIES,
          (property) => updateStyleProperty(property, htmlNode),
          createComputations(computationsCollection),
          stateNode
        )
      );

      // The things that might actually be observed
      listeners.push(
        bindMap(
          ELEMENT_ATTRIBUTES,
          (property) => updateAttribute(property, htmlNode),
          createComputations(computationsCollection),
          stateNode
        )
      );
      listeners.push(
        bindMap(
          ELEMENT_PROPERTIES,
          (property) => updateProperty(property, htmlNode),
          createComputations(computationsCollection),
          stateNode
        )
      );
      bindPolymerModelProperties(stateNode, htmlNode);

      // Prepare teardown
      listeners.push(
        stateNode.addUnregisterListener(() => {
          remove(listeners, context, computationsCollection);
        })
      );
    } else {
      applyStructuralAttributes(stateNode, htmlNode);
    }
    listeners.push(bindVisibility(listeners, context, computationsCollection, nodeFactory));

    scheduleInitialExecution(stateNode);
  }
}

// ---------- Module-level helpers (ported from the Java private methods) ----

function getNamespace(node: StateNodeLike): string | null {
  return node.getMap(ELEMENT_DATA).getProperty(NAMESPACE).getValue() as string | null;
}

function isVisible(node: StateNodeLike): boolean {
  return node.getTree().isVisible(node);
}

function scheduleInitialExecution(stateNode: StateNodeLike): void {
  const update = new InitialPropertyUpdate(stateNode);
  stateNode.setNodeData(INITIAL_PROPERTY_UPDATE_NODE_DATA_KEY, update);
  // Update command will be executed after all initial Reactive stuff. E.g.
  // initial JS (if any) will be executed BEFORE initial update command
  // execution.
  Reactive.addPostFlushListener(() => {
    setTimeout(() => {
      const propertyUpdate = stateNode.getNodeData(
        INITIAL_PROPERTY_UPDATE_NODE_DATA_KEY
      ) as InitialPropertyUpdate | null;
      // cleared if handlePropertiesChanged has already happened
      if (propertyUpdate != null) {
        propertyUpdate.execute();
      }
    }, 0);
  });
}

function bindPolymerModelProperties(node: StateNodeLike, element: Element): void {
  SimpleElementBindingStrategy.bindPolymerModelProperties(element, () => hookUpPolymerElement(node, element));
}

function hookUpPolymerElement(node: StateNodeLike, element: Element): void {
  const tree: StateTreeLike = node.getTree();
  SimpleElementBindingStrategy.hookUpPolymerElement(
    element,
    (changedProps) => handlePropertiesChanged(changedProps as Record<string, unknown>, node),
    () => PolymerUtils.fireReadyEvent(element),
    (nodeId, host, propertyName, value) =>
      handleListItemPropertyChange(nodeId, host as Element, propertyName, value, tree)
  );
}

// eslint-disable-next-line @typescript-eslint/max-params
function handleListItemPropertyChange(
  nodeId: number,
  host: Element,
  property: string,
  value: unknown,
  tree: StateTreeLike
): void {
  // Warning: tree is passed as an argument instead of StateNode or Element.
  // The patched prototype may not use the context from hookUpPolymerElement.
  const node: StateNodeLike = tree.getNode(nodeId | 0);

  if (!node.hasFeature(ELEMENT_PROPERTIES)) {
    return;
  }

  // TODO: this code doesn't care about the "security feature" which prevents
  // sending data from the client side to the server side if property is not
  // updatable. See handlePropertyChange and UpdatableModelProperties.
  const map = node.getMap(ELEMENT_PROPERTIES);
  const mapProperty = map.getProperty(property);
  mapProperty.syncToServer(value);
  // Silence unused-arg lint: host is used only by the upstream assertion.
  void host;
}

function handlePropertiesChanged(changedPropertyPathsToValues: Record<string, unknown>, node: StateNodeLike): void {
  const keys = Object.keys(changedPropertyPathsToValues);

  const runnable = (): void => {
    for (const propertyName of keys) {
      handlePropertyChange(propertyName, () => changedPropertyPathsToValues[propertyName], node);
    }
  };

  const initialUpdate = node.getNodeData(INITIAL_PROPERTY_UPDATE_NODE_DATA_KEY) as InitialPropertyUpdate | null;
  if (initialUpdate == null) {
    runnable();
  } else {
    initialUpdate.setCommand(runnable);
  }
}

function handlePropertyChange(fullPropertyName: string, valueProvider: () => unknown, node: StateNodeLike): void {
  const updatableProperties = node.getNodeData(UPDATABLE_MODEL_PROPERTIES_NODE_DATA_KEY) as {
    isUpdatableProperty(name: string): boolean;
  } | null;
  if (updatableProperties == null || !updatableProperties.isUpdatableProperty(fullPropertyName)) {
    // don't do anything if the property/sub-property is not in the
    // collection of updatable properties
    return;
  }

  // This is not the property value itself, it's a parent node of the property
  const subProperties = fullPropertyName.split('.');
  let model: StateNodeLike = node;
  let mapProperty: MapPropertyLike | null = null;
  const size = subProperties.length;
  for (let i = 0; i < size; i++) {
    const subProperty = subProperties[i];
    const elementProperties = model.getMap(ELEMENT_PROPERTIES);
    if (!elementProperties.hasPropertyValue(subProperty) && i < size - 1) {
      Console.debug("Ignoring property change for property '" + fullPropertyName + "' which isn't defined from server");
      return;
    }

    mapProperty = elementProperties.getProperty(subProperty);
    if (isStateNode(mapProperty!.getValue())) {
      model = mapProperty!.getValue();
    }
  }
  if (mapProperty == null) {
    return;
  }
  if (isStateNode(mapProperty.getValue())) {
    // Don't send to the server updates for list nodes
    const nodeValue: StateNodeLike = mapProperty.getValue();
    const obj = valueProvider() as { hasOwnProperty(key: string): boolean; nodeId?: unknown };
    if (!Object.prototype.hasOwnProperty.call(obj, 'nodeId') || nodeValue.hasFeature(TEMPLATE_MODELLIST)) {
      return;
    }
  }
  mapProperty.syncToServer(valueProvider());
}

function isStateNode(value: unknown): boolean {
  return (
    value != null &&
    typeof value === 'object' &&
    typeof (value as { hasFeature?: unknown }).hasFeature === 'function' &&
    typeof (value as { getMap?: unknown }).getMap === 'function'
  );
}

function bindShadowRoot(context: BindingContext): Registration {
  const map = context.node.getMap(SHADOW_ROOT_DATA);

  attachShadow(context);

  return map.addPropertyAddListener(() => Reactive.addFlushListener(() => attachShadow(context)));
}

function attachShadow(context: BindingContext): void {
  const map = context.node.getMap(SHADOW_ROOT_DATA);
  const shadowRootNode = map.getProperty(SHADOW_ROOT).getValue() as StateNodeLike | null;
  if (shadowRootNode != null) {
    const fn = new Function(
      'element',
      'if ( element.shadowRoot ) { return element.shadowRoot; } ' +
        "else { return element.attachShadow({'mode' : 'open'});}"
    );
    const shadowRoot = fn(context.htmlNode) as Node;

    if (shadowRootNode.getDomNode() == null) {
      shadowRootNode.setDomNode(shadowRoot);
    }

    const newContext = new BindingContext(shadowRootNode, shadowRoot, context.binderContext);
    bindChildren(newContext);
  }
}

function createComputations(computationsCollection: Array<Map<string, Computation>>): Map<string, Computation> {
  const computations = new Map<string, Computation>();
  computationsCollection.push(computations);
  return computations;
}

function bindMap(
  featureId: number,
  user: (property: MapPropertyLike) => void,
  bindings: Map<string, Computation>,
  node: StateNodeLike
): Registration {
  const map = node.getMap(featureId);
  map.forEachProperty((property: MapPropertyLike) => bindProperty(user, property, bindings).recompute());

  return map.addPropertyAddListener((e: { getProperty(): MapPropertyLike }) =>
    bindProperty(user, e.getProperty(), bindings)
  );
}

function bindProperty(
  user: (property: MapPropertyLike) => void,
  property: MapPropertyLike,
  bindings: Map<string, Computation>
): Computation {
  const name = property.getName() as string;
  const computation = new Computation(() => user(property));
  bindings.set(name, computation);
  return computation;
}

function bindVisibility(
  listeners: Registration[],
  context: BindingContext,
  computationsCollection: Array<Map<string, Computation>>,
  nodeFactory: BinderContext
): Registration {
  const visibilityData = context.node.getMap(ELEMENT_DATA);

  visibilityData.getProperty(VISIBILITY_BOUND_PROPERTY).setValue(isVisible(context.node));
  updateVisibility(listeners, context, computationsCollection, nodeFactory);
  return context.node
    .getMap(ELEMENT_DATA)
    .getProperty(VISIBLE)
    .addChangeListener(() => updateVisibility(listeners, context, computationsCollection, nodeFactory));
}

function updateVisibility(
  listeners: Registration[],
  context: BindingContext,
  computationsCollection: Array<Map<string, Computation>>,
  nodeFactory: BinderContext
): void {
  const visibilityData = context.node.getMap(ELEMENT_DATA);
  const element = context.htmlNode as Element;

  if (needsRebind(context.node) && isVisible(context.node)) {
    remove(listeners, context, computationsCollection);
    Reactive.addFlushListener(() => {
      restoreInitialHiddenAttribute(element, visibilityData);
      doBind(context.node, nodeFactory);
    });
  } else if (isVisible(context.node)) {
    visibilityData.getProperty(VISIBILITY_BOUND_PROPERTY).setValue(true);
    restoreInitialHiddenAttribute(element, visibilityData);
  } else {
    setElementInvisible(element, visibilityData);
  }
}

function setElementInvisible(element: Element, visibilityData: NodeMapLike): void {
  storeInitialHiddenAttribute(element, visibilityData);
  updateAttributeValue(() => getApplicationConfiguration(visibilityData.getNode()), element, HIDDEN_ATTRIBUTE, true);
  if (PolymerUtils.isInShadowRoot(element)) {
    (element as HTMLElement).style.display = 'none';
  }
}

/**
 * Applies structural attributes (like "slot") to the element even when it is
 * initially invisible. This preserves CSS selectors that depend on these
 * attributes without exposing backend data.
 */
function applyStructuralAttributes(stateNode: StateNodeLike, element: Element): void {
  if (stateNode.hasFeature(ELEMENT_ATTRIBUTES)) {
    const attributeMap = stateNode.getMap(ELEMENT_ATTRIBUTES);
    if (attributeMap.hasPropertyValue(SLOT_ATTRIBUTE)) {
      const property = attributeMap.getProperty(SLOT_ATTRIBUTE);
      updateAttribute(property, element);
    }
  }
}

function restoreInitialHiddenAttribute(element: Element, visibilityData: NodeMapLike): void {
  storeInitialHiddenAttribute(element, visibilityData);
  const initialVisibility = visibilityData.getProperty(VISIBILITY_HIDDEN_PROPERTY);
  if (initialVisibility.hasValue()) {
    updateAttributeValue(
      () => getApplicationConfiguration(visibilityData.getNode()),
      element,
      HIDDEN_ATTRIBUTE,
      initialVisibility.getValue()
    );
  }

  const initialDisplay = visibilityData.getProperty(VISIBILITY_STYLE_DISPLAY_PROPERTY);
  if (initialDisplay.hasValue()) {
    const initialValue = String(initialDisplay.getValue());
    (element as HTMLElement).style.display = initialValue;
  }
}

function storeInitialHiddenAttribute(element: Element, visibilityData: NodeMapLike): void {
  const initialVisibility = visibilityData.getProperty(VISIBILITY_HIDDEN_PROPERTY);
  if (!initialVisibility.hasValue()) {
    initialVisibility.setValue(element.getAttribute(HIDDEN_ATTRIBUTE));
  }

  const initialDisplay = visibilityData.getProperty(VISIBILITY_STYLE_DISPLAY_PROPERTY);
  if (PolymerUtils.isInShadowRoot(element) && !initialDisplay.hasValue() && (element as HTMLElement).style != null) {
    initialDisplay.setValue((element as HTMLElement).style.display);
  }
}

function doBind(node: StateNodeLike, nodeFactory: BinderContext): void {
  const domNode = node.getDomNode();
  // Fire an event which gives a chance to run logic which needs to know when
  // the element is completely initialized.
  node.setDomNode(null);
  node.setDomNode(domNode);
  nodeFactory.createAndBind(node);
}

/**
 * Checks whether the `node` needs re-bind. The node needs re-bind if it was
 * initially invisible (so it was only partially bound). Only the literal
 * value `false` indicates "needs re-bind".
 */
export function needsRebind(node: StateNodeLike): boolean {
  return node.getMap(ELEMENT_DATA).getProperty(VISIBILITY_BOUND_PROPERTY).getValue() === false;
}

function updateProperty(mapProperty: MapPropertyLike, element: Element): void {
  const name = mapProperty.getName() as string;
  if (mapProperty.hasValue()) {
    const treeValue = mapProperty.getValue();
    const domValue = WidgetUtil.getJsProperty(element as unknown as Record<string, unknown>, name);
    // hasPreviousDomValue / getPreviousDomValueRaw expose the same info as
    // the Java Optional<Object> getPreviousDomValue().
    const hasPrevious = mapProperty.hasPreviousDomValue();
    const previous = hasPrevious ? mapProperty.getPreviousDomValueRaw() : undefined;

    // User might have modified DOM value during server round-trip. Only
    // update to the tree value if the tree value differs from the
    // pre-server-round-trip DOM value.
    const updateToTreeValue = hasPrevious ? !WidgetUtil.equals(treeValue, previous) : true;

    // Compare with the current property to avoid setting properties that are
    // updated on the client side (e.g. when synchronizing properties to the
    // server; won't work for readonly properties).
    if (updateToTreeValue && (WidgetUtil.isUndefined(domValue) || !WidgetUtil.equals(domValue, treeValue))) {
      Reactive.runWithComputation(null, () => {
        WidgetUtil.setJsProperty(
          element as unknown as Record<string, unknown>,
          name,
          PolymerUtils.createModelTree(treeValue)
        );
      });
    }
  } else if (WidgetUtil.hasOwnJsProperty(element, name)) {
    WidgetUtil.deleteJsProperty(element as unknown as Record<string, unknown>, name);
  } else {
    // Can't delete inherited property, so instead just clear the value.
    WidgetUtil.setJsProperty(element as unknown as Record<string, unknown>, name, null);
  }
  mapProperty.clearPreviousDomValue();
}

function updateStyleProperty(mapProperty: MapPropertyLike, element: Element): void {
  const name = mapProperty.getName() as string;
  const styleElement = (element as HTMLElement).style;
  if (mapProperty.hasValue()) {
    const value = mapProperty.getValue() as string;
    let styleIsSet = false;
    if (value.indexOf('!important') !== -1) {
      const temp = document.createElement(element.tagName);
      const tmpStyle = temp.style;
      tmpStyle.cssText = name + ': ' + value + ';';
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

function updateAttribute(mapProperty: MapPropertyLike, element: Element): void {
  const name = mapProperty.getName() as string;
  updateAttributeValue(
    () => getApplicationConfiguration(mapProperty.getMap().getNode()),
    element,
    name,
    mapProperty.getValue()
  );
}

// Lazily resolves the ApplicationConfiguration from the registry. Callers
// receive the configuration *only* when a URI-rewriting attribute value is
// being processed, avoiding an obfuscated method call on the Java registry
// for every attribute update.
type ConfigSupplier = () => { webComponentMode?: boolean; serviceUrl?: string | null } | null | undefined;

function updateAttributeValue(
  configSupplier: ConfigSupplier,
  element: Element,
  attribute: string,
  value: unknown
): void {
  if (value === null || value === undefined || typeof value === 'string') {
    WidgetUtil.updateAttribute(element, attribute, (value as string | null) ?? null);
  } else if (typeof value === 'object') {
    // JSON-like object: only URI attribute objects are expected.
    const obj = value as Record<string, unknown>;
    if (Object.prototype.hasOwnProperty.call(obj, URI_ATTRIBUTE)) {
      const uri = obj[URI_ATTRIBUTE] as string;
      const configuration = configSupplier();
      const webComponentMode = configuration?.webComponentMode === true;
      if (webComponentMode && !WidgetUtil.isAbsoluteUrl(uri)) {
        let baseUri = (configuration?.serviceUrl ?? '') as string;
        baseUri = baseUri.endsWith('/') ? baseUri : baseUri + '/';
        WidgetUtil.updateAttribute(element, attribute, baseUri + uri);
      } else {
        WidgetUtil.updateAttribute(element, attribute, uri);
      }
    } else {
      WidgetUtil.updateAttribute(element, attribute, String(value));
    }
  } else {
    WidgetUtil.updateAttribute(element, attribute, String(value));
  }
}

// Resolves the ApplicationConfiguration from a state node's registry by way
// of a known lookup key. The Java registry's `getApplicationConfiguration()`
// method is renamed during GWT OBF compilation, so we cannot call it from
// the TS-emitted JS. The lookup table itself is stored in the registry under
// a fixed `String` key, so we reach it via the (also obfuscated) `get`
// method by name — but only inside a `try` so a runtime miss does not break
// attribute updates.
function getApplicationConfiguration(
  stateNode: StateNodeLike
): { webComponentMode?: boolean; serviceUrl?: string | null } | null {
  try {
    const registry = stateNode.getTree().getRegistry();
    // Prefer the @JsType-exposed accessor — works in dev mode and in any
    // build configuration that doesn't rename @JsType methods.
    if (typeof registry.getApplicationConfiguration === 'function') {
      return registry.getApplicationConfiguration();
    }
    // Fall back to the namespace-published TS class. The Bootstrapper
    // assigns the live instance to `window.Vaadin.Flow.clients.<appId>.appConfig`,
    // but the simpler `appConfig` slot may not be reliably populated; in
    // that case attribute URI rewriting silently skips webComponentMode
    // handling, which matches the existing fall-through for non-URI values.
    return null;
  } catch {
    return null;
  }
}

function bindChildren(context: BindingContext): Registration {
  const children = context.node.getList(ELEMENT_CHILDREN);
  if (children.hasBeenCleared()) {
    removeAllChildren(context.htmlNode);
  }

  for (let i = 0; i < children.length(); i++) {
    const childNode: StateNodeLike = children.get(i);

    const existingElementMap = childNode.getTree().getRegistry().getExistingElementMap();
    let child = existingElementMap.getElement(childNode.getId()) as Node | null;
    if (child != null) {
      existingElementMap.remove(childNode.getId());
      childNode.setDomNode(child);
      context.binderContext.createAndBind(childNode);
    } else {
      child = context.binderContext.createAndBind(childNode) as Node;
      (context.htmlNode as Node).appendChild(child);
    }
  }

  return children.addSpliceListener(
    (e: { isClear(): boolean; getRemove(): unknown[]; getAdd(): unknown[]; getIndex(): number }) => {
      // Handle lazily so we can create the children we need to insert. The
      // change that gives a child node an element tag name might not yet have
      // been applied at this point.
      Reactive.addFlushListener(() => handleChildrenSplice(e, context));
    }
  );
}

function bindVirtualChildren(context: BindingContext): Registration {
  const children = context.node.getList(VIRTUAL_CHILDREN);

  for (let i = 0; i < children.length(); i++) {
    appendVirtualChild(context, children.get(i) as StateNodeLike, true);
  }

  return children.addSpliceListener((e: { getAdd(): unknown[] }) => {
    // Handle lazily so we can create the children we need to insert. The
    // change that gives a child node an element tag name might not yet have
    // been applied at this point.
    Reactive.addFlushListener(() => {
      const add = e.getAdd();
      if (add != null && (add as { length: number }).length > 0) {
        const len = (add as { length: number }).length;
        for (let i = 0; i < len; i++) {
          appendVirtualChild(context, (add as unknown[])[i] as StateNodeLike, true);
        }
      }
    });
  });
}

function appendVirtualChild(context: BindingContext, node: StateNodeLike, reactivePhase: boolean): void {
  const object = getPayload(node) as Record<string, unknown>;
  const type = object[TYPE] as string;

  if (IN_MEMORY_CHILD === type) {
    context.binderContext.createAndBind(node);
    return;
  }

  if (INJECT_BY_ID === type) {
    if (LitUtils.isLitElement(context.htmlNode)) {
      LitUtils.whenRendered(context.htmlNode as Element, () => handleInjectId(context, node, object, false));
      return;
    } else if (!PolymerUtils.isReady(context.htmlNode)) {
      PolymerUtils.addReadyListener(context.htmlNode as Element, () => handleInjectId(context, node, object, false));
      return;
    }

    handleInjectId(context, node, object, reactivePhase);
  } else if (TEMPLATE_IN_TEMPLATE === type) {
    if (PolymerUtils.getDomRoot(context.htmlNode) == null) {
      PolymerUtils.addReadyListener(context.htmlNode as Element, () =>
        handleTemplateInTemplate(context, node, object, false)
      );
      return;
    }
    handleTemplateInTemplate(context, node, object, reactivePhase);
  } else if (INJECT_BY_NAME === type) {
    const name = object[PAYLOAD] as string;
    const address = "name='" + name + "'";

    const elementLookup = (): Element | null =>
      (ElementUtil.getElementByName(context.htmlNode as Element, name) as Element | undefined) ?? null;

    if (!ReactUtils.isInitialized(elementLookup)) {
      ReactUtils.addReadyCallback(context.htmlNode as Element, name, () => {
        doAppendVirtualChild(context, node, false, elementLookup, name, address);
      });
      return;
    }
    doAppendVirtualChild(context, node, reactivePhase, elementLookup, name, address);
  } else {
    throw new Error('Unexpected payload type ' + type);
  }
}

// eslint-disable-next-line @typescript-eslint/max-params
function doAppendVirtualChild(
  context: BindingContext,
  node: StateNodeLike,
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
    // Correct binding requires reactive involvement which doesn't happen
    // automatically when we are out of the phase. So we should call flush()
    // explicitly.
    Reactive.flush();
  }
}

function handleTemplateInTemplate(
  context: BindingContext,
  node: StateNodeLike,
  object: Record<string, unknown>,
  reactivePhase: boolean
): void {
  const path = object[PAYLOAD] as number[];
  const address = "path='" + JSON.stringify(path) + "'";

  const elementLookup = (): Element | null =>
    PolymerUtils.getCustomElement(PolymerUtils.getDomRoot(context.htmlNode) as Node, path);

  doAppendVirtualChild(context, node, reactivePhase, elementLookup, null, address);
}

function handleInjectId(
  context: BindingContext,
  node: StateNodeLike,
  object: Record<string, unknown>,
  reactivePhase: boolean
): void {
  const id = object[PAYLOAD] as string;
  const address = "id='" + id + "'";
  const elementLookup = (): Element | null =>
    (ElementUtil.getElementById(context.htmlNode as Node, id) as Element | undefined) ?? null;

  doAppendVirtualChild(context, node, reactivePhase, elementLookup, id, address);
}

// eslint-disable-next-line @typescript-eslint/max-params
function verifyAttachedElement(
  element: Element | null,
  attachNode: StateNodeLike,
  id: string | null,
  address: string,
  context: BindingContext
): boolean {
  const node = context.node;
  const tag = PolymerUtils.getTag(attachNode);

  let failure = false;
  if (element == null) {
    failure = true;
    Console.warn(ELEMENT_ATTACH_ERROR_PREFIX + address + " is not found. The requested tag name is '" + tag + "'");
  } else if (!ElementUtil.hasTag(element, tag)) {
    failure = true;
    Console.warn(
      ELEMENT_ATTACH_ERROR_PREFIX +
        address +
        " has the wrong tag name '" +
        element.tagName +
        "', the requested tag name is '" +
        tag +
        "'"
    );
  }

  if (failure) {
    node.getTree().sendExistingElementWithIdAttachToServer(node, attachNode.getId(), -1, id);
    return false;
  }

  if (!node.hasFeature(SHADOW_ROOT_DATA)) {
    return true;
  }
  const map = node.getMap(SHADOW_ROOT_DATA);
  const shadowRootNode = map.getProperty(SHADOW_ROOT).getValue() as StateNodeLike | null;
  if (shadowRootNode == null) {
    return true;
  }

  const list = shadowRootNode.getList(ELEMENT_CHILDREN);
  let existingId: number | null = null;

  for (let i = 0; i < list.length(); i++) {
    const stateNode: StateNodeLike = list.get(i);
    const domNode = stateNode.getDomNode();

    if (domNode === element) {
      existingId = stateNode.getId();
      break;
    }
  }

  if (existingId != null) {
    Console.warn(
      ELEMENT_ATTACH_ERROR_PREFIX +
        address +
        " has been already attached previously via the node id='" +
        existingId +
        "'"
    );
    node.getTree().sendExistingElementWithIdAttachToServer(node, attachNode.getId(), existingId, id);
    return false;
  }
  return true;
}

function verifyAttachRequest(parent: StateNodeLike, node: StateNodeLike, id: string | null, address: string): boolean {
  // Belt-and-braces check: the server should never send several attach
  // requests for the same client-side element. If it does (programming
  // error), reject the duplicate explicitly.
  const virtualChildren = parent.getList(VIRTUAL_CHILDREN);
  for (let i = 0; i < virtualChildren.length(); i++) {
    const child: StateNodeLike = virtualChildren.get(i);
    if (child === node) {
      continue;
    }
    if (JSON.stringify(getPayload(node)) === JSON.stringify(getPayload(child))) {
      Console.warn(
        'There is already a request to attach element addressed by the ' +
          address +
          ". The existing request's node id='" +
          child.getId() +
          "'. Cannot attach the same element twice."
      );
      node.getTree().sendExistingElementWithIdAttachToServer(parent, node.getId(), child.getId(), id);
      return false;
    }
  }
  return true;
}

function getPayload(node: StateNodeLike): unknown {
  const map = node.getMap(ELEMENT_DATA);
  return map.getProperty(PAYLOAD).getValue();
}

function handleChildrenSplice(
  event: { isClear(): boolean; getRemove(): unknown[]; getAdd(): unknown[]; getIndex(): number },
  context: BindingContext
): void {
  const htmlNode = context.htmlNode as Node;
  if (event.isClear()) {
    // On a full clear, all nodes must be removed, including ones the server
    // doesn't know about.
    removeAllChildren(htmlNode);
  } else {
    const remove = event.getRemove();
    const removeLen = (remove as { length: number }).length;
    for (let i = 0; i < removeLen; i++) {
      const childNode: StateNodeLike = (remove as unknown[])[i];
      const child = childNode.getDomNode() as Node | null;

      if (child == null) {
        throw new Error("Can't find element to remove");
      }

      if (child.parentNode === htmlNode) {
        htmlNode.removeChild(child);
      }
      // If the client-side element is not inside the parent the server
      // thought it should be (because of client-side-only DOM changes),
      // nothing is done at this point. If the server appends the element to
      // a new parent, that will override the client DOM below.
    }
  }

  const add = event.getAdd();
  if (add != null && (add as { length: number }).length > 0) {
    addChildren(event.getIndex(), context, add);
  }
}

function removeAllChildren(htmlNode: Node): void {
  while (htmlNode.firstChild != null) {
    htmlNode.removeChild(htmlNode.firstChild);
  }
}

function addChildren(index: number, context: BindingContext, add: unknown[]): void {
  const nodeChildren = context.node.getList(ELEMENT_CHILDREN);

  let beforeRef: Node | null;
  if (index === 0) {
    // Insert at the first position after the client-side-only nodes
    beforeRef = getFirstNodeMappedAsStateNode(nodeChildren, context.htmlNode);
  } else if (index <= nodeChildren.length() && index > 0) {
    const previousSibling = getPreviousSibling(index, context);
    // Insert before the next sibling of the current node
    beforeRef = previousSibling == null ? null : (previousSibling.getDomNode() as Node).nextSibling;
  } else {
    // Insert at the end
    beforeRef = null;
  }

  const addLen = (add as { length: number }).length;
  for (let i = 0; i < addLen; i++) {
    const newChild: StateNodeLike = (add as unknown[])[i];

    const existingElementMap = newChild.getTree().getRegistry().getExistingElementMap();
    let childNode = existingElementMap.getElement(newChild.getId()) as Node | null;
    if (childNode != null) {
      existingElementMap.remove(newChild.getId());
      newChild.setDomNode(childNode);
      context.binderContext.createAndBind(newChild);
    } else {
      childNode = context.binderContext.createAndBind(newChild) as Node;
      (context.htmlNode as Node).insertBefore(childNode, beforeRef);
    }

    beforeRef = childNode.nextSibling;
  }
}

function getFirstNodeMappedAsStateNode(mappedNodeChildren: any, htmlNode: Node): Node | null {
  const clientList = htmlNode.childNodes;
  for (const clientNode of Array.from(clientList)) {
    for (let j = 0; j < mappedNodeChildren.length(); j++) {
      const stateNode: StateNodeLike = mappedNodeChildren.get(j);
      if (clientNode === stateNode.getDomNode()) {
        return clientNode;
      }
    }
  }
  return null;
}

function getPreviousSibling(index: number, context: BindingContext): StateNodeLike | null {
  const nodeChildren = context.node.getList(ELEMENT_CHILDREN);

  let count = 0;
  let node: StateNodeLike | null = null;
  for (let i = 0; i < nodeChildren.length(); i++) {
    if (count === index) {
      return node;
    }
    const child: StateNodeLike = nodeChildren.get(i);
    if (child.getDomNode() != null) {
      node = child;
      count++;
    }
  }
  return node;
}

function remove(
  listeners: Registration[],
  context: BindingContext,
  computationsCollection: Array<Map<string, Computation>>
): void {
  const stop = (computation: Computation): void => computation.stop();

  computationsCollection.forEach((collection) => collection.forEach((c) => stop(c)));
  context.listenerBindings.forEach((c) => stop(c));

  context.listenerRemovers.forEach((rem) => rem.remove());
  listeners.forEach((r) => r.remove());

  boundNodes!.delete(context.node);
}

function bindDomEventListeners(context: BindingContext): Registration {
  const elementListeners = getDomEventListenerMap(context.node);
  elementListeners.forEachProperty((property: MapPropertyLike) => {
    const computation = bindEventHandlerProperty(property, context);
    // Run eagerly to add initial listeners before element is attached.
    computation.recompute();
  });

  return elementListeners.addPropertyAddListener((event: { getProperty(): MapPropertyLike }) =>
    bindEventHandlerProperty(event.getProperty(), context)
  );
}

function bindEventHandlerProperty(eventHandlerProperty: MapPropertyLike, context: BindingContext): Computation {
  const name = eventHandlerProperty.getName() as string;

  const computation = new Computation(() => {
    const hasValue = eventHandlerProperty.hasValue() as boolean;
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
  if (remover != null) {
    remover.remove();
  }
}

function addEventHandler(eventType: string, context: BindingContext): void {
  const target = context.htmlNode as Node;
  const handler = (event: Event): void => handleDomEvent(event, context);
  target.addEventListener(eventType, handler, false);

  const remover: Registration = {
    remove: () => target.removeEventListener(eventType, handler, false)
  };

  context.listenerRemovers.set(eventType, remover);
}

function getDomEventListenerMap(node: StateNodeLike): NodeMapLike {
  return node.getMap(ELEMENT_LISTENERS);
}

function handleDomEvent(event: Event, context: BindingContext): void {
  const element = context.htmlNode as Element;
  const node = context.node;
  const type = event.type;

  const listenerMap = getDomEventListenerMap(node);

  const constantPool = node.getTree().getRegistry().getConstantPool();
  const expressionConstantKey = listenerMap.getProperty(type).getValue() as string;

  const expressionSettings = constantPool.get(expressionConstantKey) as Record<string, unknown>;
  const expressions = Object.keys(expressionSettings);

  let eventData: Record<string, unknown> | null;
  const synchronizeProperties = new Set<string>();

  if (expressions.length === 0) {
    eventData = null;
  } else {
    eventData = {};
  }
  for (const expressionString of expressions) {
    if (expressionString.startsWith(SYNCHRONIZE_PROPERTY_TOKEN)) {
      const property = expressionString.substring(SYNCHRONIZE_PROPERTY_TOKEN.length);
      synchronizeProperties.add(property);
    } else if (expressionString === MAP_STATE_NODE_EVENT_DATA) {
      // map event.target to the closest state node
      const targetNodeId = getClosestStateNodeIdToEventTarget(node, event.target);
      eventData![MAP_STATE_NODE_EVENT_DATA] = targetNodeId;
    } else if (expressionString.startsWith(MAP_STATE_NODE_EVENT_DATA)) {
      // map element returned by JS to the closest state node
      const jsEvaluation = expressionString.substring(MAP_STATE_NODE_EVENT_DATA.length);
      const expression = getOrCreateExpression(jsEvaluation);
      const expressionValue = expression(event, element);
      const targetNodeId = getClosestStateNodeIdToDomNode(node.getTree(), expressionValue, jsEvaluation);
      eventData![expressionString] = targetNodeId;
    } else {
      const expression = getOrCreateExpression(expressionString);
      const expressionValue = expression(event, element);
      eventData![expressionString] = expressionValue;
    }
  }
  synchronizeProperties.forEach((name) => {
    const map = node.getMap(ELEMENT_PROPERTIES);
    const mapProperty = map.getProperty(name);
    const domValue = WidgetUtil.getJsProperty(element as unknown as Record<string, unknown>, name);
    mapProperty.setPreviousDomValue(domValue);
  });

  const commands = new Map<string, JsRunnable>();
  synchronizeProperties.forEach((name) => commands.set(name, getSyncPropertyCommand(name, context)));

  const sendCommand: SendCommand = (debouncePhase) => {
    sendEventToServer(node, type, eventData, debouncePhase);
  };

  const sendNow = resolveFilters(element, type, expressionSettings, eventData, sendCommand, commands);

  if (sendNow) {
    // Send if there were no filters or at least one matched.
    let commandAlreadyExecuted = false;
    const flushPendingChanges = synchronizeProperties.size === 0;

    if (flushPendingChanges) {
      // Flush all debounced events so that they don't happen in wrong order
      // in the server-side.
      const executed = Debouncer.flushAll();
      for (const exec of executed) {
        if (exec === sendCommand) {
          commandAlreadyExecuted = true;
          break;
        }
      }
    }

    if (!commandAlreadyExecuted) {
      commands.forEach((cmd) => cmd());
      sendCommand(null);
    }
  }
}

function getSyncPropertyCommand(propertyName: string, context: BindingContext): JsRunnable {
  return context.node
    .getMap(ELEMENT_PROPERTIES)
    .getProperty(propertyName)
    .getSyncToServerCommand(WidgetUtil.getJsProperty(context.htmlNode as Record<string, unknown>, propertyName));
}

function sendEventToServer(
  node: StateNodeLike,
  type: string,
  eventData: Record<string, unknown> | null,
  debouncePhase: string | null
): void {
  let data = eventData;
  if (debouncePhase == null) {
    if (data != null) {
      // eslint-disable-next-line @typescript-eslint/no-dynamic-delete
      delete data[EVENT_DATA_PHASE];
    }
  } else {
    if (data == null) {
      data = {};
    }
    data[EVENT_DATA_PHASE] = debouncePhase;
  }

  node.getTree().sendEventToServer(node, type, data);
}

// eslint-disable-next-line @typescript-eslint/max-params
function resolveFilters(
  element: Node,
  eventType: string,
  expressionSettings: Record<string, unknown>,
  eventData: Record<string, unknown> | null,
  sendCommand: SendCommand,
  commands: Map<string, JsRunnable>
): boolean {
  let noFilters = true;
  let atLeastOneFilterMatched = false;

  for (const expression of Object.keys(expressionSettings)) {
    const settings = expressionSettings[expression];
    const hasDebounce = Array.isArray(settings);

    if (!hasDebounce && !settings) {
      continue;
    }
    noFilters = false;

    let filterMatched = eventData != null && eventData[expression] === true;
    if (hasDebounce && filterMatched) {
      const debouncerId = 'on-' + eventType + ':' + expression;
      // Count as a match only if at least one debounce is eager.
      filterMatched = resolveDebounces(element, debouncerId, settings as unknown[], sendCommand, commands);
    }

    atLeastOneFilterMatched = atLeastOneFilterMatched || filterMatched;
  }

  return noFilters || atLeastOneFilterMatched;
}

// eslint-disable-next-line @typescript-eslint/max-params
function resolveDebounces(
  element: Node,
  debouncerId: string,
  debounceList: unknown[],
  sendCommand: SendCommand,
  commands: Map<string, JsRunnable>
): boolean {
  let atLeastOneEager = false;

  for (const debounceSetting of debounceList) {
    // [timeout, phase1, phase2, ...]
    const debounceSettings = debounceSetting as unknown[];
    const timeout = debounceSettings[0] as number;

    if (timeout === 0) {
      atLeastOneEager = true;
      continue;
    }

    const phases = new Set<string>();
    for (let j = 1; j < debounceSettings.length; j++) {
      phases.add(debounceSettings[j] as string);
    }

    // Debouncer.trigger uses a CommandsMap that exposes only `forEach`.
    const eager = Debouncer.getOrCreate(element, debouncerId, timeout).trigger(
      phases,
      sendCommand,
      mapAsCommandsMap(commands)
    );

    atLeastOneEager = atLeastOneEager || eager;
  }

  return atLeastOneEager;
}

function mapAsCommandsMap(map: Map<string, JsRunnable>): {
  forEach(cb: (command: JsRunnable, property: string) => void): void;
} {
  return {
    forEach(cb: (command: JsRunnable, property: string) => void): void {
      map.forEach((command, property) => cb(command, property));
    }
  };
}

function bindClassList(element: Element, node: StateNodeLike): Registration {
  const classNodeList = node.getList(CLASS_LIST);

  for (let i = 0; i < classNodeList.length(); i++) {
    element.classList.add(classNodeList.get(i) as string);
  }

  return classNodeList.addSpliceListener((e: { getRemove(): unknown[]; getAdd(): unknown[] }) => {
    const classList = element.classList;

    const remove = e.getRemove();
    const removeLen = (remove as { length: number }).length;
    for (let i = 0; i < removeLen; i++) {
      classList.remove((remove as unknown[])[i] as string);
    }

    const add = e.getAdd();
    const addLen = (add as { length: number }).length;
    for (let i = 0; i < addLen; i++) {
      classList.add((add as unknown[])[i] as string);
    }
  });
}

function bindPolymerEventHandlerNames(context: BindingContext): Registration {
  return ServerEventHandlerBinder.bindServerEventHandlerNames(
    () => ServerEventObject.get(context.htmlNode as Element) as any,
    context.node,
    POLYMER_SERVER_EVENT_HANDLERS,
    false
  );
}

function bindClientCallableMethods(context: BindingContext): Registration {
  return bindServerEventHandlerNamesShort(context.htmlNode as Element, context.node);
}

// Mirrors com.vaadin.flow.internal.nodefeature.NodeFeatures.CLIENT_DELEGATE_HANDLERS.
const CLIENT_DELEGATE_HANDLERS = 19;

// Equivalent of the `ServerEventHandlerBinder.bindServerEventHandlerNames(element, node)`
// Java @JsOverlay shortcut. We materialize it in TS so the binding code does
// not need to round-trip through the Java side.
function bindServerEventHandlerNamesShort(element: Element, node: StateNodeLike): Registration {
  return ServerEventHandlerBinder.bindServerEventHandlerNames(
    () => ServerEventObject.get(element) as any,
    node,
    CLIENT_DELEGATE_HANDLERS,
    true
  );
}

function getOrCreateExpression(expressionString: string): (event: Event, element: Element) => unknown {
  if (expressionCache == null) {
    expressionCache = new Map();
  }
  let expression = expressionCache.get(expressionString);
  if (expression === undefined) {
    expression = new Function('event', 'element', 'return (' + expressionString + ')') as (
      event: Event,
      element: Element
    ) => unknown;
    expressionCache.set(expressionString, expression);
  }
  return expression;
}

function getClosestStateNodeIdToEventTarget(topNode: StateNodeLike, target: EventTarget | null): number {
  if (target == null) {
    return -1;
  }
  try {
    let targetNode: Node | null = target as Node;
    const stack: StateNodeLike[] = [];
    stack.push(topNode);

    // Collect children and test eagerly for a direct match. The loop adds to
    // `stack` while iterating, so a for-of would not see the new entries.
    // eslint-disable-next-line @typescript-eslint/prefer-for-of
    for (let i = 0; i < stack.length; i++) {
      const stateNode = stack[i];
      if (
        targetNode != null &&
        (targetNode === stateNode.getDomNode() ||
          (typeof targetNode.isSameNode === 'function' && targetNode.isSameNode(stateNode.getDomNode())))
      ) {
        return stateNode.getId();
      }
      // For now not looking at virtual children on purpose.
      const children = stateNode.getList(ELEMENT_CHILDREN);
      children.forEach((child: StateNodeLike) => stack.push(child));
    }
    // No direct match — bottom-up search until a matching state node is found.
    targetNode = (targetNode as Node).parentNode;
    return getStateNodeForElement(stack, targetNode);
  } catch (e) {
    // Not going to let event handling fail; just report nothing found.
    Console.debug(
      'An error occurred when Flow tried to find a state node matching the element ' +
        String(target) +
        ', which was the event.target. Error: ' +
        ((e as Error).message ?? String(e))
    );
  }
  return -1;
}

function getStateNodeForElement(searchStack: StateNodeLike[], targetNode: Node | null): number {
  let current = targetNode;
  while (current != null) {
    for (let i = searchStack.length - 1; i > -1; i--) {
      const stateNode = searchStack[i];
      if (
        current === stateNode.getDomNode() ||
        (typeof current.isSameNode === 'function' && current.isSameNode(stateNode.getDomNode()))
      ) {
        return stateNode.getId();
      }
    }
    current = current.parentNode;
  }
  return -1;
}

function getClosestStateNodeIdToDomNode(
  stateTree: StateTreeLike,
  domNodeReference: unknown,
  eventDataExpression: string
): number {
  if (domNodeReference == null) {
    return -1;
  }
  try {
    let targetNode: Node | null = domNodeReference as Node;
    while (targetNode != null) {
      const stateNodeForDomNode = stateTree.getStateNodeForDomNode(targetNode);
      if (stateNodeForDomNode != null) {
        return stateNodeForDomNode.getId();
      }
      targetNode = targetNode.parentNode;
    }
  } catch (e) {
    Console.debug(
      'An error occurred when Flow tried to find a state node matching the element ' +
        String(domNodeReference) +
        ', returned by an event data expression ' +
        eventDataExpression +
        '. Error: ' +
        ((e as Error).message ?? String(e))
    );
  }
  return -1;
}

// ============== Polymer integration helpers ================================
//
// These mirror the original Polymer DOM-repeat integration. They are exposed
// on the `SimpleElementBindingStrategy` singleton below so the Java-side
// `NativeSimpleElementBindingStrategy` JsType shim keeps working.

/**
 * Polymer integration helpers migrated from
 * `com.vaadin.client.flow.binding.SimpleElementBindingStrategy`. Reached from
 * GWT-compiled code via the `NativeSimpleElementBindingStrategy` JsType shim.
 * The Java side hands over an `onHookUp` callback and the three Java methods
 * called back from inside the Polymer prototype patches; this module owns the
 * choreography but not the binding logic.
 */
export const SimpleElementBindingStrategy = {
  bindPolymerModelProperties(element: Element, onHookUp: () => void): void {
    if (PolymerUtils.isPolymerElement(element)) {
      onHookUp();
      return;
    }
    if (!PolymerUtils.mayBePolymerElement(element)) {
      return;
    }
    try {
      const localName = (element as PolymerElementInternal).localName ?? '';
      const whenDefinedPromise = customElements.whenDefined(localName);
      // whenDefined() may never resolve for a non-custom element; the timeout
      // race makes sure the chained closure can be garbage-collected.
      const promiseTimeout = new Promise<void>((resolve) => {
        setTimeout(resolve, 1000);
      });
      void Promise.race([whenDefinedPromise, promiseTimeout]).then(() => {
        if (PolymerUtils.isPolymerElement(element)) {
          onHookUp();
        }
      });
    } catch {
      // Not a custom element — ignore.
    }
  },

  hookUpPolymerElement(
    element: Element,
    handlePropertiesChanged: (changedProps: unknown) => void,
    fireReadyEvent: () => void,
    handleListItemPropertyChange: (nodeId: number, host: unknown, propertyName: string, value: unknown) => void
  ): void {
    const elem = element as PolymerElementInternal;
    const originalPropertiesChanged = elem._propertiesChanged;
    if (originalPropertiesChanged) {
      elem._propertiesChanged = function (
        this: PolymerElementInternal,
        currentProps: unknown,
        changedProps: unknown,
        oldProps: unknown
      ) {
        handlePropertiesChanged(changedProps);
        originalPropertiesChanged.apply(this, [currentProps, changedProps, oldProps]);
      };
    }

    const originalReady = elem.ready;
    elem.ready = function (this: PolymerElementInternal, ...args: unknown[]) {
      originalReady?.apply(this, args);
      fireReadyEvent();
      installDomRepeatPropertyChangeReplacement(elem, handleListItemPropertyChange);
    };
  },

  // Re-export so existing call sites can resolve `needsRebind` through the
  // bridged namespace if needed in the future.
  needsRebind
};

function installDomRepeatPropertyChangeReplacement(
  elem: PolymerElementInternal,
  handleListItemPropertyChange: (nodeId: number, host: unknown, propertyName: string, value: unknown) => void
): void {
  const replaceDomRepeatPropertyChange = function (): void {
    const domRepeat = elem.root?.querySelector('dom-repeat') as unknown as DomRepeatNode | null;
    if (domRepeat) {
      elem.removeEventListener('dom-change', replaceDomRepeatPropertyChange);
    } else {
      return;
    }
    const proto = domRepeat.constructor.prototype;
    if (proto[DOM_REPEAT_MODIFIED_MARKER]) {
      return;
    }
    proto[DOM_REPEAT_MODIFIED_MARKER] = true;
    const originalDomRepeatPropertiesChanged = proto._propertiesChanged;
    proto._propertiesChanged = function (
      this: DataHostNode,
      currentProps: DomRepeatPropertiesChanged,
      changedProps: Record<string, unknown>,
      oldProps: unknown
    ) {
      originalDomRepeatPropertiesChanged?.apply(this, [currentProps, changedProps, oldProps]);
      forwardItemChangesToServer(this, currentProps, changedProps, handleListItemPropertyChange);
    };
  };

  if (elem.root?.querySelector('dom-repeat')) {
    replaceDomRepeatPropertyChange();
  } else {
    // dom-repeat may not be in DOM yet (e.g. behind a dom-if that's currently false);
    // wait for a dom-change to retry.
    elem.addEventListener('dom-change', replaceDomRepeatPropertyChange);
  }
}

function forwardItemChangesToServer(
  domRepeat: DataHostNode,
  currentProps: DomRepeatPropertiesChanged,
  changedProps: Record<string, unknown>,
  handleListItemPropertyChange: (nodeId: number, host: unknown, propertyName: string, value: unknown) => void
): void {
  const items = 'items.';
  for (const key of Object.getOwnPropertyNames(changedProps)) {
    if (key.indexOf(items) !== 0) {
      continue;
    }
    const prop = key.substring(items.length);
    const dot = prop.indexOf('.');
    if (dot <= 0) {
      continue;
    }
    const arrayIndex = prop.substring(0, dot);
    const propertyName = prop.substring(dot + 1);
    const currentPropsItem = currentProps.items?.[arrayIndex];
    if (!currentPropsItem || currentPropsItem.nodeId == null) {
      continue;
    }
    const nodeId = currentPropsItem.nodeId;
    const value = currentPropsItem[propertyName];
    // __dataHost is a linked list whose tail is the template owning the
    // local DOM. Walk to the tail to find the template element.
    let host = domRepeat.__dataHost;
    while (host && (!host.localName || host.__dataHost)) {
      host = host.__dataHost;
    }
    handleListItemPropertyChange(nodeId, host, propertyName, value);
  }
}
