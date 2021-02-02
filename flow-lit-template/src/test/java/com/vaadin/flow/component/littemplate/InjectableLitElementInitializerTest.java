/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.template.internal.AbstractInjectableElementInitializer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

public class InjectableLitElementInitializerTest {

    private Element element = ElementFactory.createDiv();

    private InjectableLitElementInitializer initializer = new InjectableLitElementInitializer(
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
    public void initializeElement_setAttributeBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap("?class", "foo"));

        Assert.assertFalse(element.hasAttribute("class"));
        Assert.assertFalse(element.hasAttribute("?class"));
        Assert.assertFalse(element.hasProperty("class"));
        Assert.assertFalse(element.hasProperty("?class"));
    }

    @Test
    public void initializeElement_setPropertyBinding_attributeIsIgnored() {
        initializer.accept(Collections.singletonMap(".foo", "bar"));

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
    public void initializeElement_disabled_elementIsDisabled() {
        initializer.accept(
                Collections.singletonMap("disabled", Boolean.TRUE.toString()));

        Assert.assertFalse(element.isEnabled());
    }

    public void initializeElement_setText_textIsSet() {
        initializer.accept(Collections.singletonMap(
                AbstractInjectableElementInitializer.TEXT_DATA, "foo bar"));

        Assert.assertEquals("foo bar", element.getText());
    }

    @Tag(Tag.DIV)
    public static class TestComponent extends Component implements HasStyle {

        TestComponent(Element element) {
            super(element);
        }
    }
}
