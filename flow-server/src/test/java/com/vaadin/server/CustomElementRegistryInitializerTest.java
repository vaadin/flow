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
package com.vaadin.server;

import javax.servlet.ServletException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;

/**
 * Test that correct @Tag custom elements get loaded by the initializer loader.
 */
public class CustomElementRegistryInitializerTest {

    @Test
    public void testValidCustomElement() throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class).collect(Collectors.toSet()),
                null);

        Assert.assertTrue(CustomElementRegistryInitializer.customElements
                .containsKey("custom-element"));
    }

    @Test
    public void testInvalidCustomElement() throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(Stream
                .of(InvalidCustomElement.class).collect(Collectors.toSet()),
                null);

        Assert.assertFalse(CustomElementRegistryInitializer.customElements
                .containsKey("-invalid"));
    }

    @Test
    public void testMultipleTagsWithValidExtends() throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class, ValidExtendingElement.class)
                        .collect(Collectors.toSet()),
                null);

        Assert.assertTrue(CustomElementRegistryInitializer.customElements
                .containsKey("custom-element"));
        Assert.assertEquals("Stored element was not the super class",
                ValidCustomElement.class,
                CustomElementRegistryInitializer.customElements
                        .get("custom-element"));
    }

    @Test(expected = ClassCastException.class)
    public void testMultipleTagsWithFaultyExtends() throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(Stream
                .of(ValidCustomElement.class, InvalidExtendingElement.class)
                .collect(Collectors.toSet()), null);
    }

    @Test
    public void testNonPolymerTemplateElementsAreNotAccepted()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(NonPolymerElement.class).collect(Collectors.toSet()),
                null);

        Assert.assertFalse(CustomElementRegistryInitializer.customElements
                .containsKey("non-polymer"));
    }

    @Test
    public void testCreationOfRegisteredCustomElement()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(Stream
                .of(CustomPolymerElement.class).collect(Collectors.toSet()),
                null);

        Element polymerElement = new Element("custom-polymer-element");

        Assert.assertTrue("Element didn't have a Component",
                polymerElement.getComponent().isPresent());

        Assert.assertTrue("Element got unexpected Component", polymerElement
                .getComponent().get() instanceof CustomPolymerElement);
    }

    @Test
    public void testCreationOfRegisteredCustomElementGetsSuperClass()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class, ValidExtendingElement.class,
                        CustomPolymerElement.class).collect(Collectors.toSet()),
                null);

        Element element = new Element("custom-element");

        Assert.assertTrue("Element didn't have a Component",
                element.getComponent().isPresent());

        Assert.assertTrue("Element got unexpected Component",
                element.getComponent().get() instanceof ValidCustomElement);
    }

    @Test
    public void testCreationOfMultipleRegisteredCustomElements()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class, ValidExtendingElement.class,
                        CustomPolymerElement.class).collect(Collectors.toSet()),
                null);

        Element customElement = new Element("custom-element");

        Assert.assertTrue("CustomElement didn't have a Component",
                customElement.getComponent().isPresent());

        Assert.assertTrue("CustomElement got unexpected Component",
                customElement.getComponent().get() instanceof ValidCustomElement);

        Element polymerElement = new Element("custom-polymer-element");

        Assert.assertTrue("PolymerElement didn't have a Component",
                polymerElement.getComponent().isPresent());

        Assert.assertTrue("PolymerElement got unexpected Component", polymerElement
                .getComponent().get() instanceof CustomPolymerElement);
    }

    @Tag("custom-element")
    public static class ValidCustomElement
            extends PolymerTemplate<TemplateModel> {
    }

    @Tag("custom-element")
    public static class ValidExtendingElement extends ValidCustomElement {
    }

    @Tag("-invalid")
    public static class InvalidCustomElement
            extends PolymerTemplate<TemplateModel> {
    }

    @Tag("custom-element")
    public static class InvalidExtendingElement
            extends PolymerTemplate<TemplateModel> {
    }

    @Tag("custom-polymer-element")
    public static class CustomPolymerElement
            extends PolymerTemplate<TemplateModel> {
    }

    @Tag("non-polymer")
    public static class NonPolymerElement {
    }
}
