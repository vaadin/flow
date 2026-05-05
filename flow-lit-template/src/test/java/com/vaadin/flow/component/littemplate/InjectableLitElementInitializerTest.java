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
package com.vaadin.flow.component.littemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InjectableLitElementInitializerTest {

    private Element element = ElementFactory.createDiv();

    private InjectableLitElementInitializer initializer = new InjectableLitElementInitializer(
            element, Component.class);

    @Test
    void initializeElement_setId_idIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("id", "foo"));

        assertEquals("foo", element.getAttribute("id"));
    }

    @Test
    void initializeElement_setHref_hrefIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("href", "foo"));

        assertEquals("foo", element.getAttribute("href"));
    }

    @Test
    void initializeElement_setTheme_themeIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("theme", "foo"));

        assertEquals("foo", element.getAttribute("theme"));
    }

    @Test
    void initializeElement_setAttributeBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("?class", "foo"));

        assertFalse(element.hasAttribute("class"));
        assertFalse(element.hasAttribute("?class"));
        assertFalse(element.hasProperty("class"));
        assertFalse(element.hasProperty("?class"));
    }

    @Test
    void initializeElement_setPropertyBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap(".foo", "bar"));

        assertFalse(element.hasAttribute("foo"));
        assertFalse(element.hasProperty("foo"));
    }

    @Test
    void initializeElement_setDynamicValue_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("foo", "${bar}"));

        assertFalse(element.hasAttribute("foo"));
        assertFalse(element.hasProperty("foo"));
    }

    @Test
    void initializeElement_setOtherTemplateAttribute_attributeIsSetAsProperty() {
        initializer.accept(Collections.singletonMap("foo", "bar"));

        assertEquals("bar", element.getProperty("foo"));
        assertNull(element.getAttribute("foo"));
    }

    @Test
    void initializeElement_setClass_classIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("class", "foo bar"));

        assertEquals("foo bar", element.getAttribute("class"));

        TestComponent comp = new TestComponent(element);
        assertEquals("foo bar", comp.getClassName());
        assertTrue(comp.getClassNames().contains("foo"));
    }

    @Test
    void initializeElement_setStyle_styleIsSetAsAttribute() {
        initializer.accept(
                Collections.singletonMap("style", "width:100px;height:50px"));

        assertEquals("width:100px;height:50px", element.getAttribute("style"));

        TestComponent comp = new TestComponent(element);
        assertEquals("100px", comp.getStyle().get("width"));
        assertEquals("50px", comp.getStyle().get("height"));
    }

    @Test
    void initializeElement_disabled_exceptionIsThrown() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("disabled", Boolean.TRUE.toString());
        attributes.put("id", "labelId");

        IllegalAttributeException ex = assertThrows(
                IllegalAttributeException.class,
                () -> initializer.accept(attributes));
        assertTrue(ex.getMessage().contains("element 'div' with id 'labelId'"));
    }

    @Tag(Tag.DIV)
    public static class TestComponent extends Component implements HasStyle {

        TestComponent(Element element) {
            super(element);
        }
    }
}
