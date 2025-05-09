/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jcip.annotations.NotThreadSafe;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.InvalidCustomElementNameException;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@NotThreadSafe
public class WebComponentConfigurationRegistryInitializerTest {
    private static final String DUPLICATE_PROPERTY_NAME = "one";

    private WebComponentConfigurationRegistryInitializer initializer;
    @Mock
    private WebComponentConfigurationRegistry registry;
    @Mock
    private ServletContext servletContext;
    @Mock
    private VaadinService vaadinService;
    @Mock
    private VaadinContext context;
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(vaadinService.getContext()).thenReturn(context);
        Mockito.when(
                context.getAttribute(WebComponentConfigurationRegistry.class))
                .thenReturn(registry);
        Mockito.when(context.getAttribute(
                eq(WebComponentConfigurationRegistry.class), any()))
                .thenReturn(registry);

        initializer = new WebComponentConfigurationRegistryInitializer();
        when(servletContext.getAttribute(
                WebComponentConfigurationRegistry.class.getName()))
                .thenReturn(registry);

        VaadinService.setCurrent(vaadinService);
        when(vaadinService.getInstantiator())
                .thenReturn(new MockInstantiator());
    }

    @After
    public void cleanUp() {
        CurrentInstance.clearAll();
    }

    @Test
    public void process() throws ServletException {
        initializer.process(
                Stream.of(MyComponentExporter.class, UserBoxExporter.class,
                        ExporterFactory.class).collect(Collectors.toSet()),
                servletContext);
        ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(registry).setConfigurations(captor.capture());
        Set<?> set = captor.getValue();
        Assert.assertEquals(3, set.size());
        Set<Class> componentClasses = set.stream()
                .map(WebComponentConfiguration.class::cast)
                .map(WebComponentConfiguration::getComponentClass)
                .collect(Collectors.toSet());
        Assert.assertTrue(componentClasses.contains(MyComponent.class));
        Assert.assertTrue(componentClasses.contains(UserBox.class));
        Assert.assertTrue(componentClasses.contains(InvalidName.class));
    }

    @Test
    public void process_noExceptionWithNullArguments() {
        try {
            initializer.process(null, servletContext);
        } catch (Exception e) {
            Assert.fail(
                    "WebComponentRegistryInitializer.process should not throw with null argument");
        }
        // Expect a call to setWebComponents even if we have an empty or null
        // set
        Mockito.verify(registry).setConfigurations(Collections.emptySet());
    }

    @Test
    public void process_noExceptionForMultipleCorrectExportsOfTheSameComponent() {
        try {
            initializer.process(
                    Stream.of(MyComponentExporter.class, SiblingExporter.class)
                            .collect(Collectors.toSet()),
                    servletContext);
        } catch (Exception e) {
            Assert.fail("WebComponentRegistryInitializer.process should not "
                    + "throw with 'sibling' exporters");
        }
    }

    @Test
    public void emptySet_noExceptionAndWebComponentsSet() {
        try {
            initializer.process(Collections.emptySet(), servletContext);
        } catch (Exception e) {
            Assert.fail(
                    "WebComponentRegistryInitializer.process should not throw with empty set");
        }
        Mockito.verify(registry).setConfigurations(Collections.emptySet());
    }

    @Test
    public void duplicateNamesFoundprocess_exceptionIsThrown()
            throws ServletException {
        expectedEx.expect(ServletException.class);
        expectedEx.expectCause(CauseMatcher.ex(IllegalArgumentException.class)
                .msgStartsWith("Found two WebComponentExporter classes"));
        initializer.process(
                Stream.of(MyComponentExporter.class, DuplicateTagExporter.class)
                        .collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void invalidCustomElementName_initializerThrowsException()
            throws ServletException {
        expectedEx.expect(ServletException.class);
        expectedEx.expectCause(CauseMatcher
                .ex(InvalidCustomElementNameException.class)
                .msgEquals(String.format(
                        "Tag name '%s' given by '%s' is not a valid custom element "
                                + "name.",
                        "invalid",
                        InvalidNameExporter.class.getCanonicalName())));

        initializer.process(Collections.singleton(InvalidNameExporter.class),
                servletContext);
    }

    @Test
    public void duplicatePropertyRegistration_doesNotCauseIssues()
            throws ServletException {
        initializer.process(
                Collections.singleton(DuplicatePropertyExporter.class),
                servletContext);
    }

    @Test
    public void duplicatePropertyRegistrationBetweenParentAndChild_doesNotCauseIssues()
            throws ServletException {
        initializer.process(Collections.singleton(ExtendingExporter.class),
                servletContext);
    }

    private static class MyComponent extends Component {
    }

    private static class UserBox extends Component {
    }

    private static class InvalidName extends Component {
    }

    public static class MyComponentExporter
            extends WebComponentExporter<MyComponent> {

        public MyComponentExporter() {
            this("my-component");
        }

        protected MyComponentExporter(String tag) {
            super(tag);
            addProperty(DUPLICATE_PROPERTY_NAME, "component");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    public static class ExporterFactory
            implements WebComponentExporterFactory<InvalidName> {

        @Override
        public WebComponentExporter<InvalidName> create() {
            return new PrivateExporter("foo-bar");
        }

    }

    private static class PrivateExporter
            extends WebComponentExporter<InvalidName> {

        private PrivateExporter(String tag) {
            super(tag);
        }

        @Override
        protected void configureInstance(WebComponent<InvalidName> webComponent,
                InvalidName component) {
        }

    }

    public static class UserBoxExporter extends WebComponentExporter<UserBox> {

        public UserBoxExporter() {
            super("user-box");
            addProperty("user", "box");
        }

        @Override
        public void configureInstance(WebComponent<UserBox> webComponent,
                UserBox component) {

        }
    }

    public static class InvalidNameExporter
            extends WebComponentExporter<InvalidName> {

        public InvalidNameExporter() {
            super("invalid");
        }

        @Override
        public void configureInstance(WebComponent<InvalidName> webComponent,
                InvalidName component) {

        }
    }

    public static class ExtendingExporter extends MyComponentExporter {

        public ExtendingExporter() {
            super("tag-1");
            addProperty(DUPLICATE_PROPERTY_NAME, "something");
        }
    }

    public static class SiblingExporter
            extends WebComponentExporter<MyComponent> {

        public SiblingExporter() {
            super("my-component-sibling");
            addProperty("name", "something");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    public static class DuplicateTagExporter
            extends WebComponentExporter<MyComponent> {

        public DuplicateTagExporter() {
            super("my-component");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    public static class DuplicatePropertyExporter
            extends WebComponentExporter<MyComponent> {

        public DuplicatePropertyExporter() {
            super("tag-2");
            addProperty(DUPLICATE_PROPERTY_NAME, "two");
            addProperty(DUPLICATE_PROPERTY_NAME, "four");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    public static class CauseMatcher extends BaseMatcher<Throwable> {
        private final Class<? extends Throwable> throwableType;
        private boolean startsWith = false;
        private String matchable = null;

        private CauseMatcher(Class<? extends Throwable> throwableType) {
            this.throwableType = throwableType;
        }

        @Override
        public boolean matches(Object o) {
            Throwable throwable = ((Throwable) o).getCause();

            if (!throwableType.equals(throwable.getClass())) {
                return false;
            }

            if (matchable != null) {
                if (startsWith) {
                    return throwable.getMessage().startsWith(matchable);
                } else {
                    return throwable.getMessage().equals(matchable);
                }
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format("<%s: %s%s>",
                    throwableType.getCanonicalName(), matchable,
                    (startsWith ? "..." : "")));
        }

        public static CauseMatcher ex(
                Class<? extends Throwable> throwableType) {
            return new CauseMatcher(throwableType);
        }

        public CauseMatcher msgStartsWith(String str) {
            startsWith = true;
            matchable = str;
            return this;
        }

        public CauseMatcher msgEquals(String str) {
            startsWith = false;
            matchable = str;
            return this;
        }
    }
}
