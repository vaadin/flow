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
import org.junit.Before;
import org.junit.Test;

import com.vaadin.annotations.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.ui.UI;

/**
 * Test that correct @Tag custom elements get loaded by the initializer loader.
 */
public class CustomElementRegistryInitializerTest {

    @Before
    public void setup() {
        CustomElementRegistry.getInstance().initialized = false;
        UI ui = new UI();
        UI.setCurrent(ui);
    }

    @Test
    public void testValidCustomElementIsRegistered() throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class).collect(Collectors.toSet()),
                null);

        Assert.assertTrue(CustomElementRegistry.getInstance()
                .isRegisteredCustomElement("custom-element"));
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidCustomElementNameThrowsException()
            throws ServletException {
        // Invalid name should throw an exception due to not being supported
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(Stream
                .of(InvalidCustomElement.class).collect(Collectors.toSet()),
                null);
    }

    @Test
    public void testMultipleCustomElementsWithSameValidTagNameRegisterCorrectSuperClass()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class, ValidExtendingElement.class)
                        .collect(Collectors.toSet()),
                null);

        Assert.assertTrue(CustomElementRegistry.getInstance()
                .isRegisteredCustomElement("custom-element"));
        Assert.assertEquals("Stored element was not the super class",
                ValidCustomElement.class, CustomElementRegistry.getInstance()
                        .getRegisteredCustomElement("custom-element"));
    }

    @Test(expected = ClassCastException.class)
    public void testMultipleCustomElementsWithSameValidTagNameFailsDueToFaultyExtends()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(Stream
                .of(ValidCustomElement.class, InvalidExtendingElement.class)
                .collect(Collectors.toSet()), null);
    }

    @Test
    public void testNonPolymerTemplateElementsAreNotRegistered()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(NonPolymerElement.class).collect(Collectors.toSet()),
                null);

        Assert.assertFalse(CustomElementRegistry.getInstance()
                .isRegisteredCustomElement("non-polymer"));
    }

    @Test
    public void testCreationOfRegisteredCustomElementWiresComponentForElement()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(Stream
                .of(CustomPolymerElement.class).collect(Collectors.toSet()),
                null);

        Element polymerElement = new Element("custom-polymer-element");

        UI.getCurrent().getElement().appendChild(polymerElement);

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

        UI.getCurrent().getElement().appendChild(element);

        Assert.assertTrue("Element didn't have a Component",
                element.getComponent().isPresent());

        Assert.assertTrue("Element got unexpected Component",
                element.getComponent().get() instanceof ValidCustomElement);
    }

    @Test
    public void testCreationOfMultipleRegisteredCustomElementsGetCorrectComponentWired()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class, ValidExtendingElement.class,
                        CustomPolymerElement.class).collect(Collectors.toSet()),
                null);

        Element customElement = new Element("custom-element");

        UI.getCurrent().getElement().appendChild(customElement);

        Assert.assertTrue("CustomElement didn't have a Component",
                customElement.getComponent().isPresent());

        Assert.assertTrue("CustomElement got unexpected Component",
                customElement.getComponent()
                        .get() instanceof ValidCustomElement);

        Element polymerElement = new Element("custom-polymer-element");

        UI.getCurrent().getElement().appendChild(polymerElement);

        Assert.assertTrue("PolymerElement didn't have a Component",
                polymerElement.getComponent().isPresent());

        Assert.assertTrue("PolymerElement got unexpected Component",
                polymerElement.getComponent()
                        .get() instanceof CustomPolymerElement);
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
