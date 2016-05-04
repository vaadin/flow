/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird.template;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.ElementBinder;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.nodefeature.MapProperty;
import com.vaadin.client.hummingbird.nodefeature.NodeMap;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.shared.NodeFeatures;
import com.vaadin.hummingbird.template.ChildSlotNode;
import com.vaadin.hummingbird.template.ModelValueBindingProvider;

import elemental.dom.Element;
import elemental.dom.Node;

public class GwtTemplateBinderTest extends ClientEngineTestBase {
    private Registry registry = new Registry() {
        {
            set(TemplateRegistry.class, new TemplateRegistry());
        }
    };
    private StateTree tree = new StateTree(registry);
    private StateNode stateNode = new StateNode(0, tree);

    public void testTemplateProperties() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop1", "value1");
        templateNode.addProperty("prop2", "value2");

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, templateNode);

        assertEquals("value1", WidgetUtil.getJsProperty(element, "prop1"));
        assertEquals("value2", WidgetUtil.getJsProperty(element, "prop2"));
    }

    public void testTemplateAttributes() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addAttribute("attr1", "value1");
        templateNode.addAttribute("attr2", "value2");

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, templateNode);

        assertEquals("value1", element.getAttribute("attr1"));
        assertEquals("value2", element.getAttribute("attr2"));
    }

    public void testTemplateTag() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, templateNode);

        assertEquals("DIV", element.getTagName());
    }

    public void testTemplateChildren() {
        final int childId = 2345;
        TestElementTemplateNode childTemplate = TestElementTemplateNode
                .create("span");
        registry.getTemplateRegistry().register(childId, childTemplate);

        TestElementTemplateNode parentTemplate = TestElementTemplateNode
                .create("div");
        parentTemplate.setChildren(new double[] { childId });

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, parentTemplate);

        assertEquals(1, element.getChildElementCount());
        assertEquals("SPAN", element.getFirstElementChild().getTagName());
    }

    public void testTemplateText() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createStatic("text"));
        Node domNode = TemplateElementBinder.createAndBind(stateNode,
                templateNode);
        assertEquals("text", domNode.getTextContent());
    }

    public void testRegisteredTemplate() {
        final int templateId = 43;
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        registry.getTemplateRegistry().register(templateId, templateNode);

        stateNode.getMap(NodeFeatures.TEMPLATE)
                .getProperty(NodeFeatures.ROOT_TEMPLATE_ID)
                .setValue(Double.valueOf(templateId));

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode);

        assertEquals("DIV", element.getTagName());
    }

    public void testPropertyBindingTemplate() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop",
                TestBinding.createBinding(ModelValueBindingProvider.TYPE, "key"));

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODEL);
        map.getProperty("key").setValue("foo");
        Node domNode = TemplateElementBinder.createAndBind(stateNode,
                templateNode);

        Reactive.flush();

        assertEquals("foo", WidgetUtil.getJsProperty(domNode, "prop"));
    }

    public void testUpdatePropertyBindingTemplate() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop",
                TestBinding.createBinding(ModelValueBindingProvider.TYPE, "key"));

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODEL);
        map.getProperty("key").setValue("foo");
        Node domNode = TemplateElementBinder.createAndBind(stateNode,
                templateNode);

        Reactive.flush();

        map.getProperty("key").setValue("bar");

        Reactive.flush();

        assertEquals("bar", WidgetUtil.getJsProperty(domNode, "prop"));
    }

    public void testUnregister_propeprtyBindingUpdateIsNotDone() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop",
                TestBinding.createBinding(ModelValueBindingProvider.TYPE, "key"));

        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODEL);
        map.getProperty("key").setValue("foo");
        Node domNode = TemplateElementBinder.createAndBind(stateNode,
                templateNode);

        assertEquals(null, WidgetUtil.getJsProperty(domNode, "prop"));

        stateNode.unregister();

        Reactive.flush();
        assertEquals(null, WidgetUtil.getJsProperty(domNode, "prop"));
    }

    public void testPropertyBindingNoValueTemplate() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.addProperty("prop",
                TestBinding.createBinding(ModelValueBindingProvider.TYPE, "key"));
        Node domNode = TemplateElementBinder.createAndBind(stateNode,
                templateNode);

        Reactive.flush();

        assertEquals(null, WidgetUtil.getJsProperty(domNode, "prop"));
    }

    public void testTextValueTemplate() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createTextValueBinding("key"));
        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODEL);
        map.getProperty("key").setValue("foo");
        Node domNode = TemplateElementBinder.createAndBind(stateNode,
                templateNode);

        Reactive.flush();

        assertEquals("foo", domNode.getTextContent());
    }

    public void testUpdateTextValueTemplate() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createTextValueBinding("key"));
        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODEL);
        map.getProperty("key").setValue("foo");
        Node domNode = TemplateElementBinder.createAndBind(stateNode,
                templateNode);

        Reactive.flush();

        map.getProperty("key").setValue("bar");

        Reactive.flush();

        assertEquals("bar", domNode.getTextContent());
    }

    public void testUnregister_textBinsingUpdateIsNotDone() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createTextValueBinding("key"));
        NodeMap map = stateNode.getMap(NodeFeatures.TEMPLATE_MODEL);
        map.getProperty("key").setValue("foo");
        Node domNode = TemplateElementBinder.createAndBind(stateNode,
                templateNode);

        assertEquals("", domNode.getTextContent());

        stateNode.unregister();

        Reactive.flush();
        assertEquals("", domNode.getTextContent());
    }

    public void testTextNoValueTemplate() {
        TestTextTemplate templateNode = TestTextTemplate
                .create(TestBinding.createTextValueBinding("key"));
        Node domNode = TemplateElementBinder.createAndBind(stateNode,
                templateNode);

        Reactive.flush();

        assertEquals("", domNode.getTextContent());
    }

    public void testBindOverrideNodeWhenCreated() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.setId(Double.valueOf(83));

        StateNode overrideNode = new StateNode(1, tree);
        overrideNode.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty("id")
                .setValue("override");

        stateNode.getMap(NodeFeatures.TEMPLATE_OVERRIDES)
                .getProperty(templateNode.getId().toString())
                .setValue(overrideNode);

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, templateNode);

        Reactive.flush();

        assertEquals("override", element.getId());
    }

    public void testBindOverrideNodeAfterCreated() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.setId(Double.valueOf(83));

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, templateNode);

        Reactive.flush();

        StateNode overrideNode = new StateNode(1, tree);
        overrideNode.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty("id")
                .setValue("override");

        stateNode.getMap(NodeFeatures.TEMPLATE_OVERRIDES)
                .getProperty(templateNode.getId().toString())
                .setValue(overrideNode);

        Reactive.flush();

        assertEquals("override", element.getId());
    }

    public void testUnregisterOverrideNode() {
        StateNode overrideNode = new StateNode(2, tree);

        // Must register so that we can fire an unregister event later on
        tree.registerNode(stateNode);
        tree.registerNode(overrideNode);

        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        templateNode.setId(Double.valueOf(83));

        MapProperty idProperty = overrideNode
                .getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty("id");
        idProperty.setValue("override");

        stateNode.getMap(NodeFeatures.TEMPLATE_OVERRIDES)
                .getProperty(templateNode.getId().toString())
                .setValue(overrideNode);

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, templateNode);

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

    public void testChildSlot() {
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("div");
        TestTemplateNode childSlot = TestTemplateNode
                .create(ChildSlotNode.TYPE);

        int childId = 67;
        registry.getTemplateRegistry().register(childId, childSlot);

        templateNode.setChildren(new double[] { childId });

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, templateNode);

        Reactive.flush();

        assertEquals(1, element.getChildNodes().getLength());
        assertEquals(Node.COMMENT_NODE,
                element.getChildNodes().item(0).getNodeType());

        StateNode childContentNode = new StateNode(79, stateNode.getTree());
        childContentNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeFeatures.TAG).setValue("span");

        stateNode.getMap(NodeFeatures.TEMPLATE)
                .getProperty(TemplateMap.CHILD_SLOT_CONTENT)
                .setValue(childContentNode);

        Reactive.flush();

        assertEquals(2, element.getChildNodes().getLength());
        assertEquals(Node.COMMENT_NODE,
                element.getChildNodes().item(0).getNodeType());
        assertEquals(Node.ELEMENT_NODE,
                element.getChildNodes().item(1).getNodeType());
        assertEquals("SPAN", element.getLastElementChild().getTagName());

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

        templateNode.setChildren(new double[] { childId });

        childContentNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeFeatures.TAG).setValue("span");

        stateNode.getMap(NodeFeatures.TEMPLATE)
                .getProperty(TemplateMap.CHILD_SLOT_CONTENT)
                .setValue(childContentNode);

        Reactive.flush();

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, templateNode);

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

        registry.getTemplateRegistry().register(templateNodeId, templateNode);

        StateNode templateState = new StateNode(1, stateNode.getTree());
        templateState.getMap(NodeFeatures.TEMPLATE)
                .getProperty(NodeFeatures.ROOT_TEMPLATE_ID)
                .setValue(Double.valueOf(templateNodeId));

        stateNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeFeatures.TAG).setValue("div");
        stateNode.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, templateState);

        Element element = (Element) ElementBinder.createAndBind(stateNode);

        Reactive.flush();

        assertEquals(1, element.getChildElementCount());
        assertEquals("SPAN", element.getFirstElementChild().getTagName());

        stateNode.getList(NodeFeatures.ELEMENT_CHILDREN).splice(0, 1);

        Reactive.flush();

        assertEquals(0, element.getChildElementCount());
    }
}
