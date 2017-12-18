/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.client.flow.template;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.ExistingElementMap;
import com.vaadin.client.InitialPropertiesHandler;
import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.binding.SimpleElementBindingStrategy;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.TemplateMap;
import com.vaadin.flow.template.angular.ChildSlotNode;
import com.vaadin.flow.template.angular.ForTemplateNode;
import com.vaadin.flow.template.angular.ModelValueBindingProvider;

import elemental.client.Browser;
import elemental.dom.Document.Events;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.NodeList;
import elemental.dom.Text;
import elemental.events.MouseEvent;
import elemental.json.Json;
import elemental.json.JsonObject;

public class GwtTemplateBinderTest extends ClientEngineTestBase {

    private static final String MODEL_KEY = "key";

    private static int modelTypeCount = 0;

    private Registry registry;
    private StateTree tree;
    private StateNode stateNode;

    private Map<String, JsArray<?>> serverMethods = new HashMap<>();
    private Map<String, StateNode> serverRpcNodes = new HashMap<>();

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        registry = new Registry() {
            {
                set(TemplateRegistry.class, new TemplateRegistry());
                set(ConstantPool.class, new ConstantPool());
                set(ExistingElementMap.class, new ExistingElementMap());
                set(InitialPropertiesHandler.class,
                        new InitialPropertiesHandler(this));
                set(StateTree.class, new StateTree(this) {

                    @Override
                    public void sendTemplateEventToServer(StateNode node,
                            String methodName, JsArray<?> argValues) {
                        serverMethods.put(methodName, argValues);
                        serverRpcNodes.put(methodName, node);
                    }
                });
            }
        };

        tree = registry.getStateTree();

        /**
         * This state node is ALWAYS a template !!!
         */
        stateNode = new StateNode(0, tree) {

            @Override
            public boolean hasFeature(int id) {
                if (id == NodeFeatures.TEMPLATE) {
                    return true;
                }
                return super.hasFeature(id);
            }
        };

