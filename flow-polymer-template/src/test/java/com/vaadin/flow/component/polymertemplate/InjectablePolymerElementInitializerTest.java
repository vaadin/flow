/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

public class InjectablePolymerElementInitializerTest {

    private Element element = ElementFactory.createDiv();

    private InjectablePolymerElementInitializer initializer = new InjectablePolymerElementInitializer(
            element, Component.class);

    @Test
    public void initializeElement_setId_idIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("id", "foo"));

        Assert.assertEquals("foo", element.getAttribute("id"));
    }

    @Test
    public void initializeElement_setTheme_themeIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("theme", "foo"));

        Assert.assertEquals("foo", element.getAttribute("theme"));
    }

    @Test
    public void initializeElement_setHref_hrefIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("href", "foo"));

        Assert.assertEquals("foo", element.getAttribute("href"));
    }

    @Test
    public void initializeElement_setAttributeBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("class$", "foo"));

        Assert.assertFalse(element.hasAttribute("class"));
        Assert.assertFalse(element.hasAttribute("class$"));
        Assert.assertFalse(element.hasProperty("class"));
        Assert.assertFalse(element.hasProperty("class$"));
    }

    @Test
    public void initializeElement_setPropertyBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("foo", "[[bar]]"));

        Assert.assertFalse(element.hasAttribute("foo"));
        Assert.assertFalse(element.hasProperty("foo"));
    }

    @Test
    public void initializeElement_setDynamicValue_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("foo", "${bar}"));

        Assert.assertFalse(element.hasAttribute("foo"));
        Assert.assertFalse(element.hasProperty("foo"));
    }

    @Test
    public void initializeElement_setPropertyTwoWayBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("foo", "{{bar}}"));

        Assert.assertFalse(element.hasAttribute("foo"));
        Assert.assertFalse(element.hasProperty("foo"));
    }

    @Test
    public void initializeElement_setOtherTemplateAttribute_attributeIsSetAsProperty() {
        initializer.accept(Collections.singletonMap("foo", "bar"));

        Assert.assertEquals("bar", element.getProperty("foo"));
        Assert.assertNull(element.getAttribute("foo"));
    }

    @Test
    public void initializeElement_setClass_classIsSetAsAttribute() {
        initializer.accept(Collections.singletonMap("class", "foo bar"));

        Assert.assertEquals("foo bar", element.getAttribute("class"));

        TestComponent comp = new TestComponent(element);
        Assert.assertEquals("foo bar", comp.getClassName());
        Assert.assertTrue(comp.getClassNames().contains("foo"));
    }

    @Test
    public void initializeElement_setStyle_styleIsSetAsAttribute() {
        initializer.accept(
                Collections.singletonMap("style", "width:100px;height:50px"));

        Assert.assertEquals("width:100px;height:50px",
                element.getAttribute("style"));

        TestComponent comp = new TestComponent(element);
        Assert.assertEquals("100px", comp.getStyle().get("width"));
        Assert.assertEquals("50px", comp.getStyle().get("height"));
    }

    @Test
    public void initializeElement_disabled_elementIsEnabledContainsProperty() {
        initializer.accept(
                Collections.singletonMap("disabled", Boolean.TRUE.toString()));

        Assert.assertTrue("Element should stay enabled", element.isEnabled());
        Assert.assertTrue("Disabled should show as a property",
                element.hasProperty("disabled"));
    }

    @Tag(Tag.DIV)
    public static class TestComponent extends Component implements HasStyle {

        TestComponent(Element element) {
            super(element);
        }
    }
}
