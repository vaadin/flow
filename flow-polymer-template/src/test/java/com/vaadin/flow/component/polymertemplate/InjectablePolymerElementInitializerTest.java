/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InjectablePolymerElementInitializerTest {

    private Element element = ElementFactory.createDiv();

    private InjectablePolymerElementInitializer initializer = new InjectablePolymerElementInitializer(
            element, Component.class);

    @Test
    void initializeElement_setId_idIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("id", "foo"));

        assertEquals("foo", element.getAttribute("id"));
    }

    @Test
    void initializeElement_setTheme_themeIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("theme", "foo"));

        assertEquals("foo", element.getAttribute("theme"));
    }

    @Test
    void initializeElement_setHref_hrefIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("href", "foo"));

        assertEquals("foo", element.getAttribute("href"));
    }

    @Test
    void initializeElement_setAttributeBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("class$", "foo"));

        assertFalse(element.hasAttribute("class"));
        assertFalse(element.hasAttribute("class$"));
        assertFalse(element.hasProperty("class"));
        assertFalse(element.hasProperty("class$"));
    }

    @Test
    void initializeElement_setPropertyBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("foo", "[[bar]]"));

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
    void initializeElement_setPropertyTwoWayBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("foo", "{{bar}}"));

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
    void initializeElement_disabled_elementIsEnabledContainsProperty() {
        initializer.accept(
                Collections.singletonMap("disabled", Boolean.TRUE.toString()));

        assertTrue(element.isEnabled(), "Element should stay enabled");
        assertTrue(element.hasProperty("disabled"),
                "Disabled should show as a property");
    }

    @Tag(Tag.DIV)
    public static class TestComponent extends Component implements HasStyle {

        TestComponent(Element element) {
            super(element);
        }
    }
}
