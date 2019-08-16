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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.ServletHelper;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

@NotThreadSafe
public class WebComponentProviderTest {

    @Mock
    VaadinSession session;
    @Mock
    VaadinServletRequest request;
    @Mock
    VaadinResponse response;
    @Mock
    VaadinServletService service;
    @Mock
    VaadinServletContext context;
    @Mock
    DeploymentConfiguration configuration;

    WebComponentProvider provider;

    WebComponentConfigurationRegistry registry;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        registry = setUpRegistry(); // same code as used for local variables in some tests
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(session.getService()).thenReturn(service);
        Mockito.when(service.getContext()).thenReturn(context);
        Mockito.when(context.getAttribute(WebComponentConfigurationRegistry.class)).then(invocationOnMock -> registry);
        Mockito.when(context.getAttribute(eq(WebComponentConfigurationRegistry.class), anyObject())).then(invocationOnMock -> registry);
        Mockito.doAnswer(invocationOnMock -> registry = (WebComponentConfigurationRegistry)invocationOnMock.getArguments()[0])
            .when(context).setAttribute(any(WebComponentConfigurationRegistry.class));
        VaadinService.setCurrent(service);
        Mockito.when(service.getInstantiator())
                .thenReturn(new MockInstantiator());
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(configuration);
        Mockito.when(service.getContextRootRelativePath(anyObject())).then(invocationOnMock -> ServletHelper.getContextRootRelativePath(((VaadinServletRequest)invocationOnMock.getArguments()[0]))+"/");
        Mockito.when(configuration.isCompatibilityMode()).thenReturn(false);

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
        Mockito.when(request.getPathInfo()).thenReturn("/web-component" +
                "/extensionless-component");

        Assert.assertFalse("Provider shouldn't handle path without extension",
                provider.handleRequest(session, request, response));

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/component.js");

        Assert.assertFalse(
                "Provider shouldn't handle request for non-custom element name",
                provider.handleRequest(session, request, response));

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.html");

