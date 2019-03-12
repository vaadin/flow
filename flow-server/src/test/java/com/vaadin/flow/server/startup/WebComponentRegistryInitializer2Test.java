/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.server.InvalidCustomElementNameException;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.webcomponent.WebComponentBuilderRegistry;

import static org.mockito.Mockito.when;

public class WebComponentRegistryInitializer2Test {
    private static final String DUPLICATE_PROPERTY_NAME = "one";

    private WebComponentRegistryInitializer2 initializer;
    @Mock
    private WebComponentBuilderRegistry registry;
    @Mock
    private ServletContext servletContext;
    @Mock
    private VaadinService vaadinService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        initializer = new WebComponentRegistryInitializer2();
        when(servletContext
                .getAttribute(WebComponentBuilderRegistry.class.getName()))
                .thenReturn(registry);

        VaadinService.setCurrent(vaadinService);
        when(vaadinService.getInstantiator()).thenReturn(new MockInstantiator());
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void onStartUp() throws ServletException {
        initializer.onStartup(Stream.of(MyComponentExporter.class,
                UserBoxExporter.class)
                .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_noExceptionWithNullArguments() {
        try {
            initializer.onStartup(null, servletContext);
        } catch (Exception e) {
            Assert.fail(
                    "WebComponentRegistryInitializer.onStartup should not throw with null argument");
        }
        // Expect a call to setWebComponents even if we have an empty or null set
        Mockito.verify(registry).setWebComponentBuilders(Collections.emptySet());
    }

    @Test
    public void onStartUp_noExceptionForMultipleCorrectExportsOfTheSameComponent() {
        try {
            initializer.onStartup(Stream.of(MyComponentExporter.class,
                    SiblingExporter.class)
                    .collect(Collectors.toSet()), servletContext);
        } catch (Exception e) {
            Assert.fail(
                    "WebComponentRegistryInitializer.onStartup should not " +
                            "throw with 'sibling' exporters");
        }
    }

    @Test
    public void emptySet_noExceptionAndWebComponentsSet() {
        try {
            initializer.onStartup(Collections.emptySet(), servletContext);
        } catch (Exception e) {
            Assert.fail(
                    "WebComponentRegistryInitializer.onStartup should not throw with empty set");
        }
        Mockito.verify(registry).setWebComponentBuilders(Collections.emptySet());
    }

    @Test
    public void duplicateNamesFoundOnStartUp_exceptionIsThrown()
            throws ServletException {
        expectedEx.expect(IllegalArgumentException.class);
        initializer.onStartup(
                Stream.of(MyComponentExporter.class, DuplicateTagExporter.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void invalidCustomElementName_initializerThrowsException()
            throws ServletException {
        expectedEx.expect(InvalidCustomElementNameException.class);
        expectedEx.expectMessage(String.format(
                "Tag name '%s' given by '%s' is not a valid custom element " +
                        "name.",
                "invalid", InvalidNameExporter.class.getCanonicalName()));

        initializer.onStartup(Collections.singleton(InvalidNameExporter.class),
                servletContext);
    }

    @Test
    public void duplicatePropertyRegistration_initializerThrowsException() throws ServletException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(String.format(
                "Invalid exporter '%s': Property '%s' has already been " +
                        "registered! WebComponent cannot have multiple " +
                        "properties with the same name.",
                DuplicatePropertyExporter.class.getCanonicalName(), DUPLICATE_PROPERTY_NAME));

        initializer.onStartup(Collections.singleton(DuplicatePropertyExporter.class), servletContext);
    }

    @Test
    public void duplicatePropertyRegistrationBetweenParentAndChild_initializerThrowsException() throws ServletException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(String.format(
                "Invalid exporter '%s': Property '%s' has already been " +
                        "registered! WebComponent cannot have multiple " +
                        "properties with the same name.",
                ExtendingExporter.class.getCanonicalName(), DUPLICATE_PROPERTY_NAME));

        initializer.onStartup(Collections.singleton(ExtendingExporter.class), servletContext);
    }

    public static class MyComponent extends Component {
    }

    public static class MyDuplicateComponent extends Component {
    }

    public static class UserBox extends Component {
    }

    public static class InvalidName extends Component {
    }

    public static class MyComponentExporter implements WebComponentExporter<MyComponent> {
        @Override
        public String getTag() {
            return "my-component";
        }

        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {
            definition.addProperty(DUPLICATE_PROPERTY_NAME, "component");
        }
    }

    public static class UserBoxExporter implements WebComponentExporter<UserBox> {

        @Override
        public String getTag() {
            return "user-box";
        }

        @Override
        public void define(WebComponentDefinition<UserBox> definition) {
            definition.addProperty("user", "box");
        }
    }

    public static class InvalidNameExporter implements WebComponentExporter<InvalidName> {

        @Override
        public String getTag() {
            return "invalid";
        }

        @Override
        public void define(WebComponentDefinition<InvalidName> definition) {
            // PASS
        }
    }

    public static class ExtendingExporter extends MyComponentExporter {
        @Override
        public String getTag() {
            return "tag-1";
        }

        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {
            super.define(definition);

            // overwrites a property - BAD!
            definition.addProperty(DUPLICATE_PROPERTY_NAME, "something");
        }
    }

    public static class SiblingExporter implements WebComponentExporter<MyComponent> {

        @Override
        public String getTag() {
            return "my-component-sibling";
        }

        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {
            definition.addProperty("name", "something");
        }
    }

    public static class DuplicateTagExporter implements WebComponentExporter<MyComponent> {

        @Override
        public String getTag() {
            return "my-component";
        }

        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {

        }
    }

    public static class DuplicatePropertyExporter implements WebComponentExporter<MyComponent> {

        @Override
        public String getTag() {
            return "tag-2";
        }

        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {
            definition.addProperty(DUPLICATE_PROPERTY_NAME, "two");
            definition.addProperty(DUPLICATE_PROPERTY_NAME, "four");
        }
    }
}
