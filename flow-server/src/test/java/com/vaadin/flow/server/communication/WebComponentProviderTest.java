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

package com.vaadin.flow.server.communication;

import jakarta.servlet.ServletContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory.DefaultWebComponentExporterFactory;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.shared.communication.PushMode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        registry = setUpRegistry(); // same code as used for local variables in
                                    // some tests
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(session.getService()).thenReturn(service);
        Mockito.when(service.getContext()).thenReturn(context);
        Mockito.when(
                context.getAttribute(WebComponentConfigurationRegistry.class))
                .then(invocationOnMock -> registry);
        Mockito.when(context.getAttribute(
                eq(WebComponentConfigurationRegistry.class), any()))
                .then(invocationOnMock -> registry);
        Mockito.doAnswer(
                invocationOnMock -> registry = (WebComponentConfigurationRegistry) invocationOnMock
                        .getArguments()[0])
                .when(context)
                .setAttribute(any(WebComponentConfigurationRegistry.class));

        final Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        Mockito.doAnswer(i -> i.getArgument(1, Supplier.class).get())
                .when(context).getAttribute(
                        ArgumentMatchers.argThat(aClass -> "FeatureFlagsWrapper"
                                .equals(aClass.getSimpleName())),
                        any());
        VaadinService.setCurrent(service);
        Mockito.when(service.getInstantiator())
                .thenReturn(new MockInstantiator());
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        VaadinServletService service = Mockito.mock(VaadinServletService.class);
        Mockito.doCallRealMethod().when(service)
                .getContextRootRelativePath(Mockito.any());

        Mockito.doCallRealMethod().when(service)
                .getContextRootRelativePath(Mockito.any());

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
                provider.canHandleRequest(request));

        Mockito.when(request.getPathInfo()).thenReturn("");
        Assert.assertFalse("Provider shouldn't handle empty path",
                provider.canHandleRequest(request));

        Mockito.when(request.getPathInfo()).thenReturn("/home");
        Assert.assertFalse("Provider shouldn't handle non web-component path",
                provider.canHandleRequest(request));
    }

    @Test
    public void faultyTag_handlerInformsNotHandled() throws IOException {
        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component" + "/extensionless-component");

        Assert.assertFalse("Provider shouldn't handle path without extension",
                provider.synchronizedHandleRequest(session, request, response));

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/component.js");

        Assert.assertFalse(
                "Provider shouldn't handle request for non-custom element name",
                provider.synchronizedHandleRequest(session, request, response));

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.html");

        Assert.assertFalse(
                "Provider shouldn't handle html extensions in npm mode",
                provider.synchronizedHandleRequest(session, request, response));
    }

    @Test
    public void webComponentNotPresent_responseReturns404() throws IOException {
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        Mockito.when(request.getServletContext()).thenReturn(servletContext);

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        Assert.assertTrue("Provider should handle web-component request",
                provider.synchronizedHandleRequest(session, request, response));
        Mockito.verify(response).sendError(HttpStatusCode.NOT_FOUND.getCode(),
                "No web component for my-component");
    }

    @Test
    public void webComponentGenerator_responseGetsResult() throws IOException {
        registry = setupConfigurations(MyComponentExporter.class);

        ByteArrayOutputStream out = Mockito.spy(new ByteArrayOutputStream());

        DefaultDeploymentConfiguration configuration = Mockito
                .mock(DefaultDeploymentConfiguration.class);

        Mockito.when(response.getOutputStream()).thenReturn(out);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        Assert.assertTrue("Provider should handle web-component request",
                provider.synchronizedHandleRequest(session, request, response));

        Assert.assertTrue("Response should have Feature Flags updater function",
                out.toString().contains(
                        "window.Vaadin.featureFlagsUpdaters.push((activator) => {"));

        Mockito.verify(response).getOutputStream();
        Mockito.verify(out).write(Mockito.any(), Mockito.anyInt(),
                Mockito.anyInt());

    }

    @Test
    public void providesDifferentGeneratedHTMLForEachExportedComponent()
            throws IOException {
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

        registry = setupConfigurations(MyComponentExporter.class,
                OtherComponentExporter.class);

        ByteArrayOutputStream out = Mockito.mock(ByteArrayOutputStream.class);

        DefaultDeploymentConfiguration configuration = Mockito
                .mock(DefaultDeploymentConfiguration.class);

        Mockito.when(response.getOutputStream()).thenReturn(out);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        Assert.assertTrue("Provider should handle first web-component request",
                provider.synchronizedHandleRequest(session, request, response));

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/other-component.js");
        Assert.assertTrue("Provider should handle second web-component request",
                provider.synchronizedHandleRequest(session, request, response));

        Mockito.verify(response, times(2)).getOutputStream();
        Mockito.verify(out, times(2)).write(captor.capture(), Mockito.anyInt(),
                Mockito.anyInt());

        byte[] first = captor.getAllValues().get(0);
        byte[] second = captor.getAllValues().get(1);

        Assert.assertNotEquals("Stream output should not match", first, second);
    }

    @Test(expected = IllegalStateException.class)
    public void setExporters_exportersHasVariousPushes_throws() {
        WebComponentConfigurationRegistry registry = setupConfigurations(
                ThemedComponentExporter.class,
                AnotherPushComponentExporter.class);
    }

    @Test
    public void setExporters_exportersHasOnePush_pushIsSet() {
        WebComponentConfigurationRegistry registry = setupConfigurations(
                ThemedComponentExporter.class, MyComponentExporter.class);
        Assert.assertTrue(registry.getEmbeddedApplicationAnnotation(Push.class)
                .isPresent());
    }

    @Test
    public void setExporters_exportersHasSamePushDeclarations_pushIsSet() {
        WebComponentConfigurationRegistry registry = setupConfigurations(
                ThemedComponentExporter.class,
                SameThemedComponentExporter.class);
        Assert.assertTrue(registry.getEmbeddedApplicationAnnotation(Push.class)
                .isPresent());
        Assert.assertEquals(PushMode.AUTOMATIC, registry
                .getEmbeddedApplicationAnnotation(Push.class).get().value());
    }

    @Test
    public void canHandleRequest_hasNoWebComponentConfigPathIsWebComponentUI_returnsFalse() {
        WebComponentProvider handler = new WebComponentProvider();

        VaadinRequest request = mockRequest(false);
        Assert.assertFalse(handler.canHandleRequest(request));
    }

    @Test
    public void canHandleRequest_hasWebComponentConfigPathIsWebComponentUI_returnsTrue() {
        WebComponentProvider handler = new WebComponentProvider();

        VaadinRequest request = mockRequest(true);
        Assert.assertTrue(handler.canHandleRequest(request));
    }

    private VaadinRequest mockRequest(boolean hasConfig) {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(service.getContext()).thenReturn(context);

        WebComponentConfigurationRegistry registry = Mockito
                .mock(WebComponentConfigurationRegistry.class);
        Mockito.when(context.getAttribute(
                Mockito.eq(WebComponentConfigurationRegistry.class),
                Mockito.any())).thenReturn(registry);
        Mockito.when(registry.hasConfigurations()).thenReturn(hasConfig);

        Mockito.when(request.getPathInfo()).thenReturn("/web-component/a-b.js");

        return request;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @SafeVarargs
    private WebComponentConfigurationRegistry setupConfigurations(
            Class<? extends WebComponentExporter<? extends Component>>... exporters) {
        WebComponentConfigurationRegistry registry = setUpRegistry();

        final Set<Class<? extends WebComponentExporter<? extends Component>>> set = Stream
                .of(exporters).collect(Collectors.toSet());

        WebComponentExporter.WebComponentConfigurationFactory factory = new WebComponentExporter.WebComponentConfigurationFactory();

        Set<WebComponentConfiguration<? extends Component>> configurations = new HashSet<>();
        for (Class<? extends WebComponentExporter<? extends Component>> exporter : exporters)
            configurations.add(factory.create(
                    new DefaultWebComponentExporterFactory(exporter).create()));
        registry.setConfigurations(configurations);

        return registry;
    }

    private WebComponentConfigurationRegistry setUpRegistry() {
        return new WebComponentConfigurationRegistry() {
        };
    }

    @Tag("my-component")
    public static class MyComponent extends Component {
    }

    public static class MyComponentExporter
            extends WebComponentExporter<MyComponent> {

        public MyComponentExporter() {
            super("my-component");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    @Tag("another-component")
    public static class OtherComponent extends Component {
    }

    public static class OtherComponentExporter
            extends WebComponentExporter<OtherComponent> {

        public OtherComponentExporter() {
            super("other-component");
        }

        @Override
        public void configureInstance(WebComponent<OtherComponent> webComponent,
                OtherComponent component) {

        }
    }

    @Push
    public static class ThemedComponentExporter
            extends WebComponentExporter<Component> {
        public ThemedComponentExporter() {
            super("foo");
        }

        @Override
        public void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }
    }

    @Push(value = PushMode.AUTOMATIC)
    public static class SameThemedComponentExporter
            extends WebComponentExporter<Component> {
        public SameThemedComponentExporter() {
            super("foo");
        }

        @Override
        public void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }
    }

    @Push(value = PushMode.DISABLED)
    public static class AnotherPushComponentExporter
            extends WebComponentExporter<Component> {
        public AnotherPushComponentExporter() {
            super("foo-bar");
        }

        @Override
        public void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }
    }

}