        Assert.assertFalse(
                "Provider shouldn't handle html extensions in npm mode",
                provider.handleRequest(session, request, response));
    }

    @Test
    public void webComponentNotPresent_responseReturns404() throws IOException {
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        Mockito.when(request.getServletContext()).thenReturn(servletContext);

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        Assert.assertTrue("Provider should handle web-component request",
                provider.handleRequest(session, request, response));
        Mockito.verify(response).sendError(HttpServletResponse.SC_NOT_FOUND,
                "No web component for my-component");
    }

    @Test
    public void webComponentGenerator_responseGetsResult() throws IOException {
        registry = setupConfigurations(MyComponentExporter.class);

        ByteArrayOutputStream out = Mockito.mock(ByteArrayOutputStream.class);

        DefaultDeploymentConfiguration configuration = Mockito
                .mock(DefaultDeploymentConfiguration.class);

        Mockito.when(response.getOutputStream()).thenReturn(out);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        Assert.assertTrue("Provider should handle web-component request",
                provider.handleRequest(session, request, response));

        Mockito.verify(response).getOutputStream();
        Mockito.verify(out).write(Mockito.any());

    }

    @Test
    public void providesDifferentGeneratedHTMLForEachExportedComponent()
            throws IOException {
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

        registry = setupConfigurations(
                MyComponentExporter.class, OtherComponentExporter.class);

        ByteArrayOutputStream out = Mockito.mock(ByteArrayOutputStream.class);

        DefaultDeploymentConfiguration configuration = Mockito
                .mock(DefaultDeploymentConfiguration.class);

        Mockito.when(response.getOutputStream()).thenReturn(out);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        Assert.assertTrue("Provider should handle first web-component request",
                provider.handleRequest(session, request, response));

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/other-component.js");
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
        WebComponentConfigurationRegistry registry = setupConfigurations(
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
        WebComponentConfigurationRegistry registry = setupConfigurations(
                ThemedComponentExporter.class,
                AnotherThemedComponentExporter.class);
    }

    @Test(expected = IllegalStateException.class)
    public void setExporters_exportersHasVariousPushes_throws() {
        WebComponentConfigurationRegistry registry = setupConfigurations(
                ThemedComponentExporter.class,
                AnotherPushComponentExporter.class);
    }

    @Test
    public void setExporters_exportersHasOneThemes_themeIsSet() {
        WebComponentConfigurationRegistry registry = setupConfigurations(
                ThemedComponentExporter.class, MyComponentExporter.class);
        Assert.assertEquals(MyTheme.class, registry
                .getEmbeddedApplicationAnnotation(Theme.class).get().value());
    }

    @Test
    public void setExporters_exportersHasOnePush_pushIsSet() {
        WebComponentConfigurationRegistry registry = setupConfigurations(
                ThemedComponentExporter.class, MyComponentExporter.class);
        Assert.assertTrue(registry.getEmbeddedApplicationAnnotation(Push.class)
                .isPresent());
    }

    @Test
    public void setExporters_exportersHasSameThemeDeclarations_themeIsSet() {
        WebComponentConfigurationRegistry registry = setupConfigurations(
                ThemedComponentExporter.class,
                SameThemedComponentExporter.class);
        Assert.assertEquals(MyTheme.class, registry
                .getEmbeddedApplicationAnnotation(Theme.class).get().value());
    }

    @Test
    public void setExporters_exportersHasSamePushDeclarations_pushIsSet() {
        WebComponentConfigurationRegistry registry = setupConfigurations(
                ThemedComponentExporter.class,
                SameThemedComponentExporter.class);
        Assert.assertEquals(PushMode.AUTOMATIC, registry
                .getEmbeddedApplicationAnnotation(Push.class).get().value());
    }

    @SafeVarargs
    private final WebComponentConfigurationRegistry setupConfigurations(
            Class<? extends WebComponentExporter<? extends Component>>... exporters) {
        WebComponentConfigurationRegistry registry = setUpRegistry();

        final Set<Class<? extends WebComponentExporter<?
                                extends Component>>> set =
                Stream.of(exporters).collect(Collectors.toSet());

        WebComponentExporter.WebComponentConfigurationFactory factory =
                new WebComponentExporter.WebComponentConfigurationFactory();

        registry.setConfigurations(set.stream().map(factory::create)
                .collect(Collectors.toSet()));

        return registry;
    }

    private WebComponentConfigurationRegistry setUpRegistry() {
        // this hack is needed, because the OSGiAccess fake servlet context is now not needed
        return new WebComponentConfigurationRegistry(){};
    }

    @Tag("my-component")
    public static class MyComponent extends Component {
    }

    public static class MyComponentExporter
            extends WebComponentExporter<MyComponent> {

        public MyComponentExporter() {
            super ("my-component");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent, MyComponent component) {

        }
    }

    @Tag("another-component")
    public static class OtherComponent extends Component {
    }

    public static class OtherComponentExporter
            extends WebComponentExporter<OtherComponent> {

        public OtherComponentExporter() {
            super ("other-component");
        }

        @Override
        public void configureInstance(WebComponent<OtherComponent> webComponent, OtherComponent component) {

        }
    }

    @Push
    @Theme(MyTheme.class)
    public static class ThemedComponentExporter
            extends WebComponentExporter<Component> {
        public ThemedComponentExporter() {
            super ("foo");
        }

        @Override
        public void configureInstance(WebComponent<Component> webComponent, Component component) {

        }
    }

    @Theme(MyTheme.class)
    @Push(value = PushMode.AUTOMATIC)
    public static class SameThemedComponentExporter
            extends WebComponentExporter<Component> {
        public SameThemedComponentExporter() {
            super ("foo");
        }

        @Override
        public void configureInstance(WebComponent<Component> webComponent, Component component) {

        }
    }

    @Push(value = PushMode.DISABLED)
    public static class AnotherPushComponentExporter
            extends WebComponentExporter<Component> {
        public AnotherPushComponentExporter() {
            super ("foo-bar");
        }

        @Override
        public void configureInstance(WebComponent<Component> webComponent, Component component) {

        }
    }

    @Theme(AnotherTheme.class)
    public static class AnotherThemedComponentExporter
            extends WebComponentExporter<Component> {
        public AnotherThemedComponentExporter() {
            super ("foo-bar");
        }

        @Override
        public void configureInstance(WebComponent<Component> webComponent, Component component) {

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
