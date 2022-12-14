package com.vaadin.client.flow;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.google.gwt.core.client.impl.SchedulerImpl;

import com.vaadin.client.CustomScheduler;
import com.vaadin.client.PolymerUtils;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.model.UpdatableModelProperties;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class GwtPolymerModelTest extends GwtPropertyElementBinderTest {
    private static final String MODEL_PROPERTY_NAME = "model";
    private static final String LIST_PROPERTY_NAME = "listProperty";

    private StateNode node;
    private StateNode modelNode;
    private int nextId;
    private Element element;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        node = createNode();
        modelNode = createAndAttachModelNode(MODEL_PROPERTY_NAME);
        nextId = node.getId() + 1;
        element = createHtmlElement();
        initPolymer(element);

        initScheduler(new CustomScheduler());
        node.setDomNode(element);
    }

    public void testPropertyAdded() {
        Binder.bind(node, element);
        String propertyName = "black";
        String propertyValue = "coffee";

        setModelProperty(node, propertyName, propertyValue);

        assertEquals(propertyValue,
                WidgetUtil.getJsProperty(element, propertyName));
    }

    public void testPropertyUpdated() {
        Binder.bind(node, element);
        String propertyName = "black";
        String propertyValue = "coffee";
        setModelProperty(node, propertyName, propertyValue);
        String newValue = "tea";

        setModelProperty(node, propertyName, newValue);

        assertEquals(newValue, WidgetUtil.getJsProperty(element, propertyName));
    }

    public void testUnregister() {
        Binder.bind(node, element);
        String propertyName = "black";
        String propertyValue = "coffee";
        setModelProperty(node, propertyName, propertyValue);
        String notUpdatedValue = "bubblegum";

        node.unregister();
        setModelProperty(node, propertyName, notUpdatedValue);

        assertEquals(propertyValue,
                WidgetUtil.getJsProperty(element, propertyName));
    }

    public void testSetSubProperty() {
        String subProperty = "subProp";
        String value = "foo";
        setModelProperty(modelNode, subProperty, value, false);

        Binder.bind(node, element);
        Reactive.flush();

        assertEquals(value,
                WidgetUtil.getJsProperty(
                        WidgetUtil.getJsProperty(element, MODEL_PROPERTY_NAME),
                        subProperty));
    }

    public void testUpdateSubProperty() {
        Binder.bind(node, element);

        String subProperty = "subProp";
        String value = "foo";
        setModelProperty(modelNode, subProperty, value, true);

        String newValue = "bar";
        setModelProperty(modelNode, subProperty, newValue, true);

        assertEquals(newValue,
                WidgetUtil.getJsProperty(
                        WidgetUtil.getJsProperty(element, MODEL_PROPERTY_NAME),
                        subProperty));
    }

    public void testSubPropertyUnregister() {
        Binder.bind(node, element);

        String subProperty = "subProp";
        String value = "foo";
        setModelProperty(modelNode, subProperty, value, true);
        node.unregister();
        modelNode.unregister();

        setModelProperty(modelNode, subProperty, "bar", true);

        assertEquals(value,
                WidgetUtil.getJsProperty(
                        WidgetUtil.getJsProperty(element, MODEL_PROPERTY_NAME),
                        subProperty));
    }

    public void testAddList() {
        List<String> serverList = Arrays.asList("one", "two");
        createAndAttachNodeWithList(modelNode, serverList);

        Binder.bind(node, element);
        Reactive.flush();

        assertListsEqual(serverList, getClientList());
    }

    public void testAddBasicTypeList() {
        List<String> serverList = Arrays.asList("one", "two");
        createAndAttachNodeWithList(modelNode, serverList,
                this::createBasicTypeWrapper);

        Binder.bind(node, element);
        Reactive.flush();

        assertListsEqual(serverList, getClientList());
    }

    public void testSetNewListForTheSameProperty() {
        createAndAttachNodeWithList(modelNode, Arrays.asList("one", "two"));

        Binder.bind(node, element);
        Reactive.flush();

        List<String> newServerList = Arrays.asList("1", "2", "3");
        createAndAttachNodeWithList(modelNode, newServerList);

        Reactive.flush();

        assertListsEqual(newServerList, getClientList());
    }

    public void testSetNewBasicTypeListForTheSameProperty() {
        createAndAttachNodeWithList(modelNode, Arrays.asList("one", "two"),
                this::createBasicTypeWrapper);

        Binder.bind(node, element);
        Reactive.flush();

        List<String> newServerList = Arrays.asList("1", "2", "3");
        createAndAttachNodeWithList(modelNode, newServerList,
                this::createBasicTypeWrapper);

        Reactive.flush();

        assertListsEqual(newServerList, getClientList());
    }

    public void testListUpdatesAreIgnoredAfterUnregister() {
        List<String> serverList = Arrays.asList("one", "two");
        StateNode nodeWithList = createAndAttachNodeWithList(modelNode,
                serverList);

        Binder.bind(node, element);

        Reactive.flush();
        node.unregister();
        modelNode.unregister();

        fillNodeWithListItems(nodeWithList, Arrays.asList("1", "2", "3"));
        Reactive.flush();

        assertListsEqual(serverList, getClientList());
    }

    public void testUpdateList() {
        StateNode nodeWithList = createAndAttachNodeWithList(modelNode,
                Arrays.asList("one", "two"));

        assertUpdateListValues(nodeWithList);
    }

    public void testUpdateBasicTypeList() {
        StateNode nodeWithList = createAndAttachNodeWithList(modelNode,
                Arrays.asList("one", "two"), this::createBasicTypeWrapper);

        assertUpdateListValues(nodeWithList);
    }

    public void testPolymerUtilsStoreNodeIdNotAvailableAsListItem() {
        nextId = 98;
        List<String> serverList = Arrays.asList("one", "two");
        StateNode andAttachNodeWithList = createAndAttachNodeWithList(modelNode,
                serverList);

        JsonValue jsonValue = PolymerUtils
                .createModelTree(andAttachNodeWithList);

        assertTrue(
                "Expected instance of JsonObject from converter, but was not.",
                jsonValue instanceof JsonObject);
        double nodeId = ((JsonObject) jsonValue).getNumber("nodeId");
        assertFalse(
                "JsonValue array contained nodeId even though it shouldn't be visible",
                jsonValue.toJson().contains(Double.toString(nodeId)));
        assertEquals("Found nodeId didn't match the set nodeId", 98.0, nodeId);
    }

    public void testLatePolymerInit() {
        emulatePolymerNotLoaded();
        addMockMethods(element);

        String propertyName = "black";
        String propertyValue = "coffee";
        setModelProperty(node, propertyName, propertyValue);

        node.setNodeData(
                new UpdatableModelProperties(JsCollections.array("black")));

        Binder.bind(node, element);
        Reactive.flush();
        assertEquals(
                "Expected to have property with name " + propertyName
                        + " defined after initial binding",
                propertyValue, WidgetUtil.getJsProperty(element, propertyName));

        String newPropertyValue = "bubblegum";
        emulatePolymerPropertyChange(element, propertyName, newPropertyValue);
        Reactive.flush();
        assertEquals(
                "Expected to have property with name " + propertyName
                        + " updated from client side",
                newPropertyValue,
                WidgetUtil.getJsProperty(element, propertyName));

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty(propertyName);
        assertEquals(newPropertyValue, property.getValue());

        assertEquals(newPropertyValue,
                tree.synchronizedProperties.get(node).get(propertyName));

        assertEquals("`_propertiesChanged` should be triggered exactly once",
                1.0, WidgetUtil.getJsProperty(element,
                        "propertiesChangedCallCount"));
        assertEquals(
                "Exactly one `whenDefined.then` callback should be called after element was initialized",
                1.0, WidgetUtil.getJsProperty(element, "callbackCallCount"));
    }

    public void testInitialUpdateModelProperty_propertyIsUpdatable_propertyIsSynced() {
        addMockMethods(element);
        String propertyName = "black";
        String propertyValue = "coffee";
        setModelProperty(node, propertyName, propertyValue);

        node.setNodeData(new UpdatableModelProperties(
                JsCollections.array(propertyName)));

        Binder.bind(node, element);
        Reactive.flush();
        assertEquals(
                "Expected to have property with name " + propertyName
                        + " defined after initial binding",
                propertyValue, WidgetUtil.getJsProperty(element, propertyName));

        String newPropertyValue = "bubblegum";
        emulatePolymerPropertyChange(element, propertyName, newPropertyValue);
        Reactive.flush();
        assertEquals(
                "Expected to have property with name " + propertyName
                        + " updated from client side",
                newPropertyValue,
                WidgetUtil.getJsProperty(element, propertyName));
        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty(propertyName);
        assertEquals(newPropertyValue, property.getValue());

        assertEquals(newPropertyValue,
                tree.synchronizedProperties.get(node).get(propertyName));
    }

    public void testInitialUpdateModelProperty_propertyIsUpdatableAndSchedulerIsNotExecuted_propertyIsNotSync() {
        addMockMethods(element);
        String propertyName = "black";
        String propertyValue = "coffee";
        setModelProperty(node, propertyName, propertyValue);

        initScheduler(new SchedulerImpl() {
            @Override
            public void scheduleDeferred(ScheduledCommand cmd) {
            }
        });

        node.setNodeData(
                new UpdatableModelProperties(JsCollections.array("black")));

        Binder.bind(node, element);
        Reactive.flush();
        assertEquals(
                "Expected to have property with name " + propertyName
                        + " defined after initial binding",
                propertyValue, WidgetUtil.getJsProperty(element, propertyName));

        String newPropertyValue = "bubblegum";
        emulatePolymerPropertyChange(element, propertyName, newPropertyValue);
        Reactive.flush();
        assertEquals(
                "Expected to have property with name " + propertyName
                        + " updated from client side",
                propertyValue, WidgetUtil.getJsProperty(element, propertyName));
        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty(propertyName);
        assertEquals(propertyValue, property.getValue());

        assertFalse(tree.synchronizedProperties.has(node));
    }

    public void testUpdateModelSubProperty_subpropertyIsUpdatableAndIsNotSetFromServer_subpropertyIsSync() {
        addMockMethods(element);
        setModelProperty(node, "bar", modelNode);

        node.setNodeData(
                new UpdatableModelProperties(JsCollections.array("bar.foo")));

        Binder.bind(node, element);
        Reactive.flush();

        String newSubPropertyValue = "baz";
        PolymerUtils.setProperty(element, "bar.foo", "baz");
        emulatePolymerPropertyChange(element, "bar.foo", "baz");
        Reactive.flush();

        assertEquals(
                "Expected to have an object 'bar' with a property named 'foo'"
                        + " updated from client side",
                newSubPropertyValue, WidgetUtil.getJsProperty(
                        WidgetUtil.getJsProperty(element, "bar"), "foo"));

        MapProperty property = modelNode.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");
        assertEquals(newSubPropertyValue, property.getValue());

        assertEquals(newSubPropertyValue,
                tree.synchronizedProperties.get(modelNode).get("foo"));
    }

    public void testUpdateModelProperty_propertyIsNotUpdatable_propertyIsNotSync() {
        emulatePolymerNotLoaded();
        addMockMethods(element);

        String propertyName = "black";
        String propertyValue = "coffee";

        setModelProperty(node, propertyName, propertyValue);

        Binder.bind(node, element);
        Reactive.flush();
        assertEquals(
                "Expected to have property with name " + propertyName
                        + " defined after initial binding",
                propertyValue, WidgetUtil.getJsProperty(element, propertyName));

        emulatePolymerPropertyChange(element, propertyName, "doesNotMatter");
        Reactive.flush();
        assertEquals(
                "Expected the property with name " + propertyName
                        + " not to be updated since it's not updatable",
                propertyValue, WidgetUtil.getJsProperty(element, propertyName));
    }

    ////////////////////////
    private native void addMockMethods(Element element)
    /*-{
        element.propertiesChangedCallCount = 0;
        element._propertiesChanged = function() {
            element.propertiesChangedCallCount += 1;
        };
        element.callbackCallCount = 0;
        $wnd.customElements = {
            whenDefined: function() {
                return new Promise(function(resolve) {
                    $wnd.Polymer = $wnd.OldPolymer;
                    element.callbackCallCount += 1;
                    resolve();
                });
            }
        };
        if( !element.removeAttribute ) {
            element.removeAttribute = function(attribute){
                element[attribute] = null;
            };
        }
        if ( !element.getAttribute ){
            element.getAttribute = function( attribute ){
                return element[attribute];
            };
        }
        if ( !element.setAttribute ){
            element.setAttribute = function( attribute, value ){
                element[attribute] = value;
            };
        }
    }-*/;

    private native void emulatePolymerNotLoaded()
    /*-{
        $wnd.OldPolymer = $wnd.Polymer;
        $wnd.Polymer = null;
    }-*/;

    private native void emulatePolymerPropertyChange(Element element,
            String propertyName, String newPropertyValue)
    /*-{
        var changedProperties = {};
        changedProperties[propertyName] = newPropertyValue;
        element._propertiesChanged({}, changedProperties, {});
    }-*/;

    private native void emulatePolymerPropertyChange(Element element,
            String objectName, String propertyName, String newPropertyValue)
    /*-{
        var changedProperties = {};
        changedProperties[objectName] = {};
        element._propertiesChanged({}, changedProperties, {});
        changedProperties[objectName][propertyName] = newPropertyValue;
        element._propertiesChanged({}, changedProperties, {});
    }-*/;

    @Override
    protected StateNode createNode() {
        StateNode newNode = super.createNode();

        newNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        newNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);
        newNode.getMap(NodeFeatures.ELEMENT_DATA);
        return newNode;
    }

    private void assertUpdateListValues(StateNode nodeWithList) {
        Binder.bind(node, element);
        Reactive.flush();

        List<String> newList = Arrays.asList("1", "2", "3");
        fillNodeWithListItems(nodeWithList, newList);

        Reactive.flush();

        JsonArray argumentsArray = WidgetUtil.crazyJsCast(
                WidgetUtil.getJsProperty(element, "argumentsArray"));

        // Since `fillNodeWithListItems` makes separate `add` call for every
        // element in newList, we will be having
        // the same number of `splice` calls.
        assertEquals(newList.size(), argumentsArray.length());
        for (int i = 0; i < newList.size(); i++) {
            JsonArray arguments = argumentsArray.getArray(i);

            assertEquals(4, arguments.length());
            String path = arguments.getString(0);
            int start = (int) arguments.getNumber(1);
            int deleteCount = (int) arguments.getNumber(2);
            String items = arguments.getString(3);

            assertEquals(MODEL_PROPERTY_NAME + "." + LIST_PROPERTY_NAME, path);
            assertEquals(i, start);
            assertEquals(0, deleteCount);
            assertEquals(newList.get(i), items);
        }
    }

    private StateNode createAndAttachModelNode(String modelPropertyName) {
        StateNode modelNode = new StateNode(nextId, node.getTree());
        modelNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        nextId++;
        setModelProperty(node, modelPropertyName, modelNode, true);
        return modelNode;
    }

    private Element createHtmlElement() {
        String name = "custom-div";
        Element element = Browser.getDocument().createElement(name);
        WidgetUtil.setJsProperty(element, "localName", name);
        setupSetMethod(element);
        setupMockSpliceMethod(element);
        WidgetUtil.setJsProperty(element, "removeAttribute",
                new NativeFunction(""));
        WidgetUtil.setJsProperty(element, "getAttribute",
                new NativeFunction("return false;"));
        WidgetUtil.setJsProperty(element, "setAttribute",
                new NativeFunction(""));
        return element;
    }

    private native void setupSetMethod(Element element)
    /*-{
          element.set = function(path, newValue) {
              var split = path.split(".");
              var prop = element;
              for (var i = 0; i < split.length - 1; i++) {
                if (!prop) {
                  break;
                }
                prop = prop[split[i]];
              }
              if (prop) {
                prop[split[split.length - 1]] = newValue;
              }
          }
    }-*/;

    /**
     * Sets up mock splice method, that is called when model list is modified.
     * For each call, method stores call arguments in the element property named
     * argumentsArray.
     *
     * @param element
     *            html element to set the method to
     */
    private void setupMockSpliceMethod(Element element) {
        NativeFunction function = NativeFunction.create("path", "start",
                "deleteCount", "items",
                "this.argumentsArray ? this.argumentsArray.push(arguments) : this.argumentsArray = [arguments]");
        WidgetUtil.setJsProperty(element, "splice", function);
    }

    private static void setModelProperty(StateNode stateNode, String name,
            Object value, boolean flush) {
        stateNode.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty(name)
                .setValue(value);
        if (value instanceof StateNode) {
            ((StateNode) value).setParent(stateNode);
        }
        if (flush) {
            Reactive.flush();
        }
    }

    private static void setModelProperty(StateNode stateNode, String name,
            Object value) {
        setModelProperty(stateNode, name, value, true);
    }

    private StateNode createAndAttachNodeWithList(StateNode modelNode,
            List<String> listItems, Function<Object, ?> converter) {
        StateNode nodeWithList = new StateNode(nextId, node.getTree());
        nextId++;
        fillNodeWithListItems(nodeWithList, listItems, converter);
        setModelProperty(modelNode, LIST_PROPERTY_NAME, nodeWithList, false);
        return nodeWithList;
    }

    private StateNode createAndAttachNodeWithList(StateNode modelNode,
            List<String> listItems) {
        return createAndAttachNodeWithList(modelNode, listItems,
                Function.identity());
    }

    private void fillNodeWithListItems(StateNode node, List<?> listItems) {
        fillNodeWithListItems(node, listItems, Function.identity());
    }

    private void fillNodeWithListItems(StateNode node, List<?> listItems,
            Function<Object, ?> converter) {
        NodeList nodeList = node.getList(NodeFeatures.TEMPLATE_MODELLIST);
        for (int i = 0; i < listItems.size(); i++) {
            nodeList.add(i, converter.apply(listItems.get(i)));
        }
    }

    private void assertListsEqual(List<String> serverList,
            JsonArray clientList) {
        assertEquals(serverList.size(), clientList.length());
        for (int i = 0; i < serverList.size(); i++) {
            assertEquals(serverList.get(i), clientList.getString(i));
        }
    }

    private JsonArray getClientList() {
        return WidgetUtil.crazyJsCast(WidgetUtil.getJsProperty(
                WidgetUtil.getJsProperty(element, MODEL_PROPERTY_NAME),
                LIST_PROPERTY_NAME));
    }

    private StateNode createBasicTypeWrapper(Object value) {
        StateNode node = new StateNode(nextId, tree);
        nextId++;
        node.getMap(NodeFeatures.BASIC_TYPE_VALUE)
                .getProperty(NodeProperties.VALUE).setValue(value);
        return node;
    }

    private native void initPolymer(Element element)
    /*-{
        $wnd.Polymer = function() {};
        $wnd.Polymer.Element = {};
        $wnd.Polymer.version="2.0.1";
        element.__proto__ = $wnd.Polymer.Element;
    }-*/;

    private native void initScheduler(SchedulerImpl scheduler)
    /*-{
       @com.google.gwt.core.client.impl.SchedulerImpl::INSTANCE = scheduler;
    }-*/;
}
