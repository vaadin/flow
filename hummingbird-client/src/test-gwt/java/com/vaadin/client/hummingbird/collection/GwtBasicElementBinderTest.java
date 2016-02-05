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
package com.vaadin.client.hummingbird.collection;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.BasicElementBinder;
import com.vaadin.client.hummingbird.MapNamespace;
import com.vaadin.client.hummingbird.MapProperty;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.shared.Namespaces;

import elemental.client.Browser;
import elemental.dom.Element;

public class GwtBasicElementBinderTest extends ClientEngineTestBase {
    private StateNode node = new StateTree().getRootNode();

    private MapNamespace properties = node
            .getMapNamespace(Namespaces.ELEMENT_PROPERTIES);
    private MapNamespace attributes = node
            .getMapNamespace(Namespaces.ELEMENT_ATTRIBUTES);
    private MapNamespace elementData = node
            .getMapNamespace(Namespaces.ELEMENT_DATA);

    private MapProperty titleProperty = properties.getProperty("title");
    private MapProperty idAttribute = attributes.getProperty("id");

    private Element element;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        element = Browser.getDocument().createElement("div");
    }

    public void testBindExistingProperty() {
        titleProperty.setValue("foo");

        BasicElementBinder.bind(node, element);

        Reactive.flush();

        assertEquals("foo", element.getTitle());
    }

    public void testBindNewProperty() {
        BasicElementBinder.bind(node, element);

        properties.getProperty("lang").setValue("foo");

        Reactive.flush();

        assertEquals("foo", element.getLang());
    }

    public void testBindingBeforeFlush() {
        titleProperty.setValue("foo");

        BasicElementBinder.bind(node, element);

        assertEquals("", element.getTitle());
    }

    public void testUnbindBeforeFlush() {
        BasicElementBinder binder = BasicElementBinder.bind(node, element);

        titleProperty.setValue("foo");
        idAttribute.setValue("foo");

        binder.remove();

        titleProperty.setValue("bar");
        idAttribute.setValue("bar");

        Reactive.flush();

        assertEquals("", element.getTitle());
        assertEquals("", element.getId());
    }

    public void testUnbindAfterFlush() {
        BasicElementBinder binder = BasicElementBinder.bind(node, element);

        titleProperty.setValue("foo");
        idAttribute.setValue("foo");

        Reactive.flush();

        binder.remove();

        titleProperty.setValue("bar");
        idAttribute.setValue("bar");

        Reactive.flush();

        assertEquals("foo", element.getTitle());
        assertEquals("foo", element.getId());
    }

    public void testRemoveArbitraryProperty() {
        MapProperty foo = properties.getProperty("foo");
        foo.setValue("bar");

        BasicElementBinder.bind(node, element);

        Reactive.flush();

        assertTrue(WidgetUtil.hasOwnJsProperty(element, "foo"));

        foo.removeValue();

        Reactive.flush();

        assertFalse(WidgetUtil.hasOwnJsProperty(element, "foo"));
    }

    public void testRemoveBuiltInProperty() {
        titleProperty.setValue("foo");

        BasicElementBinder.bind(node, element);

        Reactive.flush();

        titleProperty.removeValue();

        Reactive.flush();

        // Properties inherited from e.g. Element can't be removed
        // Assigning null to title produces "null"
        assertEquals("null", element.getTitle());
    }

    public void testBindWrongTagThrows() {
        elementData.getProperty(Namespaces.TAG).setValue("span");

        try {
            BasicElementBinder.bind(node, element);
            fail("Should have thrown");
        } catch (AssertionError expected) {
        }
    }

    public void testBindRightTagOk() {
        elementData.getProperty(Namespaces.TAG).setValue("div");

        BasicElementBinder.bind(node, element);
    }

    public void testBindExistingAttribute() {
        idAttribute.setValue("foo");

        BasicElementBinder.bind(node, element);

        Reactive.flush();

        assertEquals("foo", element.getId());
    }

    public void testBindNewAttribute() {
        BasicElementBinder.bind(node, element);

        attributes.getProperty("lang").setValue("foo");

        Reactive.flush();

        assertEquals("foo", element.getLang());
    }

    public void testSetAttributeWithoutFlush() {
        idAttribute.setValue("foo");

        BasicElementBinder.bind(node, element);

        assertEquals("", element.getId());
    }

    public void restRemoveAttribute() {
        BasicElementBinder.bind(node, element);

        idAttribute.setValue("foo");

        Reactive.flush();

        idAttribute.removeValue();

        Reactive.flush();

        assertEquals(null, element.getId());
    }
}
