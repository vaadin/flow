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
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.hummingbird.shared.Namespaces;

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

    public void testTemplateAttributes() {
        TestElementTemplateNode template = TestElementTemplateNode
                .create("div");
        template.addProperty("attr1", "value1");
        template.addProperty("attr2", "value2");

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, template);

        assertEquals("value1", WidgetUtil.getJsProperty(element, "attr1"));
        assertEquals("value2", WidgetUtil.getJsProperty(element, "attr2"));
    }

    public void testTemplateTag() {
        TestElementTemplateNode template = TestElementTemplateNode
                .create("div");

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode, template);

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
        TestTextTemplate template = TestTextTemplate
                .create(TestStaticBinding.create("text"));
        Node domNode = TemplateElementBinder.createAndBind(stateNode, template);
        assertEquals("text", domNode.getTextContent());
    }

    public void testRegisteredTemplate() {
        final int templateId = 43;
        TestElementTemplateNode template = TestElementTemplateNode
                .create("div");
        registry.getTemplateRegistry().register(templateId, template);

        stateNode.getMapNamespace(Namespaces.TEMPLATE)
                .getProperty(Namespaces.ROOT_TEMPLATE_ID)
                .setValue(Double.valueOf(templateId));

        Element element = (Element) TemplateElementBinder
                .createAndBind(stateNode);

        assertEquals("DIV", element.getTagName());
    }
}
