/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.client.flow.binding;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jsinterop.annotations.JsFunction;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.Command;
import com.vaadin.client.Console;
import com.vaadin.client.ElementUtil;
import com.vaadin.client.ExistingElementMap;
import com.vaadin.client.InitialPropertiesHandler;
import com.vaadin.client.LitUtils;
import com.vaadin.client.PolymerUtils;
import com.vaadin.client.ReactUtils;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsMap.ForEachCallback;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.collection.JsWeakMap;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.dom.DomElement;
import com.vaadin.client.flow.dom.DomElement.DomTokenList;
import com.vaadin.client.flow.dom.DomNode;
import com.vaadin.client.flow.model.UpdatableModelProperties;
import com.vaadin.client.flow.nodefeature.ListSpliceEvent;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Computation;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.shared.JsonConstants;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.events.EventTarget;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Binding strategy for a simple (not template) {@link Element} node.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SimpleElementBindingStrategy implements BindingStrategy<Element> {

    private static final String INITIAL_CHANGE = "isInitialChange";

    private static final String HIDDEN_ATTRIBUTE = "hidden";

    private static final String ELEMENT_ATTACH_ERROR_PREFIX = "Element addressed by the ";

    @FunctionalInterface
    private interface PropertyUser {
        void use(MapProperty property);
    }

    /**
     * Callback interface for an event expression parsed using new Function() in
     * JavaScript.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    private interface EventExpression {
        /**
         * Callback interface for an event expression parsed using new
         * Function() in JavaScript.
         *
         * @param event
         *            Event to expand
         * @param element
         *            target Element
         * @return Result of evaluated function
         */
        JsonValue evaluate(Event event, Element element);
    }

    private static JsMap<String, EventExpression> expressionCache;

    /**
     * This is used as a weak set. Only keys are important so that they are
     * weakly referenced
     */
    private static JsWeakMap<StateNode, Boolean> boundNodes;

    /**
     * Just a context class whose instance is passed as a parameter between the
     * operations of various kind to be able to access the data like listeners,
     * node and element which they operate on.
     * <p>
     * It's used to avoid having methods with a long numbers of parameters and
     * because the strategy instance is stateless.
     */
    private static class BindingContext {

        private final Node htmlNode;
        private final StateNode node;
        private final BinderContext binderContext;

        private final JsMap<String, Computation> listenerBindings = JsCollections
                .map();
        private final JsMap<String, EventRemover> listenerRemovers = JsCollections
                .map();

        private BindingContext(StateNode node, Node htmlNode,
                BinderContext binderContext) {
            this.node = node;
            this.htmlNode = htmlNode;
            this.binderContext = binderContext;
        }
    }

    private static class InitialPropertyUpdate {
        private Runnable command;
        private final StateNode node;

        private InitialPropertyUpdate(StateNode node) {
            this.node = node;
        }

        private void setCommand(Runnable command) {
            this.command = command;
        }

        private void execute() {
            if (command != null) {
                command.run();
            }
            node.clearNodeData(this);
        }
    }

    private String getNamespace(StateNode node) {
        return (String) node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.NAMESPACE).getValue();
    }

    @Override
    public Element create(StateNode node) {
        String tag = getTag(node);

        assert tag != null : "New child must have a tag";

        String namespace = getNamespace(node);
        if (namespace != null) {
            return Browser.getDocument().createElementNS(namespace, tag);
        } else if (node.getParent() != null) {
            String namespaceURI = node.getParent().getDomNode()
                    .getNamespaceURI();
            if (namespaceURI != null) {
                return Browser.getDocument().createElementNS(namespaceURI, tag);
            }
        }

        return Browser.getDocument().createElement(tag);
    }

    @Override
    public boolean isApplicable(StateNode node) {
        if (node.hasFeature(NodeFeatures.ELEMENT_DATA)) {
            return true;
        }
        return node.getTree() != null
                && node.equals(node.getTree().getRootNode());
    }

    @Override
    public void bind(StateNode stateNode, Element htmlNode,
            BinderContext nodeFactory) {
        boolean isVisible = isVisible(stateNode);

        assert hasSameTag(stateNode, htmlNode) : "Element tag name is '"
                + htmlNode.getTagName() + "', but the required tag name is "
                + getTag(stateNode);

        if (boundNodes == null) {
            boundNodes = JsCollections.weakMap();
        }

        if (boundNodes.has(stateNode)) {
            return;
        }
        boundNodes.set(stateNode, true);

        BindingContext context = new BindingContext(stateNode, htmlNode,
                nodeFactory);

        JsArray<JsMap<String, Computation>> computationsCollection = JsCollections
                .array();

        JsArray<EventRemover> listeners = JsCollections.array();

        if (isVisible) {
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
            listeners.push(bindMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES,
                    property -> updateStyleProperty(property, htmlNode),
                    createComputations(computationsCollection), stateNode));

            // The things that might actually be observed
            listeners.push(bindMap(NodeFeatures.ELEMENT_ATTRIBUTES,
                    property -> updateAttribute(property, htmlNode),
                    createComputations(computationsCollection), stateNode));
            listeners.push(bindMap(NodeFeatures.ELEMENT_PROPERTIES,
                    property -> updateProperty(property, htmlNode),
                    createComputations(computationsCollection), stateNode));
            bindPolymerModelProperties(stateNode, htmlNode);

            // Prepare teardown
            listeners.push(stateNode.addUnregisterListener(
                    e -> remove(listeners, context, computationsCollection)));
        }
        listeners.push(bindVisibility(listeners, context,
                computationsCollection, nodeFactory));

        scheduleInitialExecution(stateNode);
    }

    private void scheduleInitialExecution(StateNode stateNode) {
        InitialPropertyUpdate update = new InitialPropertyUpdate(stateNode);
        stateNode.setNodeData(update);
        /*
         * Update command will be executed after all initial Reactive stuff.
         * E.g. initial JS (if any) will be executed BEFORE initial update
         * command execution
         */
        Reactive.addPostFlushListener(
                () -> Scheduler.get().scheduleDeferred(() -> {
                    InitialPropertyUpdate propertyUpdate = stateNode
                            .getNodeData(InitialPropertyUpdate.class);
                    // cleared if handlePropertiesChanged has already happened
                    if (propertyUpdate != null) {
                        propertyUpdate.execute();
                    }
                }));
    }

    private native void bindPolymerModelProperties(StateNode node,
            Element element)
    /*-{
      if ( @com.vaadin.client.PolymerUtils::isPolymerElement(*)(element) ) {
          this.@SimpleElementBindingStrategy::hookUpPolymerElement(*)(node, element);
      } else if ( @com.vaadin.client.PolymerUtils::mayBePolymerElement(*)(element) ) {
          var self = this;
          try {
              var whenDefinedPromise = $wnd.customElements.whenDefined(element.localName);
              var promiseTimeout = new Promise(function(r) { setTimeout(r, 1000); });
              // if element is not a web component, the promise returned by
              // whenDefined may never complete, causing memory leaks because of
              // closures in chained function.
              // Using `Promise.race` with a secondary promise that resolves after
              // a defined interval and chaining on this one, will always resolve,
              // execute the function and allow the garbage collector to free resources
              Promise.race([whenDefinedPromise, promiseTimeout]).then( function () {
                  if ( @com.vaadin.client.PolymerUtils::isPolymerElement(*)(element) ) {
                      self.@SimpleElementBindingStrategy::hookUpPolymerElement(*)(node, element);
                  }
              });
          }
          catch (e) {
              // ignore the exception: the element cannot be a custom element
          }
      }
    }-*/;

    private native void hookUpPolymerElement(StateNode node, Element element)
    /*-{
        var self = this;

        var originalPropertiesChanged = element._propertiesChanged;

        if (originalPropertiesChanged) {
            element._propertiesChanged = function (currentProps, changedProps, oldProps) {
                $entry(function () {
                    self.@SimpleElementBindingStrategy::handlePropertiesChanged(*)(changedProps, node);
                })();
                originalPropertiesChanged.apply(this, arguments);
            };
        }


        var tree = node.@com.vaadin.client.flow.StateNode::getTree()();

        var originalReady = element.ready;

        element.ready = function (){
            originalReady.apply(this, arguments);
            @com.vaadin.client.PolymerUtils::fireReadyEvent(*)(element);

            // The  _propertiesChanged method which is replaced above for the element
            // doesn't do anything for items in dom-repeat.
            // Instead it's called with some meaningful info for the <code>dom-repeat</code> element.
            // So here the <code>_propertiesChanged</code> method is replaced
            // for the <code>dom-repeat</code> prototype
            // which changes this method for any dom-repeat instance.
            var replaceDomRepeatPropertyChange = function(){
                var domRepeat = element.root.querySelector('dom-repeat');

                if ( domRepeat ){
                 // If the <code>dom-repeat</code> element is in the DOM then
                 // this method should not be executed anymore. The logic below will replace
                 // the <code>_propertiesChanged</code> method in its prototype so that our
                 // method will work for any dom-repeat instance.
                 element.removeEventListener('dom-change', replaceDomRepeatPropertyChange);
                }
                else {
                    return;
                }
                // if dom-repeat is found => replace _propertiesChanged method in the prototype and mark it as replaced.
                if ( !domRepeat.constructor.prototype.$propChangedModified){
                    domRepeat.constructor.prototype.$propChangedModified = true;

                    var changed = domRepeat.constructor.prototype._propertiesChanged;

                    domRepeat.constructor.prototype._propertiesChanged = function(currentProps, changedProps, oldProps){
                        changed.apply(this, arguments);

                        var props = Object.getOwnPropertyNames(changedProps);
                        var items = "items.";
                        var i;
                        for(i=0; i<props.length; i++){
                            // There should be a property which starts with "items."
                            // and the next token is the index of changed item
                            // the code parses this proeprty
                            var index = props[i].indexOf(items);
                            if ( index == 0 ){
                                var prop = props[i].substr(items.length);
                                index = prop.indexOf('.');
                                if ( index >0 ){
                                    // this is the index of the changed item
                                    var arrayIndex = prop.substr(0,index);
                                    // this is the property name of the changed item
                                    var propertyName = prop.substr(index+1);
                                    var currentPropsItem = currentProps.items[arrayIndex];
                                    if( currentPropsItem && currentPropsItem.nodeId ){
                                        var nodeId = currentPropsItem.nodeId;
                                        var value = currentPropsItem[propertyName];

                                        // this is an attempt to find the template element
                                        // which is not available as a context in the protype method
                                        var host = this.__dataHost;
                                        // __dataHost is an element in the local DOM which owns the changed data
                                        // Such elements form a linked list where the head is the dom-repeat (this)
                                        //  and the tail is the template which owns the local DOM, so this code
                                        // goes via this list and search for the tail which is supposed to be a template
                                        while( !host.localName || host.__dataHost ){
                                            host = host.__dataHost;
                                        }

                                        $entry(function () {
                                            @SimpleElementBindingStrategy::handleListItemPropertyChange(*)(nodeId, host, propertyName, value, tree);
                                        })();
                                    }
                                }
                            }
                        }
                    };
                }
            };

            // dom-repeat doesn't have to be in DOM even if template has it
            //  such situation happens if there is dom-if e.g. which evaluates to <code>false</code> initially.
            // in this case dom-repeat is not yet in the DOM tree until dom-if becomes <code>true</code>
            if ( element.root && element.root.querySelector('dom-repeat') ){
                replaceDomRepeatPropertyChange();
            }
            else {
                // if there is no dom-repeat at the moment just add a dom-change
                // listener which will be notified once local DOM is changed
                // and the  <code>replaceDomRepeatPropertyChange</code> will get a chance
                // to execute its logic if there is dom-repeat.
                element.addEventListener('dom-change',replaceDomRepeatPropertyChange);
            }
        }

    }-*/;

    private static void handleListItemPropertyChange(double nodeId,
            Element host, String property, Object value, StateTree tree) {
        // Warning : it's important that <code>tree</code> is passed as an
        // argument instead of StateNode or Element ! We have replaced a method
        // in the prototype which means that it may not use the context from the
        // hookUpPolymerElement method. Only a tree may be use as a context
        // since StateTree is a singleton.
        StateNode node = tree.getNode((int) nodeId);

        if (!node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
            return;
        }

        assert checkParent(node, host)
                : "Host element is not a parent of the node whose property has changed. "
                        + "This is an implementation error. "
                        + "Most likely it means that there are several StateTrees on the same page "
                        + "(might be possible with portlets) and the target StateTree should not be passed "
                        + "into the method as an argument but somehow detected from the host element. "
                        + "Another option is that host element is calculated incorrectly.";

        // TODO: this code doesn't care about "security feature" which prevents
        // sending
        // data from the client side to the server side if property is not
        // "updatable". See <code>handlePropertyChange</code> and
        // UpdatableModelProperties.
        // It should be aware of that. The current issue is that we don't know
        // the full property path (dot separated) to the property which is a
        // property for the <code>host</code> StateNode and not
        // for the <code>node</code> below. It's tricky to calculate FQN
        // property name at this point though the <code>host</code> element
        // which is
        // the template element could be used for that: a StateNode of
        // <code>host</code> is an ancestor of the <code>node</code> and it
        // should be possible to calculate FQN using this info. Also at the
        // moment
        // AllowClientUpdates ignores bean properties in
        // lists ( if "list" is a property name of list type property and
        // "name" is a property of a bean then
        // "list.name" is not in the UpdatableModelProperties ).
        NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        MapProperty mapProperty = map.getProperty(property);
        mapProperty.syncToServer(value);
    }

    private static boolean checkParent(StateNode node, Element supposedParent) {
        StateNode parent = node;
        while (true) {
            parent = parent.getParent();
            if (parent == null) {
                return false;
            }
            if (supposedParent.equals(parent.getDomNode())) {
                return true;
            }
        }
    }

    private void handlePropertiesChanged(
            JavaScriptObject changedPropertyPathsToValues, StateNode node) {
        String[] keys = WidgetUtil.getKeys(changedPropertyPathsToValues);

        Runnable runnable = () -> {
            for (String propertyName : keys) {
                handlePropertyChange(propertyName,
                        () -> WidgetUtil.getJsProperty(
                                changedPropertyPathsToValues, propertyName),
                        node);
            }
        };

        InitialPropertyUpdate initialUpdate = node
                .getNodeData(InitialPropertyUpdate.class);
        if (initialUpdate == null) {
            runnable.run();
        } else {
            initialUpdate.setCommand(runnable);
        }
    }

    private void handlePropertyChange(String fullPropertyName,
            Supplier<Object> valueProvider, StateNode node) {
        UpdatableModelProperties updatableProperties = node
                .getNodeData(UpdatableModelProperties.class);
        if (updatableProperties == null
                || !updatableProperties.isUpdatableProperty(fullPropertyName)) {
            // don't do anything if the property/sub-property is not in the
            // collection of updatable properties
            return;
        }

        // This is not the property value itself, its a parent node of the
        // property
        String[] subProperties = fullPropertyName.split("\\.");
        StateNode model = node;
        MapProperty mapProperty = null;
        int i = 0;
        int size = subProperties.length;
        for (String subProperty : subProperties) {
            NodeMap elementProperties = model
                    .getMap(NodeFeatures.ELEMENT_PROPERTIES);
            if (!elementProperties.hasPropertyValue(subProperty)
                    && i < size - 1) {
                Console.debug("Ignoring property change for property '"
                        + fullPropertyName
                        + "' which isn't defined from server");
                return;
            }

            mapProperty = elementProperties.getProperty(subProperty);
            if (mapProperty.getValue() instanceof StateNode) {
                model = (StateNode) mapProperty.getValue();
            }
            i++;
        }
        if (mapProperty.getValue() instanceof StateNode) {
            // Don't send to the server updates for list nodes
            StateNode nodeValue = (StateNode) mapProperty.getValue();
            JsonObject obj = WidgetUtil.crazyJsCast(valueProvider.get());
            if (!obj.hasKey("nodeId")
                    || nodeValue.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
                return;
            }
        }
        mapProperty.syncToServer(valueProvider.get());
    }

    private EventRemover bindShadowRoot(BindingContext context) {
        assert context.htmlNode instanceof Element
                : "Cannot bind shadow root to a Node";
        NodeMap map = context.node.getMap(NodeFeatures.SHADOW_ROOT_DATA);

        attachShadow(context);

        return map.addPropertyAddListener(event -> Reactive
                .addFlushListener(() -> attachShadow(context)));
    }

    private void attachShadow(BindingContext context) {
        NodeMap map = context.node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
        StateNode shadowRootNode = (StateNode) map
                .getProperty(NodeProperties.SHADOW_ROOT).getValue();
        if (shadowRootNode != null) {
            NativeFunction function = NativeFunction.create("element",
                    "if ( element.shadowRoot ) { return element.shadowRoot; } "
                            + "else { return element.attachShadow({'mode' : 'open'});}");
            Node shadowRoot = (Node) function.call(null, context.htmlNode);

            if (shadowRootNode.getDomNode() == null) {
                shadowRootNode.setDomNode(shadowRoot);
            }

            BindingContext newContext = new BindingContext(shadowRootNode,
                    shadowRoot, context.binderContext);
            bindChildren(newContext);
        }
    }

    @SuppressWarnings("unchecked")
    private JsMap<String, Computation> createComputations(
            JsArray<JsMap<String, Computation>> computationsCollection) {
        JsMap<String, Computation> computations = JsCollections.map();
        computationsCollection.push(computations);
        return computations;
    }

    private boolean hasSameTag(StateNode node, Element element) {
        String nsTag = getTag(node);
        return nsTag == null || element.getTagName().equalsIgnoreCase(nsTag);
    }

    private EventRemover bindMap(int featureId, PropertyUser user,
            JsMap<String, Computation> bindings, StateNode node) {
        NodeMap map = node.getMap(featureId);
        map.forEachProperty((property,
                name) -> bindProperty(user, property, bindings).recompute());

        return map.addPropertyAddListener(
                e -> bindProperty(user, e.getProperty(), bindings));
    }

    private EventRemover bindVisibility(JsArray<EventRemover> listeners,
            BindingContext context,
            JsArray<JsMap<String, Computation>> computationsCollection,
            BinderContext nodeFactory) {
        assert context.htmlNode instanceof Element
                : "The HTML node for the StateNode with id="
                        + context.node.getId() + " is not an Element";
        NodeMap visibilityData = context.node.getMap(NodeFeatures.ELEMENT_DATA);

        visibilityData.getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY)
                .setValue(isVisible(context.node));
        updateVisibility(listeners, context, computationsCollection,
                nodeFactory);
        return context.node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.VISIBLE)
                .addChangeListener(event -> updateVisibility(listeners, context,
                        computationsCollection, nodeFactory));
    }

    private boolean isVisible(StateNode node) {
        return node.getTree().isVisible(node);
    }

    private void updateVisibility(JsArray<EventRemover> listeners,
            BindingContext context,
            JsArray<JsMap<String, Computation>> computationsCollection,
            BinderContext nodeFactory) {
        assert context.htmlNode instanceof Element
                : "The HTML node for the StateNode with id="
                        + context.node.getId() + " is not an Element";
        NodeMap visibilityData = context.node.getMap(NodeFeatures.ELEMENT_DATA);

        Element element = (Element) context.htmlNode;

        if (needsRebind(context.node) && isVisible(context.node)) {
            remove(listeners, context, computationsCollection);
            Reactive.addFlushListener(() -> {

                restoreInitialHiddenAttribute(element, visibilityData);
                doBind(context.node, nodeFactory);
            });
        } else if (isVisible(context.node)) {
            visibilityData.getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY)
                    .setValue(true);
            restoreInitialHiddenAttribute(element, visibilityData);
        } else {
            setElementInvisible(element, visibilityData);
        }
    }

    private void setElementInvisible(Element element, NodeMap visibilityData) {
        storeInitialHiddenAttribute(element, visibilityData);
        updateAttributeValue(
                visibilityData.getNode().getTree().getRegistry()
                        .getApplicationConfiguration(),
                element, HIDDEN_ATTRIBUTE, Boolean.TRUE);
        if (PolymerUtils.isInShadowRoot(element)) {
            element.getStyle().setDisplay("none");
        }
    }

    private void restoreInitialHiddenAttribute(Element element,
            NodeMap visibilityData) {
        storeInitialHiddenAttribute(element, visibilityData);
        MapProperty initialVisibility = visibilityData
                .getProperty(NodeProperties.VISIBILITY_HIDDEN_PROPERTY);
        if (initialVisibility.hasValue()) {
            updateAttributeValue(
                    visibilityData.getNode().getTree().getRegistry()
                            .getApplicationConfiguration(),
                    element, HIDDEN_ATTRIBUTE, initialVisibility.getValue());
        }

        MapProperty initialDisplay = visibilityData
                .getProperty(NodeProperties.VISIBILITY_STYLE_DISPLAY_PROPERTY);
        if (initialDisplay.hasValue()) {
            final String initialValue = initialDisplay.getValue().toString();
            element.getStyle().setDisplay(initialValue);
        }
    }

    private void storeInitialHiddenAttribute(Element element,
            NodeMap visibilityData) {
        MapProperty initialVisibility = visibilityData
                .getProperty(NodeProperties.VISIBILITY_HIDDEN_PROPERTY);
        if (!initialVisibility.hasValue()) {
            initialVisibility.setValue(element.getAttribute(HIDDEN_ATTRIBUTE));
        }

        MapProperty initialDisplay = visibilityData
                .getProperty(NodeProperties.VISIBILITY_STYLE_DISPLAY_PROPERTY);
        if (PolymerUtils.isInShadowRoot(element) && !initialDisplay.hasValue()
                && element.getStyle() != null) {
            initialDisplay.setValue(element.getStyle().getDisplay());
        }
    }

    private void doBind(StateNode node, BinderContext nodeFactory) {
        Node domNode = node.getDomNode();
        // this will fire an event which gives a chance to run logic which
        // needs to know when the element is completely initialized
        node.setDomNode(null);
        node.setDomNode(domNode);
        nodeFactory.createAndBind(node);
    }

    /**
     * Checks whether the {@code node} needs re-bind.
     * <p>
     * The node needs re-bind if it was initially invisible. As a consequence
     * such node has not be bound. It has been bound in respect to visibility
     * feature only (partially bound). Such node needs re-bind once it becomes
     * visible.
     *
     * @param node
     *            the node to check
     * @return {@code true} if the node is not entirely bound and needs re-bind
     *         later on
     */
    public static boolean needsRebind(StateNode node) {
        /*
         * Absence of value or "true" means that the node doesn't need re-bind.
         * So only "false" means "needs re-bind".
         */
        return Boolean.FALSE.equals(node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY)
                .getValue());
    }

    private static Computation bindProperty(PropertyUser user,
            MapProperty property, JsMap<String, Computation> bindings) {
        String name = property.getName();

        assert !bindings.has(name) : "There's already a binding for " + name;

        Computation computation = Reactive
                .runWhenDependenciesChange(() -> user.use(property));

        bindings.set(name, computation);

        return computation;
    }

    private void updateProperty(MapProperty mapProperty, Element element) {
        String name = mapProperty.getName();
        if (mapProperty.hasValue()) {
            Object treeValue = mapProperty.getValue();
            Object domValue = WidgetUtil.getJsProperty(element, name);
            Optional<Object> previousDomValue = mapProperty
                    .getPreviousDomValue();

            // User might have modified DOM value during server round-trip.
            // That is why we only want to update to the tree value if the tree
            // value is different from the pre-server-round-trip DOM value.
            boolean updateToTreeValue = previousDomValue
                    .map(o -> !WidgetUtil.equals(treeValue, o)).orElse(true);

            // We compare with the current property to avoid setting properties
            // which are updated on the client side, e.g. when synchronizing
            // properties to the server (won't work for readonly properties).
            if (updateToTreeValue && (WidgetUtil.isUndefined(domValue)
                    || !WidgetUtil.equals(domValue, treeValue))) {
                Reactive.runWithComputation(null,
                        () -> WidgetUtil.setJsProperty(element, name,
                                PolymerUtils.createModelTree(treeValue)));
            }
        } else if (WidgetUtil.hasOwnJsProperty(element, name)) {
            WidgetUtil.deleteJsProperty(element, name);
        } else {
            // Can't delete inherited property, so instead just clear
            // the value
            WidgetUtil.setJsProperty(element, name, null);
        }
        mapProperty.clearPreviousDomValue();
    }

    private void updateStyleProperty(MapProperty mapProperty, Element element) {
        String name = mapProperty.getName();
        CSSStyleDeclaration styleElement = element.getStyle();
        if (mapProperty.hasValue()) {
            String value = (String) mapProperty.getValue();
            boolean styleIsSet = false;
            if (value.contains("!important")) {
                Element temp = Browser.getDocument()
                        .createElement(element.getTagName());
                CSSStyleDeclaration tmpStyle = temp.getStyle();
                tmpStyle.setCssText(name + ": " + value + ";");
                String priority = "important";
                if (priority
                        .equals(temp.getStyle().getPropertyPriority(name))) {
                    styleElement.setProperty(name,
                            temp.getStyle().getPropertyValue(name), priority);
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

    private void updateAttribute(MapProperty mapProperty, Element element) {
        String name = mapProperty.getName();
        updateAttributeValue(
                mapProperty.getMap().getNode().getTree().getRegistry()
                        .getApplicationConfiguration(),
                element, name, mapProperty.getValue());
    }

    private EventRemover bindChildren(BindingContext context) {
        NodeList children = context.node.getList(NodeFeatures.ELEMENT_CHILDREN);
        if (children.hasBeenCleared()) {
            removeAllChildren(context.htmlNode);
        }

        for (int i = 0; i < children.length(); i++) {
            StateNode childNode = (StateNode) children.get(i);

            ExistingElementMap existingElementMap = childNode.getTree()
                    .getRegistry().getExistingElementMap();
            Node child = existingElementMap.getElement(childNode.getId());
            if (child != null) {
                existingElementMap.remove(childNode.getId());
                childNode.setDomNode(child);
                context.binderContext.createAndBind(childNode);
            } else {
                child = context.binderContext.createAndBind(childNode);
                DomApi.wrap(context.htmlNode).appendChild(child);
            }
        }

        return children.addSpliceListener(e -> {
            /*
             * Handle lazily so we can create the children we need to insert.
             * The change that gives a child node an element tag name might not
             * yet have been applied at this point.
             */
            Reactive.addFlushListener(() -> handleChildrenSplice(e, context));
        });
    }

    private EventRemover bindVirtualChildren(BindingContext context) {
        NodeList children = context.node.getList(NodeFeatures.VIRTUAL_CHILDREN);

        for (int i = 0; i < children.length(); i++) {
            appendVirtualChild(context, (StateNode) children.get(i), true);
        }

        return children.addSpliceListener(e -> {
            /*
             * Handle lazily so we can create the children we need to insert.
             * The change that gives a child node an element tag name might not
             * yet have been applied at this point.
             */
            Reactive.addFlushListener(() -> {
                JsArray<?> add = e.getAdd();
                if (!add.isEmpty()) {
                    for (int i = 0; i < add.length(); i++) {
                        appendVirtualChild(context, (StateNode) add.get(i),
                                true);
                    }
                }
            });
        });
    }

    private void appendVirtualChild(BindingContext context, StateNode node,
            boolean reactivePhase) {
        JsonObject object = getPayload(node);
        String type = object.getString(NodeProperties.TYPE);

        if (NodeProperties.IN_MEMORY_CHILD.equals(type)) {
            context.binderContext.createAndBind(node);
            return;
        }

        assert context.htmlNode instanceof Element
                : "Unexpected html node. The node is supposed to be a custom element";
        if (NodeProperties.INJECT_BY_ID.equals(type)) {
            if (LitUtils.isLitElement(context.htmlNode)) {
                LitUtils.whenRendered((Element) context.htmlNode,
                        () -> handleInjectId(context, node, object, false));
                return;
            } else if (!PolymerUtils.isReady(context.htmlNode)) {
                PolymerUtils.addReadyListener((Element) context.htmlNode,
                        () -> handleInjectId(context, node, object, false));
                return;
            }

            handleInjectId(context, node, object, reactivePhase);
        } else if (NodeProperties.TEMPLATE_IN_TEMPLATE.equals(type)) {
            if (PolymerUtils.getDomRoot(context.htmlNode) == null) {
                PolymerUtils.addReadyListener((Element) context.htmlNode,
                        () -> handleTemplateInTemplate(context, node, object,
                                false));
                return;
            }
            handleTemplateInTemplate(context, node, object, reactivePhase);
        } else if (NodeProperties.INJECT_BY_NAME.equals(type)) {
            String name = object.getString(NodeProperties.PAYLOAD);
            String address = "name='" + name + "'";

            Supplier<Element> elementLookup = () -> ElementUtil
                    .getElementByName(context.htmlNode, name);

            if (!ReactUtils.isInitialized(elementLookup)) {
                ReactUtils.addReadyCallback((Element) context.htmlNode, name,
                        () -> {
                            doAppendVirtualChild(context, node, false,
                                    elementLookup, name, address);
                        });
                return;
            }
            doAppendVirtualChild(context, node, reactivePhase, elementLookup,
                    name, address);
        } else {
            assert false : "Unexpected payload type " + type;
        }
    }

    private void doAppendVirtualChild(BindingContext context, StateNode node,
            boolean reactivePhase, Supplier<Element> elementLookup, String id,
            String address) {
        if (!verifyAttachRequest(context.node, node, id, address)) {
            return;
        }
        Element element = elementLookup.get();
        if (verifyAttachedElement(element, node, id, address, context)) {
            if (!reactivePhase) {
                InitialPropertiesHandler initialPropertiesHandler = node
                        .getTree().getRegistry().getInitialPropertiesHandler();

                initialPropertiesHandler.nodeRegistered(node);
                initialPropertiesHandler.flushPropertyUpdates();
            }
            node.setDomNode(element);
            context.binderContext.createAndBind(node);
        }
        if (!reactivePhase) {
            // Correct binding requires reactive involvement which doesn't
            // happen automatically when we are out of the phase. So we
            // should
            // call <code>flush()</code> explicitly.
            Reactive.flush();
        }

    }

    private void handleTemplateInTemplate(BindingContext context,
            StateNode node, JsonObject object, boolean reactivePhase) {
        JsonArray path = object.getArray(NodeProperties.PAYLOAD);
        String address = "path='" + path.toString() + "'";

        Supplier<Element> elementLookup = () -> PolymerUtils.getCustomElement(
                PolymerUtils.getDomRoot(context.htmlNode), path);

        doAppendVirtualChild(context, node, reactivePhase, elementLookup, null,
                address);

    }

    private void handleInjectId(BindingContext context, StateNode node,
            JsonObject object, boolean reactivePhase) {
        String id = object.getString(NodeProperties.PAYLOAD);
        String address = "id='" + id + "'";
        Supplier<Element> elementLookup = () -> ElementUtil
                .getElementById(context.htmlNode, id);

        doAppendVirtualChild(context, node, reactivePhase, elementLookup, id,
                address);
    }

    private boolean verifyAttachedElement(Element element, StateNode attachNode,
            String id, String address, BindingContext context) {
        StateNode node = context.node;
        String tag = getTag(attachNode);

        boolean failure = false;
        if (element == null) {
            failure = true;
            Console.warn(ELEMENT_ATTACH_ERROR_PREFIX + address
                    + " is not found. The requested tag name is '" + tag + "'");
        } else if (!ElementUtil.hasTag(element, tag)) {
            failure = true;
            Console.warn(ELEMENT_ATTACH_ERROR_PREFIX + address
                    + " has the wrong tag name '" + element.getTagName()
                    + "', the requested tag name is '" + tag + "'");
        }

        if (failure) {
            node.getTree().sendExistingElementWithIdAttachToServer(node,
                    attachNode.getId(), -1, id);
            return false;
        }

        if (!node.hasFeature(NodeFeatures.SHADOW_ROOT_DATA)) {
            return true;
        }
        NodeMap map = node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
        StateNode shadowRootNode = (StateNode) map
                .getProperty(NodeProperties.SHADOW_ROOT).getValue();
        if (shadowRootNode == null) {
            return true;
        }

        NodeList list = shadowRootNode.getList(NodeFeatures.ELEMENT_CHILDREN);
        Integer existingId = null;

        for (int i = 0; i < list.length(); i++) {
            StateNode stateNode = (StateNode) list.get(i);
            Node domNode = stateNode.getDomNode();

            if (domNode.equals(element)) {
                existingId = stateNode.getId();
                break;
            }
        }

        if (existingId != null) {
            Console.warn(ELEMENT_ATTACH_ERROR_PREFIX + address
                    + " has been already attached previously via the node id='"
                    + existingId + "'");
            node.getTree().sendExistingElementWithIdAttachToServer(node,
                    attachNode.getId(), existingId, id);
            return false;
        }
        return true;
    }

    private boolean verifyAttachRequest(StateNode parent, StateNode node,
            String id, String address) {
        /*
         * This should not happen at all because server side may not send
         * several attach requests for the same client-side element. But that's
         * the situation when the code is written. So this is a kind of
         * assertion for the future code which verifies the correctness of
         * assumptions made on the client side about server-side code impl.
         */
        NodeList virtualChildren = parent
                .getList(NodeFeatures.VIRTUAL_CHILDREN);
        for (int i = 0; i < virtualChildren.length(); i++) {
            StateNode child = (StateNode) virtualChildren.get(i);
            if (child == node) {
                continue;
            }
            if (getPayload(node).toJson().equals(getPayload(child).toJson())) {
                Console.warn("There is already a request to attach "
                        + "element addressed by the " + address
                        + ". The existing request's node id='" + child.getId()
                        + "'. Cannot attach the same element twice.");
                node.getTree().sendExistingElementWithIdAttachToServer(parent,
                        node.getId(), child.getId(), id);
                return false;
            }
        }
        return true;
    }

    private JsonObject getPayload(StateNode node) {
        NodeMap map = node.getMap(NodeFeatures.ELEMENT_DATA);
        return (JsonObject) map.getProperty(NodeProperties.PAYLOAD).getValue();
    }

    private Computation invokeWhenNodeIsConstructed(Command command,
            StateNode node) {
        Computation computation = Reactive.runWhenDependenciesChange(command);
        node.addUnregisterListener(event -> computation.stop());
        return computation;
    }

    private void handleChildrenSplice(ListSpliceEvent event,
            BindingContext context) {
        Node htmlNode = context.htmlNode;
        if (event.isClear()) {
            /*
             * When a full clear event is fired, all nodes must be removed,
             * including the nodes the server doesn't know about.
             */
            removeAllChildren(htmlNode);
        } else {
            JsArray<?> remove = event.getRemove();
            for (int i = 0; i < remove.length(); i++) {
                StateNode childNode = (StateNode) remove.get(i);
                Node child = childNode.getDomNode();

                assert child != null : "Can't find element to remove";

                if (DomApi.wrap(child).getParentNode() == htmlNode) {
                    DomApi.wrap(htmlNode).removeChild(child);
                }
                /*
                 * If the client-side element is not inside the parent the
                 * server thought it should be (because of client-side-only DOM
                 * changes), nothing is done at this point. If the server
                 * appends the element to a new parent, that will override the
                 * client DOM in the code below.
                 */
            }
        }

        JsArray<?> add = event.getAdd();
        if (!add.isEmpty()) {
            addChildren(event.getIndex(), context, add);
        }
    }

    private void removeAllChildren(Node htmlNode) {
        DomElement wrap = DomApi.wrap(htmlNode);
        while (wrap.getFirstChild() != null) {
            wrap.removeChild(wrap.getFirstChild());
        }
    }

    private void addChildren(int index, BindingContext context,
            JsArray<?> add) {
        NodeList nodeChildren = context.node
                .getList(NodeFeatures.ELEMENT_CHILDREN);

        Node beforeRef;
        if (index == 0) {
            // Insert at the first position after the client-side-only nodes
            beforeRef = getFirstNodeMappedAsStateNode(nodeChildren,
                    context.htmlNode);
        } else if (index <= nodeChildren.length() && index > 0) {
            StateNode previousSibling = getPreviousSibling(index, context);
            // Insert before the next sibling of the current node
            beforeRef = previousSibling == null ? null
                    : DomApi.wrap(previousSibling.getDomNode())
                            .getNextSibling();
        } else {
            // Insert at the end
            beforeRef = null;
        }

        for (int i = 0; i < add.length(); i++) {
            Object newChildObject = add.get(i);
            StateNode newChild = (StateNode) newChildObject;

            ExistingElementMap existingElementMap = newChild.getTree()
                    .getRegistry().getExistingElementMap();
            Node childNode = existingElementMap.getElement(newChild.getId());
            if (childNode != null) {
                existingElementMap.remove(newChild.getId());
                newChild.setDomNode(childNode);
                context.binderContext.createAndBind(newChild);
            } else {
                childNode = context.binderContext.createAndBind(newChild);

                DomApi.wrap(context.htmlNode).insertBefore(childNode,
                        beforeRef);
            }

            beforeRef = DomApi.wrap(childNode).getNextSibling();
        }
    }

    private static Node getFirstNodeMappedAsStateNode(
            NodeList mappedNodeChildren, Node htmlNode) {

        JsArray<Node> clientList = DomApi.wrap(htmlNode).getChildNodes();
        for (int i = 0; i < clientList.length(); i++) {
            Node clientNode = clientList.get(i);
            for (int j = 0; j < mappedNodeChildren.length(); j++) {
                StateNode stateNode = (StateNode) mappedNodeChildren.get(j);
                if (clientNode.equals(stateNode.getDomNode())) {
                    return clientNode;
                }
            }
        }
        return null;
    }

    private StateNode getPreviousSibling(int index, BindingContext context) {
        NodeList nodeChildren = context.node
                .getList(NodeFeatures.ELEMENT_CHILDREN);

        int count = 0;
        StateNode node = null;
        for (int i = 0; i < nodeChildren.length(); i++) {
            if (count == index) {
                return node;
            }
            StateNode child = (StateNode) nodeChildren.get(i);
            if (child.getDomNode() != null) {
                node = child;
                count++;
            }
        }
        return node;
    }

    /**
     * Removes all bindings.
     */
    private void remove(JsArray<EventRemover> listeners, BindingContext context,
            JsArray<JsMap<String, Computation>> computationsCollection) {
        ForEachCallback<String, Computation> computationStopper = (computation,
                name) -> computation.stop();

        computationsCollection
                .forEach(collection -> collection.forEach(computationStopper));
        context.listenerBindings.forEach(computationStopper);

        context.listenerRemovers.forEach((remover, name) -> remover.remove());
        listeners.forEach(EventRemover::remove);

        assert boundNodes != null;
        boundNodes.delete(context.node);
    }

    private EventRemover bindDomEventListeners(BindingContext context) {
        NodeMap elementListeners = getDomEventListenerMap(context.node);
        elementListeners.forEachProperty((property, name) -> {
            Computation computation = bindEventHandlerProperty(property,
                    context);

            // Run eagerly to add initial listeners before element is attached
            computation.recompute();
        });

        return elementListeners.addPropertyAddListener(
                event -> bindEventHandlerProperty(event.getProperty(),
                        context));
    }

    private Computation bindEventHandlerProperty(
            MapProperty eventHandlerProperty, BindingContext context) {
        String name = eventHandlerProperty.getName();
        assert !context.listenerBindings.has(name);

        Computation computation = Reactive.runWhenDependenciesChange(() -> {
            boolean hasValue = eventHandlerProperty.hasValue();
            boolean hasListener = context.listenerRemovers.has(name);

            if (hasValue != hasListener) {
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

    private void removeEventHandler(String eventType, BindingContext context) {
        EventRemover remover = context.listenerRemovers.get(eventType);
        context.listenerRemovers.delete(eventType);

        assert remover != null;
        remover.remove();
    }

    private void addEventHandler(String eventType, BindingContext context) {
        assert !context.listenerRemovers.has(eventType);

        EventRemover remover = context.htmlNode.addEventListener(eventType,
                event -> handleDomEvent(event, context), false);

        context.listenerRemovers.set(eventType, remover);
    }

    private NodeMap getDomEventListenerMap(StateNode node) {
        return node.getMap(NodeFeatures.ELEMENT_LISTENERS);
    }

    private void handleDomEvent(Event event, BindingContext context) {
        assert context != null;

        Node element = context.htmlNode;
        StateNode node = context.node;
        assert element instanceof Element
                : "Cannot handle DOM event for a Node";

        String type = event.getType();

        NodeMap listenerMap = getDomEventListenerMap(node);

        ConstantPool constantPool = node.getTree().getRegistry()
                .getConstantPool();
        String expressionConstantKey = (String) listenerMap.getProperty(type)
                .getValue();
        assert expressionConstantKey != null;

        assert constantPool.has(expressionConstantKey);

        JsonObject expressionSettings = constantPool.get(expressionConstantKey);
        String[] expressions = expressionSettings.keys();

        JsonObject eventData;
        JsSet<String> synchronizeProperties = JsCollections.set();

        if (expressions.length == 0) {
            eventData = null;
        } else {
            eventData = Json.createObject();
        }
        for (String expressionString : expressions) {
            if (expressionString
                    .startsWith(JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN)) {
                String property = expressionString.substring(
                        JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN.length());
                synchronizeProperties.add(property);
            } else if (expressionString
                    .equals(JsonConstants.MAP_STATE_NODE_EVENT_DATA)) {
                // map event.target to the closest state node
                int targetNodeId = getClosestStateNodeIdToEventTarget(node,
                        event.getTarget());
                eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                        targetNodeId);
            } else if (expressionString
                    .startsWith(JsonConstants.MAP_STATE_NODE_EVENT_DATA)) {
                // map element returned by JS to the closest state node
                String jsEvaluation = expressionString.substring(
                        JsonConstants.MAP_STATE_NODE_EVENT_DATA.length());
                EventExpression expression = getOrCreateExpression(
                        jsEvaluation);
                JsonValue expressionValue = expression.evaluate(event,
                        (Element) element);
                // find the closest state node matching the expression value
                int targetNodeId = getClosestStateNodeIdToDomNode(
                        node.getTree(), expressionValue, jsEvaluation);
                eventData.put(expressionString, targetNodeId);
            } else {
                EventExpression expression = getOrCreateExpression(
                        expressionString);

                JsonValue expressionValue = expression.evaluate(event,
                        (Element) element);

                eventData.put(expressionString, expressionValue);
            }
        }
        synchronizeProperties.forEach(name -> {
            NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
            MapProperty mapProperty = map.getProperty(name);
            Object domValue = WidgetUtil.getJsProperty(element, name);
            mapProperty.setPreviousDomValue(domValue);
        });

        JsMap<String, Runnable> commands = JsCollections.map();
        synchronizeProperties.forEach(name -> commands.set(name,
                getSyncPropertyCommand(name, context)));

        Consumer<String> sendCommand = debouncePhase -> {
            sendEventToServer(node, type, eventData, debouncePhase);
        };

        boolean sendNow = resolveFilters(element, type, expressionSettings,
                eventData, sendCommand, commands);

        if (sendNow) {
            // Send if there were not filters or at least one matched

            boolean commandAlreadyExecuted = false;
            boolean flushPendingChanges = synchronizeProperties.isEmpty();

            if (flushPendingChanges) {
                // Flush all debounced events so that they don't happen
                // in wrong order in the server-side
                commandAlreadyExecuted = Debouncer.flushAll()
                        .contains(sendCommand);
            }

            if (!commandAlreadyExecuted) {
                commands.mapValues().forEach(Runnable::run);
                sendCommand.accept(null);
            }
        }
    }

    private Runnable getSyncPropertyCommand(String propertyName,
            BindingContext context) {
        return context.node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty(propertyName).getSyncToServerCommand(WidgetUtil
                        .getJsProperty(context.htmlNode, propertyName));
    }

    private static void sendEventToServer(StateNode node, String type,
            JsonObject eventData, String debouncePhase) {
        if (debouncePhase == null) {
            if (eventData != null) {
                eventData.remove(JsonConstants.EVENT_DATA_PHASE);
            }
        } else {
            if (eventData == null) {
                eventData = Json.createObject();
            }
            eventData.put(JsonConstants.EVENT_DATA_PHASE, debouncePhase);
        }

        node.getTree().sendEventToServer(node, type, eventData);
    }

    private static boolean resolveFilters(Node element, String eventType,
            JsonObject expressionSettings, JsonObject eventData,
            Consumer<String> sendCommand, JsMap<String, Runnable> commands) {

        boolean noFilters = true;
        boolean atLeastOneFilterMatched = false;

        for (String expression : expressionSettings.keys()) {
            JsonValue settings = expressionSettings.get(expression);

            boolean hasDebounce = settings.getType() == JsonType.ARRAY;

            if (!hasDebounce && !settings.asBoolean()) {
                continue;
            }
            noFilters = false;

            boolean filterMatched = eventData != null
                    && eventData.getBoolean(expression);
            if (hasDebounce && filterMatched) {
                String debouncerId = "on-" + eventType + ":" + expression;

                // Count as a match only if at least one debounce is eager
                filterMatched = resolveDebounces(element, debouncerId,
                        (JsonArray) settings, sendCommand, commands);
            }

            atLeastOneFilterMatched |= filterMatched;
        }

        return noFilters || atLeastOneFilterMatched;
    }

    private static boolean resolveDebounces(Node element, String debouncerId,
            JsonArray debounceList, Consumer<String> sendCommand,
            JsMap<String, Runnable> commands) {
        boolean atLeastOneEager = false;

        for (int i = 0; i < debounceList.length(); i++) {
            // [timeout, phase1, phase2, ...]
            JsonArray debounceSettings = debounceList.getArray(i);

            double timeout = debounceSettings.getNumber(0);

            if (timeout == 0) {
                atLeastOneEager = true;
                continue;
            }

            JsSet<String> phases = JsCollections.set();
            for (int j = 1; j < debounceSettings.length(); j++) {
                phases.add(debounceSettings.getString(j));
            }

            boolean eager = Debouncer.getOrCreate(element, debouncerId, timeout)
                    .trigger(phases, sendCommand, commands);

            atLeastOneEager |= eager;
        }

        return atLeastOneEager;
    }

    private EventRemover bindClassList(Element element, StateNode node) {
        NodeList classNodeList = node.getList(NodeFeatures.CLASS_LIST);

        for (int i = 0; i < classNodeList.length(); i++) {
            DomApi.wrap(element).getClassList()
                    .add((String) classNodeList.get(i));
        }

        return classNodeList.addSpliceListener(e -> {
            DomTokenList classList = DomApi.wrap(element).getClassList();

            JsArray<?> remove = e.getRemove();
            for (int i = 0; i < remove.length(); i++) {
                classList.remove((String) remove.get(i));
            }

            JsArray<?> add = e.getAdd();
            for (int i = 0; i < add.length(); i++) {
                classList.add((String) add.get(i));
            }
        });
    }

    private EventRemover bindPolymerEventHandlerNames(BindingContext context) {
        return ServerEventHandlerBinder.bindServerEventHandlerNames(
                () -> WidgetUtil.crazyJsoCast(context.htmlNode), context.node,
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS, false);
    }

    private EventRemover bindClientCallableMethods(BindingContext context) {
        assert context.htmlNode instanceof Element
                : "Cannot bind client delegate methods to a Node";
        return ServerEventHandlerBinder.bindServerEventHandlerNames(
                (Element) context.htmlNode, context.node);
    }

    private static void updateAttributeValue(
            ApplicationConfiguration configuration, Element element,
            String attribute, Object value) {
        if (value == null || value instanceof String) {
            WidgetUtil.updateAttribute(element, attribute, (String) value);
        } else {
            JsonValue jsonValue = WidgetUtil.crazyJsoCast(value);
            if (JsonType.OBJECT.equals(jsonValue.getType())) {
                JsonObject object = (JsonObject) jsonValue;
                assert object.hasKey(NodeProperties.URI_ATTRIBUTE)
                        : "Implementation error: JsonObject is recieved as an attribute value for '"
                                + attribute + "' but it has no "
                                + NodeProperties.URI_ATTRIBUTE + " key";
                String uri = object.getString(NodeProperties.URI_ATTRIBUTE);
                if (configuration.isWebComponentMode()
                        && !WidgetUtil.isAbsoluteUrl(uri)) {
                    String baseUri = configuration.getServiceUrl();
                    baseUri = baseUri.endsWith("/") ? baseUri : baseUri + "/";
                    WidgetUtil.updateAttribute(element, attribute,
                            baseUri + uri);
                } else {
                    WidgetUtil.updateAttribute(element, attribute, uri);
                }
            } else {
                WidgetUtil.updateAttribute(element, attribute,
                        value.toString());
            }
        }
    }

    private static EventExpression getOrCreateExpression(
            String expressionString) {
        if (expressionCache == null) {
            expressionCache = JsCollections.map();
        }
        EventExpression expression = expressionCache.get(expressionString);

        if (expression == null) {
            expression = NativeFunction.create("event", "element",
                    "return (" + expressionString + ")");
            expressionCache.set(expressionString, expression);
        }

        return expression;
    }

    // This method could be moved somewhere to be reusable
    private int getClosestStateNodeIdToEventTarget(StateNode topNode,
            EventTarget target) {
        if (target == null) {
            return -1;
        }
        try {
            DomNode targetNode = DomApi.wrap(WidgetUtil.crazyJsCast(target));
            JsArray<StateNode> stack = JsCollections.array();
            stack.push(topNode);

            // collect children and test eagerly for direct match
            for (int i = 0; i < stack.length(); i++) {
                final StateNode stateNode = stack.get(i);
                if (targetNode.isSameNode(stateNode.getDomNode())) {
                    return stateNode.getId();
                }
                // NOTE: for now not looking at virtual children on purpose.
                // If needed (?), those can be included here to the search stack
                stateNode.getList(NodeFeatures.ELEMENT_CHILDREN)
                        .forEach(child -> stack.push((StateNode) child));
            }
            // no direct match, all child element state nodes collected.
            // bottom-up search elements until matching state node found
            targetNode = DomApi.wrap(targetNode.getParentNode());
            return getStateNodeForElement(stack, targetNode);
        } catch (Exception e) {
            // not going to let event handling fail; just report nothing found
            Console.debug(
                    "An error occurred when Flow tried to find a state node matching the element "
                            + target + ", which was the event.target. Error: "
                            + e.getMessage());
        }
        return -1; // no match / error;
    }

    private static int getStateNodeForElement(JsArray<StateNode> searchStack,
            DomNode targetNode) {
        while (targetNode != null) {
            for (int i = searchStack.length() - 1; i > -1; i--) {
                final StateNode stateNode = searchStack.get(i);
                if (targetNode.isSameNode(stateNode.getDomNode())) {
                    return stateNode.getId();
                }
            }
            targetNode = DomApi.wrap(targetNode.getParentNode());
        }
        return -1;
    }

    private int getClosestStateNodeIdToDomNode(StateTree stateTree,
            Object domNodeReference, String eventDataExpression) {
        if (domNodeReference == null) {
            return -1;
        }
        try {
            DomNode targetNode = DomApi
                    .wrap(WidgetUtil.crazyJsCast(domNodeReference));
            while (targetNode != null) {
                StateNode stateNodeForDomNode = stateTree
                        .getStateNodeForDomNode(targetNode);
                if (stateNodeForDomNode != null) {
                    return stateNodeForDomNode.getId();
                }
                targetNode = DomApi.wrap(targetNode.getParentNode());
            }
        } catch (Exception e) {
            // not going to let event handling fail; just report nothing found
            Console.debug(
                    "An error occurred when Flow tried to find a state node matching the element "
                            + domNodeReference
                            + ", returned by an event data expression "
                            + eventDataExpression + ". Error: "
                            + e.getMessage());
        }
        return -1; // no match / error;
    }

}
