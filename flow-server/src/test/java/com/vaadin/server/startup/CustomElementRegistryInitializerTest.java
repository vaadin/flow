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
package com.vaadin.server.startup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import org.jsoup.Jsoup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.InvalidCustomElementNameException;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.ui.polymertemplate.TemplateParser;
import com.vaadin.util.HasCurrentService;

import net.jcip.annotations.NotThreadSafe;

/**
 * Test that correct @Tag custom elements get loaded by the initializer loader.
 */
@NotThreadSafe
public class CustomElementRegistryInitializerTest extends HasCurrentService {

    private static final TemplateParser TEST_PARSER = (clazz, tag) -> Jsoup
            .parse("<dom-module id='" + tag + "'></dom-module>");

    private CustomElementRegistryInitializer customElementRegistryInitializer;

    @Override
    protected VaadinService createService() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        return service;
    }

    @Before
    public void setup() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field customElements = CustomElementRegistry.class
                .getDeclaredField("customElements");
        customElements.setAccessible(true);
        customElements.set(CustomElementRegistry.getInstance(),
                new AtomicReference<>());

        customElementRegistryInitializer = new CustomElementRegistryInitializer();

        VaadinSession session = Mockito.mock(VaadinSession.class);
        UI ui = new UI() {
            @Override
            public VaadinSession getSession() {
                return session;
            }
        };
        VaadinService service = Mockito.mock(VaadinService.class);
        when(session.getService()).thenReturn(service);
        DefaultInstantiator instantiator = new DefaultInstantiator(service);
        when(service.getInstantiator()).thenReturn(instantiator);
        UI.setCurrent(ui);
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    public void registryInitializerAcceptsNull() throws ServletException {
        // doesn't throw
        customElementRegistryInitializer.onStartup(null, null);
        assertTrue(CustomElementRegistry.getInstance().isInitialized());
    }

    @Test
    public void registeringACustomElementWithValidNameIsCollectedToTheRegistry()
            throws ServletException {
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class).collect(Collectors.toSet()),
                null);

        assertTrue(CustomElementRegistry.getInstance()
                .isRegisteredCustomElement("custom-element"));
    }

    @Test(expected = InvalidCustomElementNameException.class)
    public void registeringCustomElementWithInvalidNameThrowsException()
            throws ServletException {
        // Invalid name should throw an exception due to not being supported
        customElementRegistryInitializer.onStartup(Stream
                .of(InvalidCustomElement.class).collect(Collectors.toSet()),
                null);
    }

    @Test
    public void multipleCustomElementsWithSameValidTagNameRegisterCorrectSuperClass()
            throws ServletException {
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class, ValidExtendingElement.class)
                        .collect(Collectors.toSet()),
                null);

        assertTrue(CustomElementRegistry.getInstance()
                .isRegisteredCustomElement("custom-element"));
        assertEquals("Stored element was not the super class",
                ValidCustomElement.class, CustomElementRegistry.getInstance()
                        .getRegisteredCustomElement("custom-element"));
    }

    @Test(expected = IllegalStateException.class)
    public void testMultipleCustomElementsWithSameValidTagNameFailsDueToFaultyExtends()
            throws ServletException {
        customElementRegistryInitializer.onStartup(Stream
                .of(ValidCustomElement.class, InvalidExtendingElement.class)
                .collect(Collectors.toSet()), null);
    }

    @Test
    public void nonPolymerTemplateElementsAreNotRegistered()
            throws ServletException {
        customElementRegistryInitializer.onStartup(
                Stream.of(NonPolymerElement.class).collect(Collectors.toSet()),
                null);

        assertFalse(CustomElementRegistry.getInstance()
                .isRegisteredCustomElement("non-polymer"));
    }

    @Test
    public void creatingElementWithRegisteredCustomTagNameWiresComponentForElement()
            throws ServletException {
        customElementRegistryInitializer.onStartup(Stream
                .of(CustomPolymerElement.class).collect(Collectors.toSet()),
                null);

        Element polymerElement = new Element("custom-polymer-element");

        assertTrue("Element didn't have a Component",
                polymerElement.getComponent().isPresent());

        assertTrue("Element got unexpected Component",
                polymerElement.getComponent().get().getClass()
                        .equals(CustomPolymerElement.class));
    }

    @Test
    public void creatingElementWithRegisteredCustomTagNameGetsSuperClassComponent()
            throws ServletException {
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class, ValidExtendingElement.class,
                        CustomPolymerElement.class).collect(Collectors.toSet()),
                null);

        Element element = new Element("custom-element");

        assertTrue("Element didn't have a Component",
                element.getComponent().isPresent());

        assertTrue("Element got unexpected Component",
                element.getComponent().get().getClass()
                        .equals(ValidCustomElement.class));
    }

    @Test
    public void creatingElementsWhenMultipleRegisteredCustomTagNamesInRegistryGetCorrectComponentWired()
            throws ServletException {
        customElementRegistryInitializer.onStartup(
                Stream.of(ValidCustomElement.class, ValidExtendingElement.class,
                        CustomPolymerElement.class).collect(Collectors.toSet()),
                null);

        Element customElement = new Element("custom-element");

        assertTrue("CustomElement didn't have a Component",
                customElement.getComponent().isPresent());

        assertEquals("CustomElement got unexpected Component",
                ValidCustomElement.class,
                customElement.getComponent().get().getClass());

        Element polymerElement = new Element("custom-polymer-element");

        assertTrue("PolymerElement didn't have a Component",
                polymerElement.getComponent().isPresent());

        assertEquals("PolymerElement got unexpected Component",
                CustomPolymerElement.class,
                polymerElement.getComponent().get().getClass());
    }

    @Test
    public void shouldNotFailWhenCommonSuperClassIsLastElement()
            throws ServletException {
        customElementRegistryInitializer.onStartup(Stream
                .of(ValidExtendingElement.class, ValidExtendingElement2.class,
                        ValidExtendingElement3.class,
                        CustomPolymerElement.class, ValidCustomElement.class)
                .collect(Collectors.toCollection(LinkedHashSet::new)), null);

        Element element = new Element("custom-element");

        assertTrue("Element didn't have a Component",
                element.getComponent().isPresent());

        assertTrue("Element got unexpected Component",
                element.getComponent().get().getClass()
                        .equals(ValidCustomElement.class));
    }

    @Tag("custom-element")
    public static class ValidCustomElement
            extends PolymerTemplate<TemplateModel> {

        public ValidCustomElement() {
            super(TEST_PARSER);
        }

    }

    @Tag("custom-element")
    private static class ValidExtendingElement extends ValidCustomElement {
    }

    @Tag("custom-element")
    private static class ValidExtendingElement2 extends ValidCustomElement {
    }

    @Tag("custom-element")
    private static class ValidExtendingElement3 extends ValidCustomElement {
    }

    @Tag("-invalid")
    private static class InvalidCustomElement
            extends PolymerTemplate<TemplateModel> {

        public InvalidCustomElement() {
            super(TEST_PARSER);
        }
    }

    @Tag("custom-element")
    private static class InvalidExtendingElement
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
    private static class NonPolymerElement {
    }
}
