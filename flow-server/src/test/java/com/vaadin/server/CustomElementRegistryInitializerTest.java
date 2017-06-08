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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.annotations.Tag;
import com.vaadin.external.jsoup.Jsoup;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.TemplateParser;
import com.vaadin.flow.template.model.TemplateModel;

/**
 * Test that correct @Tag custom elements get loaded by the initializer loader.
 */
public class CustomElementRegistryInitializerTest {

    private static final TemplateParser TEST_PARSER = (clazz, tag) -> Jsoup
            .parse("<dom-module id='" + tag + "'></dom-module>");

    @Before
    public void setup() {
        CustomElementRegistryAccess.resetRegistry();
    }

    @Test
    public void registeringACustomElementWithValidNameIsCollectedToTheRegistry()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class).collect(Collectors.toSet()),
                null);

        Assert.assertTrue(CustomElementRegistry.getInstance()
                .isRegisteredCustomElement("custom-element"));
    }

    @Test(expected = InvalidCustomElementNameException.class)
    public void registeringCustomElementWithInvalidNameThrowsException()
            throws ServletException {
        // Invalid name should throw an exception due to not being supported
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(Stream
                .of(InvalidCustomElement.class).collect(Collectors.toSet()),
                null);
    }

    @Test
    public void multipleCustomElementsWithSameValidTagNameRegisterCorrectSuperClass()
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
    public void nonPolymerTemplateElementsAreNotRegistered()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(
                Stream.of(NonPolymerElement.class).collect(Collectors.toSet()),
                null);

        Assert.assertFalse(CustomElementRegistry.getInstance()
                .isRegisteredCustomElement("non-polymer"));
    }

    @Test
    public void creatingElementWithRegisteredCustomTagNameWiresComponentForElement()
            throws ServletException {
        CustomElementRegistryInitializer customElementRegistryInitializer = new CustomElementRegistryInitializer();
        customElementRegistryInitializer.onStartup(Stream
                .of(CustomPolymerElement.class).collect(Collectors.toSet()),
                null);

        Element polymerElement = new Element("custom-polymer-element");

        Assert.assertTrue("Element didn't have a Component",
                polymerElement.getComponent().isPresent());

        Assert.assertTrue("Element got unexpected Component",
                polymerElement.getComponent().get().getClass()
                        .equals(CustomPolymerElement.class));
    }

    @Test
    public void creatingElementWithRegisteredCustomTagNameGetsSuperClassComponent()
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
                element.getComponent().get().getClass()
                        .equals(ValidCustomElement.class));
    }

    @Test
    public void creatingElementsWhenMultipleRegisteredCustomTagNamesInRegistryGetCorrectComponentWired()
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
                customElement.getComponent().get().getClass()
                        .equals(ValidCustomElement.class));

        Element polymerElement = new Element("custom-polymer-element");

        Assert.assertTrue("PolymerElement didn't have a Component",
                polymerElement.getComponent().isPresent());

        Assert.assertTrue("PolymerElement got unexpected Component",
                polymerElement.getComponent().get().getClass()
                        .equals(CustomPolymerElement.class));
    }

    @Tag("custom-element")
    public static class ValidCustomElement
            extends PolymerTemplate<TemplateModel> {

        public ValidCustomElement() {
            super(TEST_PARSER);
        }

    }

    @Tag("custom-element")
    public static class ValidExtendingElement extends ValidCustomElement {
    }

    @Tag("-invalid")
    public static class InvalidCustomElement
            extends PolymerTemplate<TemplateModel> {

        public InvalidCustomElement() {
            super(TEST_PARSER);
        }
    }

    @Tag("custom-element")
    public static class InvalidExtendingElement
            extends PolymerTemplate<TemplateModel> {

        public InvalidExtendingElement() {
            super(TEST_PARSER);
        }
    }

    @Tag("custom-polymer-element")
    public static class CustomPolymerElement
            extends PolymerTemplate<TemplateModel> {

        public CustomPolymerElement() {
            super(TEST_PARSER);
        }

    }

    @Tag("non-polymer")
    public static class NonPolymerElement {
    }
}