        // Use the most common model structure by default
        JsonObject modelType = Json.createObject();
        modelType.put(MODEL_KEY, "String");
        setModelType(stateNode, modelType);
    }

    public void testTemplateProperties() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop1", "value1");
        templateNode.addProperty("prop2", "value2");

        Element element = createElement(templateNode);

        assertEquals("value1", WidgetUtil.getJsProperty(element, "prop1"));
        assertEquals("value2", WidgetUtil.getJsProperty(element, "prop2"));
    }

    public void testTemplateAttributes() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addAttribute("attr1", "value1");
        templateNode.addAttribute("attr2", "value2");

        Element element = createElement(templateNode);

        assertEquals("value1", element.getAttribute("attr1"));
        assertEquals("value2", element.getAttribute("attr2"));
    }

    public void testTemplateTag() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");

        Element element = createElement(templateNode);

        assertEquals("DIV", element.getTagName());
    }

    public void testTemplateChildren() {
        final int childId = 2345;
        TestElementTemplateNode childTemplate = TestElementTemplateNode
                .create("span");
        registry.getTemplateRegistry().register(childId, childTemplate);

        TestElementTemplateNode parentTemplate = TestElementTemplateNode
                .create("div");
        parentTemplate.setChildrenIds(new double[] { childId });

        Element element = createElement(parentTemplate);

        assertEquals(1, element.getChildElementCount());
        assertEquals("SPAN", element.getFirstElementChild().getTagName());
    }

    public void testTemplateText() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createStatic("text"));
        Node domNode = createText(templateNode);
        assertEquals("text", domNode.getTextContent());
    }

    public void testPropertyBindingTemplate() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop", TestBinding
                .createBinding(ModelValueBindingProvider.TYPE, MODEL_KEY));

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue("foo");
        Node domNode = createElement(templateNode);

        Reactive.flush();

        assertEquals("foo", WidgetUtil.getJsProperty(domNode, "prop"));
    }

    public void testUpdatePropertyBindingTemplate() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop", TestBinding
                .createBinding(ModelValueBindingProvider.TYPE, MODEL_KEY));

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue("foo");
        Node domNode = createElement(templateNode);

        Reactive.flush();

        map.getProperty(MODEL_KEY).setValue("bar");

        Reactive.flush();

        assertEquals("bar", WidgetUtil.getJsProperty(domNode, "prop"));
    }

    public void testUnregister_propeprtyBindingUpdateIsNotDone() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop", TestBinding
                .createBinding(ModelValueBindingProvider.TYPE, MODEL_KEY));

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue("foo");
        Node domNode = createElement(templateNode);

        assertEquals(null, WidgetUtil.getJsProperty(domNode, "prop"));

        stateNode.unregister();

        Reactive.flush();
        assertEquals(null, WidgetUtil.getJsProperty(domNode, "prop"));
    }

    public void testPropertyBindingNoValueTemplate() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop", TestBinding
                .createBinding(ModelValueBindingProvider.TYPE, MODEL_KEY));
        Node domNode = createElement(templateNode);

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue(null);

        Reactive.flush();

        assertEquals(null, WidgetUtil.getJsProperty(domNode, "prop"));
    }

    public void testAttributeBindingTemplate() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addAttribute("attr", TestBinding
                .createBinding(ModelValueBindingProvider.TYPE, MODEL_KEY));

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue("foo");
        Element domNode = createElement(templateNode);

        Reactive.flush();

        assertEquals("foo", domNode.getAttribute("attr"));
    }

    public void testUpdateAttributeBindingTemplate() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addAttribute("attr", TestBinding
                .createBinding(ModelValueBindingProvider.TYPE, MODEL_KEY));

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue("foo");
        Element domNode = createElement(templateNode);

        Reactive.flush();

        map.getProperty(MODEL_KEY).setValue("bar");

        Reactive.flush();

        assertEquals("bar", domNode.getAttribute("attr"));
    }

    public void testUnregister_sttributeBindingUpdateIsNotDone() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addAttribute("attr", TestBinding
                .createBinding(ModelValueBindingProvider.TYPE, MODEL_KEY));

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue("foo");
        Element domNode = createElement(templateNode);

        assertEquals(null, domNode.getAttribute("attr"));

        stateNode.unregister();

        Reactive.flush();
        assertEquals(null, domNode.getAttribute("attr"));
    }

    public void testAttributeBindingNoValueTemplate() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addAttribute("attr", TestBinding
                .createBinding(ModelValueBindingProvider.TYPE, MODEL_KEY));
        Element domNode = createElement(templateNode);

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue(null);

        Reactive.flush();

        assertEquals(null, domNode.getAttribute("attr"));
    }

    public void testClassNameBinding() {
        MapProperty property = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP)
                .getProperty(MODEL_KEY);

        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");

        templateNode.addClassName("static", "true");
        templateNode.addClassName("dynamic", TestBinding
                .createBinding(ModelValueBindingProvider.TYPE, MODEL_KEY));

        Element element = createElement(templateNode);

        property.setValue(Boolean.TRUE);
        Reactive.flush();
        assertEquals("static dynamic", element.getClassName());

        property.setValue(Boolean.FALSE);
        Reactive.flush();
        assertEquals("static", element.getClassName());

        // Test that the evaluation logic is based on thruthishness instead of
        // strict boolean evaluation

        // trueish value
        property.setValue("yes");
        Reactive.flush();
        assertEquals("static dynamic", element.getClassName());

        // falseish value
        property.setValue("");
        Reactive.flush();
        assertEquals("static", element.getClassName());

        // trueish value
        property.setValue(Double.valueOf(1));
        Reactive.flush();
        assertEquals("static dynamic", element.getClassName());

        property.removeValue();
        Reactive.flush();
        assertEquals("static", element.getClassName());
    }

    public void testTextValueTemplate() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createTextValueBinding(MODEL_KEY));
        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue("foo");
        Node domNode = createText(templateNode);

        Reactive.flush();

        assertEquals("foo", domNode.getTextContent());
    }

    public void testUpdateTextValueTemplate() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createTextValueBinding(MODEL_KEY));
        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue("foo");
        Node domNode = createText(templateNode);

        Reactive.flush();

        map.getProperty(MODEL_KEY).setValue("bar");

        Reactive.flush();

        assertEquals("bar", domNode.getTextContent());
    }

    public void testUnregister_textBinsingUpdateIsNotDone() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createTextValueBinding(MODEL_KEY));
        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue("foo");
        Node domNode = createText(templateNode);

        assertEquals("", domNode.getTextContent());

        stateNode.unregister();

        Reactive.flush();
        assertEquals("", domNode.getTextContent());
    }

    public void testTextNoValueTemplate() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createTextValueBinding(MODEL_KEY));
        Node domNode = createText(templateNode);

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue(null);

        Reactive.flush();

        assertEquals("", domNode.getTextContent());
    }

    public void testBindOverrideNodeWhenCreated() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        int id = 83;

        StateNode overrideNode = createSimpleOverrideNode(id);

        overrideNode.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty("id")
                .setValue("override");

        Element element = createElement(templateNode, id);

        Reactive.flush();

        assertEquals("override", element.getId());
    }

    public void testBindOverrideNodeAfterCreated() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        int id = 83;

        Element element = createElement(templateNode, id);

        Reactive.flush();

        // tests the rare(ish) case resulting in #1050 (fixed by #1051)
        tree.setUpdateInProgress(true);

        StateNode overrideNode = createSimpleOverrideNode(id);
        overrideNode.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty("id")
                .setValue("override");

        tree.setUpdateInProgress(false);

        Reactive.flush();

        assertEquals("override", element.getId());
    }

    public void testUnregisterOverrideNode() {
        int id = 83;
        StateNode overrideNode = createSimpleOverrideNode(id, 2);

        // Must register so that we can fire an unregister event later on
        tree.registerNode(stateNode);
        tree.registerNode(overrideNode);

        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");

        MapProperty idProperty = overrideNode
                .getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty("id");
        idProperty.setValue("override");

        Element element = createElement(templateNode, id);

        Reactive.flush();

        tree.unregisterNode(stateNode);
        tree.unregisterNode(overrideNode);

        Reactive.flush();

        // Updating override node after unregistering the nodes should not
        // cause the element to update
        idProperty.setValue("new");

        Reactive.flush();

        assertEquals("override", element.getId());
    }

    public void testBindOverrideNode_properties_beforeBind() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        int id = 83;

        StateNode overrideNode = createSimpleOverrideNode(id);
        NodeMap props = overrideNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        props.getProperty("foo").setValue("bar");

        Element element = createElement(templateNode, id);

        Reactive.flush();

        assertEquals("bar", WidgetUtil.getJsProperty(element, "foo"));
    }

    public void testBindOverrideNode_properties_afterBind() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        int id = 68;

        StateNode overrideNode = createSimpleOverrideNode(id);

        Element element = createElement(templateNode, id);

        NodeMap props = overrideNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        props.getProperty("foo").setValue("bar");

        Reactive.flush();

        assertEquals("bar", WidgetUtil.getJsProperty(element, "foo"));
    }

    public void testBindOverrideNode_attributes_beforeBind() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        int id = 48;

        StateNode overrideNode = createSimpleOverrideNode(id);
        NodeMap attrs = overrideNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);
        attrs.getProperty("foo").setValue("bar");

        Element element = createElement(templateNode, id);

        Reactive.flush();

        assertEquals("bar", element.getAttribute("foo"));
    }

    public void testBindOverrideNode_attributes_afterBind() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        int id = 45;

        StateNode overrideNode = createSimpleOverrideNode(id);

        Element element = createElement(templateNode, id);

        NodeMap props = overrideNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);
        props.getProperty("foo").setValue("bar");

        Reactive.flush();

        assertEquals("bar", element.getAttribute("foo"));
    }

    public void testTemplateAttributes_bindOverrideNodeAttribute() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addAttribute("attr1", "value1");
        templateNode.addAttribute("attr2", "value2");

        int id = 37;

        StateNode overrideNode = createSimpleOverrideNode(id);

        Element element = createElement(templateNode, id);

        NodeMap props = overrideNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);
        props.getProperty("attr2").setValue("foo");

        Reactive.flush();

        assertEquals("value1", element.getAttribute("attr1"));
        assertEquals("foo", element.getAttribute("attr2"));
    }

    public void testChildSlot() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        TestTemplateNode childSlot = TestTemplateNode
                .create(ChildSlotNode.TYPE);

        int childId = 67;
        registry.getTemplateRegistry().register(childId, childSlot);

        templateNode.setChildrenIds(new double[] { childId });

        Element element = createElement(templateNode);

        Reactive.flush();

        assertEquals(1, DomApi.wrap(element).getChildNodes().length());
        assertEquals(Node.COMMENT_NODE,
                DomApi.wrap(element).getChildNodes().get(0).getNodeType());

        StateNode childContentNode = new StateNode(79, stateNode.getTree());

        childContentNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.TAG).setValue("span");

        stateNode.getMap(NodeFeatures.TEMPLATE)
                .getProperty(TemplateMap.CHILD_SLOT_CONTENT)
                .setValue(childContentNode);

        Reactive.flush();

        assertEquals(2, element.getChildNodes().getLength());
        assertEquals(Node.COMMENT_NODE,
                element.getChildNodes().item(0).getNodeType());
        assertEquals(Node.ELEMENT_NODE,
                element.getChildNodes().item(1).getNodeType());
        assertEquals("SPAN",
                DomApi.wrap(element).getLastElementChild().getTagName());

        stateNode.getMap(NodeFeatures.TEMPLATE)
                .getProperty(TemplateMap.CHILD_SLOT_CONTENT).setValue(null);

        Reactive.flush();

        assertEquals(1, element.getChildNodes().getLength());
        assertEquals(Node.COMMENT_NODE,
                element.getChildNodes().item(0).getNodeType());
    }

    public void testChildSlotAfterUnregister() {
        StateNode childContentNode = new StateNode(79, stateNode.getTree());

        // Must register so that we can fire an unregister event later on
        tree.registerNode(stateNode);
        tree.registerNode(childContentNode);

        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        TestTemplateNode childSlot = TestTemplateNode
                .create(ChildSlotNode.TYPE);

        int childId = 67;
        registry.getTemplateRegistry().register(childId, childSlot);

        templateNode.setChildrenIds(new double[] { childId });

        childContentNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.TAG).setValue("span");

        stateNode.getMap(NodeFeatures.TEMPLATE)
                .getProperty(TemplateMap.CHILD_SLOT_CONTENT)
                .setValue(childContentNode);

        Reactive.flush();

        Element element = createElement(templateNode);

        assertEquals(1, element.getChildNodes().getLength());

        tree.unregisterNode(stateNode);
        tree.unregisterNode(childContentNode);

        Reactive.flush();

        stateNode.getMap(NodeFeatures.TEMPLATE)
                .getProperty(TemplateMap.CHILD_SLOT_CONTENT).setValue(null);

        Reactive.flush();

        // Emptying child slot should have not effect when node is unregistered
        assertEquals(1, element.getChildNodes().getLength());
    }

    public void testRemoveTemplateFromDom() {
        int templateNodeId = 1;

        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("span");

        StateNode templateState = new StateNode(1, stateNode.getTree());
        registerTemplateNode(templateNode, templateState, templateNodeId);

        StateNode simpleNode = new StateNode(2, tree);
        simpleNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.TAG).setValue("div");
        simpleNode.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, templateState);

        Element element = new SimpleElementBindingStrategy().create(simpleNode);
        Binder.bind(simpleNode, element);

        Reactive.flush();

        assertEquals(1, element.getChildElementCount());
        assertEquals("SPAN", element.getFirstElementChild().getTagName());

        simpleNode.getList(NodeFeatures.ELEMENT_CHILDREN).splice(0, 1);

        Reactive.flush();

        assertEquals(0, element.getChildElementCount());
    }

    public void testEventHandler() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addEventHandler("click", "$element.id='foo'");

        Element element = createElement(templateNode);
        MouseEvent event = (MouseEvent) Browser.getDocument()
                .createEvent(Events.MOUSE);
        event.initMouseEvent("click", true, true, Browser.getWindow(), 0, 0, 0,
                0, 0, false, false, false, false, 0, element);

        Browser.getDocument().getBody().appendChild(element);

        element.dispatchEvent(event);
        assertEquals("foo", element.getAttribute("id"));
    }

    public void testClientDelegateHandler_noArgs() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        String operation = "operation";
        templateNode.addEventHandler("click", "$server." + operation + "()");

        stateNode.getList(NodeFeatures.CLIENT_DELEGATE_HANDLERS).set(0,
                operation);

        Element element = createElement(templateNode);
        MouseEvent event = (MouseEvent) Browser.getDocument()
                .createEvent(Events.MOUSE);
        event.initMouseEvent("click", true, true, Browser.getWindow(), 0, 0, 0,
                0, 0, false, false, false, false, 0, element);

        Browser.getDocument().getBody().appendChild(element);

        element.dispatchEvent(event);

        assertEquals(1, serverMethods.size());
        JsArray<?> args = serverMethods.get(operation);
        assertNotNull(args);
        assertEquals(0, args.length());
    }

    public void testClientDelegateHandler_args() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        String operation = "operation";
        templateNode.addEventHandler("click", "$server." + operation
                + "($wnd.booleanprop, $wnd.stringprop, $wnd.numberprop, $wnd.objectprop)");

        stateNode.getList(NodeFeatures.CLIENT_DELEGATE_HANDLERS).set(0,
                operation);

        Element element = createElement(templateNode);
        WidgetUtil.setJsProperty(Browser.getWindow(), "booleanprop", true);
        WidgetUtil.setJsProperty(Browser.getWindow(), "stringprop", "foo");
        WidgetUtil.setJsProperty(Browser.getWindow(), "numberprop", 11);
        JsonObject obj = Json.createObject();
        obj.put("foo", "bar");
        WidgetUtil.setJsProperty(Browser.getWindow(), "objectprop", obj);

        MouseEvent event = (MouseEvent) Browser.getDocument()
                .createEvent(Events.MOUSE);
        event.initMouseEvent("click", true, true, Browser.getWindow(), 0, 0, 0,
                0, 0, false, false, false, false, 0, element);

        Browser.getDocument().getBody().appendChild(element);

        element.dispatchEvent(event);

        assertEquals(1, serverMethods.size());
        JsArray<?> args = serverMethods.get(operation);
        assertNotNull(args);
        assertEquals(4, args.length());
        assertEquals(true, args.get(0));
        assertEquals("foo", args.get(1));
        assertEquals(11, args.get(2));
        assertTrue(args.get(3) instanceof JsonObject);
        assertEquals(obj, args.get(3));
    }

    public void testNgFor() {
        TestElementTemplateNode parent = TestElementTemplateNode.create("div");
        String textVar = "text";
        // create 3 children for the parent: <div/><li
        // *ngFor>{{text}}</li><span/>
        StateNode modelNode = createNgForModelNode(parent, "div", "li", "span",
                "items", textVar);

        StateNode varNode = new StateNode(2, tree);
        varNode.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("foo");

        modelNode.getList(NodeFeatures.TEMPLATE_MODELLIST).add(0, varNode);

        Element element = createElement(parent);

        Reactive.flush();

        assertEquals("DIV", element.getTagName());
        JsArray<Node> childNodes = DomApi.wrap(element).getChildNodes();
        assertTrue(childNodes.length() > 1);
        assertEquals("DIV", ((Element) childNodes.get(0)).getTagName());
        assertEquals("SPAN", ((Element) childNodes.get(childNodes.length() - 1))
                .getTagName());

        Element li = ((Element) childNodes.get(childNodes.length() - 2));
        assertEquals("LI", li.getTagName());
        assertEquals(4, childNodes.length());
        // comment
        assertEquals(Node.COMMENT_NODE, childNodes.get(1).getNodeType());

        assertEquals("foo", li.getTextContent());
    }

    public void testServerEventHandlerInNgFor() {
        TestElementTemplateNode parent = TestElementTemplateNode.create("div");
        String operation = "operation";

        // ============= create <li *ngFor> ===============================
        String collectionVar = "items";

        TestForTemplateNode templateNode = TestTemplateNode
                .create(ForTemplateNode.TYPE);

        int templateId = 42;
        registry.getTemplateRegistry().register(templateId, templateNode);

        TestElementTemplateNode forChild = TestElementTemplateNode.create("li");
        templateNode.setCollectionVariable(collectionVar);

        forChild.addEventHandler("click", "$server." + operation + "()");

        int forChildId = 11;
        registry.getTemplateRegistry().register(forChildId, forChild);

        templateNode.setChildrenIds(new double[] { forChildId });

        parent.setChildrenIds(new double[] { templateId });

        NodeMap model = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        MapProperty property = model.getProperty(collectionVar);
        StateNode modelNode = new StateNode(1, tree);

        property.setValue(modelNode);
        // ================== now modelNode is NG FOR model node ==========

        StateNode varNode = new StateNode(2, tree);

        com.vaadin.client.flow.nodefeature.NodeList modelList = modelNode
                .getList(NodeFeatures.TEMPLATE_MODELLIST);
        // add one item to the "collection" model
        modelList.add(0, varNode);

        stateNode.getList(NodeFeatures.CLIENT_DELEGATE_HANDLERS).set(0,
                operation);

        Element element = createElement(parent);

        Reactive.flush();

        MouseEvent event = (MouseEvent) Browser.getDocument()
                .createEvent(Events.MOUSE);
        event.initMouseEvent("click", true, true, Browser.getWindow(), 0, 0, 0,
                0, 0, false, false, false, false, 0, element);

        Browser.getDocument().getBody().appendChild(element);

        element.getElementsByTagName("li").item(0).dispatchEvent(event);

        assertEquals(1, serverMethods.size());
        assertNotNull(serverMethods.get(operation));
        assertEquals(stateNode.getId(), serverRpcNodes.get(operation).getId());
    }

    public void testNgFor_unregister_noUpdates() {
        TestElementTemplateNode parent = TestElementTemplateNode.create("div");
        String textVar = "text";
        // create 3 children for the parent: <div/><li
        // *ngFor>{{text}}</li><span/>
        StateNode modelNode = createNgForModelNode(parent, "div", "li", "span",
                "items", textVar);

        StateNode varNode = new StateNode(2, tree);
        varNode.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("foo");

        com.vaadin.client.flow.nodefeature.NodeList modelList = modelNode
                .getList(NodeFeatures.TEMPLATE_MODELLIST);
        modelList.add(0, varNode);

        Element element = createElement(parent);

        Reactive.flush();

        String htmlBeforeUregister = element.getOuterHTML();

        stateNode.unregister();

        Reactive.flush();

        StateNode varNode1 = new StateNode(3, tree);
        varNode1.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("bar");

        modelList.add(1, varNode);

        Reactive.flush();

        assertEquals(htmlBeforeUregister, element.getOuterHTML());
    }

    public void testNgFor_updateModelValues() {
        TestElementTemplateNode parent = TestElementTemplateNode.create("div");
        String textVar = "text";
        // create 3 children for the parent: <div/><li
        // *ngFor>{{text}}</li><span/>
        StateNode modelNode = createNgForModelNode(parent, "div", "li", "span",
                "items", textVar);

        StateNode varNode = new StateNode(2, tree);
        varNode.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("foo");

        com.vaadin.client.flow.nodefeature.NodeList modelList = modelNode
                .getList(NodeFeatures.TEMPLATE_MODELLIST);
        modelList.add(0, varNode);

        Element element = createElement(parent);

        Reactive.flush();

        StateNode varNode1 = new StateNode(3, tree);
        varNode1.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("bar");

        StateNode varNode2 = new StateNode(4, tree);
        varNode2.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("bar1");

        modelList.splice(0, 1);
        modelList.add(0, varNode1);
        modelList.add(1, varNode2);

        Reactive.flush();

        assertEquals("DIV", element.getTagName());
        NodeList childNodes = element.getChildNodes();
        assertTrue(childNodes.getLength() > 1);
        assertEquals("DIV", ((Element) childNodes.item(0)).getTagName());
        assertEquals("SPAN",
                ((Element) childNodes.item(childNodes.getLength() - 1))
                        .getTagName());

        Element li = ((Element) childNodes.item(childNodes.getLength() - 3));
        assertEquals("LI", li.getTagName());
        assertEquals(5, childNodes.getLength());
        // comment
        assertEquals(Node.COMMENT_NODE, childNodes.item(1).getNodeType());

        assertEquals("bar", li.getTextContent());

        li = ((Element) childNodes.item(childNodes.getLength() - 2));
        assertEquals("LI", li.getTagName());
        assertEquals("bar1", li.getTextContent());
    }

    public void testNgFor_updateModel() {
        TestElementTemplateNode parent = TestElementTemplateNode.create("div");
        String textVar = "text";
        String collectionVar = "items";
        // create 3 children for the parent: <div/><li
        // *ngFor>{{text}}</li><span/>
        StateNode modelNode = createNgForModelNode(parent, "div", "li", "span",
                collectionVar, textVar);

        StateNode varNode = new StateNode(2, tree);
        varNode.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("foo");

        modelNode.getList(NodeFeatures.TEMPLATE_MODELLIST).add(0, varNode);

        Element element = createElement(parent);

        Reactive.flush();

        NodeMap model = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        MapProperty property = model.getProperty(collectionVar);
        modelNode = new StateNode(3, tree);

        property.setValue(modelNode);

        varNode = new StateNode(4, tree);
        varNode.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("bar");

        modelNode.getList(NodeFeatures.TEMPLATE_MODELLIST).add(0, varNode);

        Reactive.flush();

        assertEquals("DIV", element.getTagName());
        NodeList childNodes = element.getChildNodes();
        assertTrue(childNodes.getLength() > 1);
        assertEquals("DIV", ((Element) childNodes.item(0)).getTagName());
        assertEquals("SPAN",
                ((Element) childNodes.item(childNodes.getLength() - 1))
                        .getTagName());

        Element li = ((Element) childNodes.item(childNodes.getLength() - 2));
        assertEquals("LI", li.getTagName());
        assertEquals(4, childNodes.getLength());
        // comment
        assertEquals(Node.COMMENT_NODE, childNodes.item(1).getNodeType());

        assertEquals("bar", li.getTextContent());
    }

    public void testNgFor_notRecreate() {
        TestElementTemplateNode parent = TestElementTemplateNode.create("div");
        String textVar = "text";
        String collectionVar = "items";
        // create 3 children for the parent: <div/><li
        // *ngFor>{{text}}</li><span/>
        StateNode modelNode = createNgForModelNode(parent, "div", "li", "span",
                collectionVar, textVar);

        StateNode childNode1 = new StateNode(2, tree);
        childNode1.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("foo");

        modelNode.getList(NodeFeatures.TEMPLATE_MODELLIST).add(0, childNode1);

        Element parentElement = createElement(parent);

        Reactive.flush();

        Element firstLi = parentElement.querySelector("li");
        assertEquals("LI", firstLi.getTagName());

        firstLi.setAttribute("class", "custom");

        StateNode childNode2 = new StateNode(2, tree);
        childNode2.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(textVar)
                .setValue("foo");

        modelNode.getList(NodeFeatures.TEMPLATE_MODELLIST).add(1, childNode2);

        Reactive.flush();

        // Original DOM element should not have been recreated
        firstLi = parentElement.querySelector("li");
        assertEquals("LI", firstLi.getTagName());
        assertEquals("custom", firstLi.getAttribute("class"));

        // Remove one item from index 1
        modelNode.getList(NodeFeatures.TEMPLATE_MODELLIST).splice(1, 1);

        Reactive.flush();

        // Original DOM element should not have been recreated
        firstLi = parentElement.querySelector("li");
        assertEquals("LI", firstLi.getTagName());
        assertEquals("custom", firstLi.getAttribute("class"));
    }

    public void testJSExpressionInBinding() {
        // create binding with expression : : key ? key+'@bar.com' :'foo'
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createTextValueBinding(
                        MODEL_KEY + " ? " + MODEL_KEY + "+'@bar.com' : 'foo'"));
        Node domNode = createText(templateNode);

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        map.getProperty(MODEL_KEY).setValue(null);

        Reactive.flush();

        assertEquals("foo", domNode.getTextContent());

        map.getProperty(MODEL_KEY).setValue("value");

        Reactive.flush();

        assertEquals("value@bar.com", domNode.getTextContent());
    }

    public void testJSExpresisonsWithSubProperties() {
        JsonObject beanType = Json.createObject();
        beanType.put(MODEL_KEY, "String");

        JsonObject modelType = Json.createObject();
        modelType.put(MODEL_KEY, beanType);

        setModelType(stateNode, modelType);

        // create binding with expression : : key.key ? key.key+'@bar.com'
        // :'foo'
        TestTextTemplate templateNode = TestTextTemplate.create(TestBinding
                .createTextValueBinding(MODEL_KEY + "." + MODEL_KEY + " ? "
                        + MODEL_KEY + "." + MODEL_KEY + "+'@bar.com' : 'foo'"));

        Node domNode = createText(templateNode);

        NodeMap parentMap = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        StateNode childNode = new StateNode(458, stateNode.getTree());
        NodeMap childMap = childNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        parentMap.getProperty(MODEL_KEY).setValue(childNode);

        childMap.getProperty(MODEL_KEY).setValue(null);

        Reactive.flush();

        assertEquals("foo", domNode.getTextContent());

        childMap.getProperty(MODEL_KEY).setValue("value");

        Reactive.flush();

        assertEquals("value@bar.com", domNode.getTextContent());

    }

    /**
     * Creates 3 children for the {@code parent}: the first one is defined via
     * the {@code firstChildTag}, the second is defined via the {@code ngForTag}
     * (this is the template which represents *ngFor) and the
     * {@code lastChildTag} defines the third child.
     * <p>
     * {@code collectionVar} is an outer scope collection variable name and
     * {@code textVar} is text template parameter name. It's used inside *ngFor
     * template.
     * <p>
     * So the result is:
     *
     * <pre>
     *                                 parent
     *                                   |
     *              ________________________________________________________
     *              |                          |                           |
     *      <firstChildTag>    <ngForTag>{{textVar}}</ngForTag>     <lastChildTag>
     * </pre>
     */
    private StateNode createNgForModelNode(TestElementTemplateNode parent,
            String firstChildTag, String ngForTag, String lastChildTag,
            String collectionVar, String textVar) {
        TestElementTemplateNode child1 = TestElementTemplateNode
                .create(firstChildTag);
        int child1Id = 57;
        registry.getTemplateRegistry().register(child1Id, child1);

        TestForTemplateNode templateNode = TestTemplateNode
                .create(ForTemplateNode.TYPE);

        int templateId = 42;
        registry.getTemplateRegistry().register(templateId, templateNode);

        TestElementTemplateNode forChild = TestElementTemplateNode
                .create(ngForTag);
        templateNode.setCollectionVariable(collectionVar);

        TestTextTemplate text = TestTextTemplate
                .create(TestBinding.createTextValueBinding(textVar));
        int textChildId = 85;
        registry.getTemplateRegistry().register(textChildId, text);
        forChild.setChildrenIds(new double[] { textChildId });

        int forChildId = 11;
        registry.getTemplateRegistry().register(forChildId, forChild);

        templateNode.setChildrenIds(new double[] { forChildId });

        TestElementTemplateNode child2 = TestElementTemplateNode
                .create(lastChildTag);
        int child2Id = 84;
        registry.getTemplateRegistry().register(child2Id, child2);
        parent.setChildrenIds(new double[] { child1Id, templateId, child2Id });

        NodeMap model = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        MapProperty property = model.getProperty(collectionVar);
        StateNode modelNode = new StateNode(1, tree);

        property.setValue(modelNode);

        return modelNode;
    }

    private StateNode createSimpleOverrideNode(int mainNodeTemplateId) {
        return createSimpleOverrideNode(mainNodeTemplateId, 1);
    }

    private StateNode createSimpleOverrideNode(int mainNodeTemplateId,
            int overrideNodeId) {
        StateNode overrideNode = new StateNode(overrideNodeId, tree);
        // make it recognizable as an override element
        overrideNode.getMap(NodeFeatures.OVERRIDE_DATA);

        stateNode.getMap(NodeFeatures.TEMPLATE_OVERRIDES)
                .getProperty(String.valueOf(mainNodeTemplateId))
                .setValue(overrideNode);

        return overrideNode;
    }

    private Text createText(TestTextTemplate templateNode) {
        registerTemplateNode(templateNode);
        Text text = new TextTemplateBindingStrategy().create(stateNode);
        Binder.bind(stateNode, text);
        return text;
    }

    private Element createElement(TestElementTemplateNode templateNode,
            int id) {
        registerTemplateNode(templateNode, stateNode, id);
        Element element = new ElementTemplateBindingStrategy()
                .create(stateNode);
        Binder.bind(stateNode, element);
        return element;
    }

    private Element createElement(TestElementTemplateNode templateNode) {
        registerTemplateNode(templateNode);
        Element element = new ElementTemplateBindingStrategy()
                .create(stateNode);
        stateNode.setDomNode(element);

        Binder.bind(stateNode, element);
        return element;
    }

    private void registerTemplateNode(TestTemplateNode templateNode,
            StateNode node, int id) {
        registry.getTemplateRegistry().register(id, templateNode);
        node.getMap(NodeFeatures.TEMPLATE)
                .getProperty(NodeProperties.ROOT_TEMPLATE_ID)
                .setValue(Double.valueOf(id));
    }

    private void registerTemplateNode(TestTemplateNode templateNode) {
        registerTemplateNode(templateNode, stateNode, 1);
    }

    private static void setModelType(StateNode node, JsonObject modelTypeJson) {
        String constantId = "modelType " + modelTypeCount++;

        JsonObject constantsJson = Json.createObject();
        constantsJson.put(constantId, modelTypeJson);

        ConstantPool constantPool = node.getTree().getRegistry()
                .getConstantPool();
        constantPool.importFromJson(constantsJson);

        node.getMap(NodeFeatures.TEMPLATE)
                .getProperty(NodeProperties.MODEL_DESCRIPTOR)
                .setValue(constantId);
    }
}
