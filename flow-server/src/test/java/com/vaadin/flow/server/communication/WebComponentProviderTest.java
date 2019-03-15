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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentBuilder;
import com.vaadin.flow.server.webcomponent.WebComponentBuilderRegistry;

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
        Mockito.when(service.getInstantiator())
                .thenReturn(new MockInstantiator());

        provider = new WebComponentProvider();
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
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        Mockito.when(request.getServletContext()).thenReturn(servletContext);
        Mockito.when(request.getContextPath()).thenReturn("");
        WebComponentBuilderRegistry registry = WebComponentBuilderRegistry
                .getInstance(servletContext);
        final HashMap<String, Class<? extends WebComponentExporter<?
                extends Component>>> map = new HashMap<>();
        map.put("tag", MyComponentExporter.class);
        registry.setExporters(map);
        Mockito.when(servletContext
                .getAttribute(WebComponentBuilderRegistry.class.getName()))
                .thenReturn(registry);

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

    @Tag("my-component")
    public static class MyComponent extends Component {
    }

    @Tag("my-component")
    public static class MyComponentExporter implements WebComponentExporter<MyComponent> {
        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {

        }
    }

}
