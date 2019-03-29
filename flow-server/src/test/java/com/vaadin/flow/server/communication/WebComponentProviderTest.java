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

package com.vaadin.flow.server.communication;

import static org.mockito.Mockito.times;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class WebComponentProviderTest {

    @Mock
    VaadinSession session;
    @Mock
    VaadinServletRequest request;
    @Mock
    VaadinResponse response;

    WebComponentProvider provider;

    @Mock
    VaadinService service;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(session.getService()).thenReturn(service);
        VaadinService.setCurrent(service);
        Mockito.when(service.getInstantiator())
                .thenReturn(new MockInstantiator());

        provider = new WebComponentProvider();
    }

    @After
    public void cleanUp() {
        CurrentInstance.clearAll();
    }

    @Test
    public void nonHandledPaths_handlerInformsNotHandled() throws IOException {
        Mockito.when(request.getPathInfo()).thenReturn(null);
        Assert.assertFalse("Provider shouldn't handle null path",
                provider.handleRequest(session, request, response));

        Mockito.when(request.getPathInfo()).thenReturn("");
        Assert.assertFalse("Provider shouldn't handle empty path",
                provider.handleRequest(session, request, response));

        Mockito.when(request.getPathInfo()).thenReturn("/home");
        Assert.assertFalse("Provider shouldn't handle non web-component path",
                provider.handleRequest(session, request, response));
    }

    @Test
    public void faultyTag_handlerInformsNotHandled() throws IOException {
        Mockito.when(request.getPathInfo()).thenReturn("/web-component/path");

        Assert.assertFalse("Provider shouldn't handle non '.html' path",
                provider.handleRequest(session, request, response));

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/component.html");

        Assert.assertFalse(
                "Provider shouldn't handle request for non custom element html name",
                provider.handleRequest(session, request, response));
    }

    @Test
    public void webComponentNotPresent_responseReturns404() throws IOException {
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        Mockito.when(request.getServletContext()).thenReturn(servletContext);

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.html");
        Assert.assertTrue("Provider should handle web-component request",
                provider.handleRequest(session, request, response));
        Mockito.verify(response).sendError(HttpServletResponse.SC_NOT_FOUND,
                "No such web component");
    }

    @Test
    public void webComponentGenerator_responseGetsResult() throws IOException {
        WebComponentConfigurationRegistry registry = setupExporters(
                MyComponentExporter.class);

        ByteArrayOutputStream out = Mockito.mock(ByteArrayOutputStream.class);

        DefaultDeploymentConfiguration configuration = Mockito
                .mock(DefaultDeploymentConfiguration.class);

        Mockito.when(response.getOutputStream()).thenReturn(out);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.getRootElementId()).thenReturn("");

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.html");
        Assert.assertTrue("Provider should handle web-component request",
                provider.handleRequest(session, request, response));

        Mockito.verify(response).getOutputStream();
        Mockito.verify(out).write(Mockito.any());

    }

    @Test
    public void providesDifferentGeneratedHTMLForEachExportedComponent()
            throws IOException {
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

        WebComponentConfigurationRegistry registry = setupExporters(
                MyComponentExporter.class, OtherComponentExporter.class);

        ByteArrayOutputStream out = Mockito.mock(ByteArrayOutputStream.class);

        DefaultDeploymentConfiguration configuration = Mockito
                .mock(DefaultDeploymentConfiguration.class);

        Mockito.when(response.getOutputStream()).thenReturn(out);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.getRootElementId()).thenReturn("");

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.html");
        Assert.assertTrue("Provider should handle first web-component request",
                provider.handleRequest(session, request, response));

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/other-component.html");
        Assert.assertTrue("Provider should handle second web-component request",
                provider.handleRequest(session, request, response));

        Mockito.verify(response, times(2)).getOutputStream();
        Mockito.verify(out, times(2)).write(captor.capture());

        byte[] first = captor.getAllValues().get(0);
        byte[] second = captor.getAllValues().get(1);

        Assert.assertNotEquals("Stream output should not match", first, second);
    }

    @Test
    public void setExporters_exportersHasNoTheme_themeIsNull() {
        WebComponentConfigurationRegistry registry = setupExporters(
                MyComponentExporter.class, OtherComponentExporter.class);

        Assert.assertFalse(registry
                .getEmbeddedApplicationAnnotation(Theme.class).isPresent());
    }

    @Test
    public void notInitializedRegistry_themeIsEmpty() {
        WebComponentConfigurationRegistry registry = setUpRegistry();
        Assert.assertFalse(registry
                .getEmbeddedApplicationAnnotation(Theme.class).isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void setExporters_exportersHasVariousThemes_throws() {
        WebComponentConfigurationRegistry registry = setupExporters(
                ThemedComponentExporter.class,
                AnotherThemedComponentExporter.class);
    }

    @Test(expected = IllegalStateException.class)
    public void setExporters_exportersHasVariousPushes_throws() {
        WebComponentConfigurationRegistry registry = setupExporters(
                ThemedComponentExporter.class,
                AnotherPushComponentExporter.class);
    }

    @Test
    public void setExporters_exportersHasOneThemes_themeIsSet() {
        WebComponentConfigurationRegistry registry = setupExporters(
                ThemedComponentExporter.class, MyComponentExporter.class);
        Assert.assertEquals(MyTheme.class, registry
                .getEmbeddedApplicationAnnotation(Theme.class).get().value());
    }

    @Test
    public void setExporters_exportersHasOnePush_pushIsSet() {
        WebComponentConfigurationRegistry registry = setupExporters(
                ThemedComponentExporter.class, MyComponentExporter.class);
        Assert.assertTrue(registry.getEmbeddedApplicationAnnotation(Push.class)
                .isPresent());
    }

    @Test
    public void setExporters_exportersHasSameThemeDeclarations_themeIsSet() {
        WebComponentConfigurationRegistry registry = setupExporters(
                ThemedComponentExporter.class,
                SameThemedComponentExporter.class);
        Assert.assertEquals(MyTheme.class, registry
                .getEmbeddedApplicationAnnotation(Theme.class).get().value());
    }

    @Test
    public void setExporters_exportersHasSamePushDeclarations_pushIsSet() {
        WebComponentConfigurationRegistry registry = setupExporters(
                ThemedComponentExporter.class,
                SameThemedComponentExporter.class);
        Assert.assertEquals(PushMode.AUTOMATIC, registry
                .getEmbeddedApplicationAnnotation(Push.class).get().value());
    }

    private WebComponentConfigurationRegistry setupExporters(
            Class<? extends WebComponentExporter<? extends Component>>... exporters) {
        WebComponentConfigurationRegistry registry = setUpRegistry();

        final HashMap<String, Class<? extends WebComponentExporter<? extends Component>>> map = new HashMap<>();
        Stream.of(exporters)
                .forEach(clazz -> map.put(AnnotationReader
                        .getAnnotationValueFor(clazz, Tag.class, Tag::value)
                        .get(), clazz));
        registry.setExporters(map);

        return registry;
    }

    private WebComponentConfigurationRegistry setUpRegistry() {
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        Mockito.when(request.getServletContext()).thenReturn(servletContext);
        Mockito.when(request.getContextPath()).thenReturn("");
        WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                .getInstance(servletContext);
        Mockito.when(servletContext.getAttribute(
                WebComponentConfigurationRegistry.class.getName()))
                .thenReturn(registry);
        return registry;
    }

    @Tag("my-component")
    public static class MyComponent extends Component {
    }

    @Tag("my-component")
    public static class MyComponentExporter
            implements WebComponentExporter<MyComponent> {
        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {

        }
    }

    @Tag("another-component")
    public static class OtherComponent extends Component {
    }

    @Tag("other-component")
    public static class OtherComponentExporter
            implements WebComponentExporter<OtherComponent> {
        @Override
        public void define(WebComponentDefinition<OtherComponent> definition) {

        }
    }

    @Tag("foo")
    @Theme(MyTheme.class)
    @Push
    public static class ThemedComponentExporter
            implements WebComponentExporter<Component> {
        @Override
        public void define(WebComponentDefinition<Component> definition) {
        }
    }

    @Tag("foo")
    @Theme(MyTheme.class)
    @Push(value = PushMode.AUTOMATIC)
    public static class SameThemedComponentExporter
            implements WebComponentExporter<Component> {
        @Override
        public void define(WebComponentDefinition<Component> definition) {
        }
    }

    @Tag("foo-bar")
    @Push(value = PushMode.DISABLED)
    public static class AnotherPushComponentExporter
            implements WebComponentExporter<Component> {
        @Override
        public void define(WebComponentDefinition<Component> definition) {
        }
    }

    @Tag("foo-bar")
    @Theme(AnotherTheme.class)
    public static class AnotherThemedComponentExporter
            implements WebComponentExporter<Component> {
        @Override
        public void define(WebComponentDefinition<Component> definition) {
        }
    }

    public static class MyTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return null;
        }

        @Override
        public String getThemeUrl() {
            return null;
        }

    }

    public static class AnotherTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return null;
        }

        @Override
        public String getThemeUrl() {
            return null;
        }

    }

}
