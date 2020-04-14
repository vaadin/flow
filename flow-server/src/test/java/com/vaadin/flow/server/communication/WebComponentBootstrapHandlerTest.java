/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.hamcrest.CoreMatchers;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.PwaIcon;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

public class WebComponentBootstrapHandlerTest {

    private static class TestWebComponentBootstrapHandler
            extends WebComponentBootstrapHandler {
        @Override
        protected boolean canHandleRequest(VaadinRequest request) {
            return true;
        }

        @Override
        protected String getServiceUrl(VaadinRequest request,
                VaadinResponse response) {
            return "/";
        }
    }

    @Test
    public void writeBootstrapPage_skipMetaAndStyleHeaderElements()
            throws IOException {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Element head = new Document("").normalise().head();
        Element meta = head.ownerDocument().createElement("meta");
        head.appendChild(meta);
        meta.attr("http-equiv", "Content-Type");

        Element style = head.ownerDocument().createElement("style");
        head.appendChild(style);
        style.attr("type", "text/css");
        style.text("body {height:100vh;width:100vw;margin:0;}");

        Element script = head.ownerDocument().createElement("script");
        head.appendChild(script);
        script.text("var i=1;");

        VaadinResponse response = getMockResponse(stream);
        handler.writeBootstrapPage("", response, head, "");

        String resultingScript = stream.toString(StandardCharsets.UTF_8.name());

        Assert.assertThat(resultingScript,
                CoreMatchers.containsString("var i=1;"));
        Assert.assertThat(resultingScript, CoreMatchers.not(CoreMatchers
                .containsString("body {height:100vh;width:100vw;margin:0;}")));
        Assert.assertThat(resultingScript,
                CoreMatchers.not(CoreMatchers.containsString("http-equiv")));
    }

    @Test
    public void writeBootstrapPage_noPWA()
            throws IOException, ServiceException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();

        PwaRegistry registry = Mockito.mock(PwaRegistry.class);

        PwaConfiguration conf = Mockito.mock(PwaConfiguration.class);

        Mockito.when(registry.getPwaConfiguration()).thenReturn(conf);

        Mockito.when(conf.isEnabled()).thenReturn(true);

        Mockito.when(conf.getManifestPath()).thenReturn("bar");

        PwaIcon icon = Mockito.mock(PwaIcon.class);
        Mockito.when(icon.asElement()).thenReturn(new Element("h1"));

        Mockito.when(registry.getHeaderIcons())
                .thenReturn(Collections.singletonList(icon));

        Mockito.when(conf.isInstallPromptEnabled()).thenReturn(true);

        Mockito.when(registry.getInstallPrompt()).thenReturn("baz");

        VaadinServletService service = new MockVaadinServletService() {
            @Override
            protected PwaRegistry getPwaRegistry() {
                return registry;
            };
        };
        service.init();
        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        session.setConfiguration(service.getDeploymentConfiguration());
        MockDeploymentConfiguration config = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        config.setEnableDevServer(false);

        VaadinServletRequest request = Mockito.mock(VaadinServletRequest.class);
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(request.getServletPath()).thenReturn("/");
        VaadinResponse response = getMockResponse(null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(stream);

        handler.synchronizedHandleRequest(session, request, response);

        String result = stream.toString(StandardCharsets.UTF_8.name());
        Assert.assertThat(result,
                CoreMatchers.not(CoreMatchers.containsString("bar")));
        Assert.assertThat(result,
                CoreMatchers.not(CoreMatchers.containsString("h1")));
        Assert.assertThat(result,
                CoreMatchers.not(CoreMatchers.containsString("baz")));
    }

    @Test
    public void writeBootstrapPage_devmodeGizmoIsDisabled()
            throws IOException, ServiceException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();
        VaadinServletService service = new MockVaadinServletService();
        service.init();
        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        session.setConfiguration(service.getDeploymentConfiguration());
        MockDeploymentConfiguration config = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        config.setEnableDevServer(false);

        VaadinServletRequest request = Mockito.mock(VaadinServletRequest.class);
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(request.getServletPath()).thenReturn("/");
        VaadinResponse response = getMockResponse(null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(stream);

        handler.synchronizedHandleRequest(session, request, response);

        String result = stream.toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue(result.contains("\\\"devmodeGizmoEnabled\\\": false"));
    }

    @Test
    public void writeBootstrapPage_spepe()
            throws Exception {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Element head = new Document("").normalise().head();

        VaadinResponse response = getMockResponse(stream);
        handler.writeBootstrapPage("", response, head, "");

        String resultingScript = stream.toString(StandardCharsets.UTF_8.name());


        System.err.println(resultingScript);

    }

    private VaadinResponse getMockResponse(ByteArrayOutputStream stream) throws IOException {
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(response.getOutputStream()).thenReturn(stream);
        Mockito.when(response.getService()).thenReturn(service);
        Mockito.when(service.getContext()).thenReturn(context);
        Mockito.when(context.getAttribute(
                eq(WebComponentConfigurationRegistry.class), any())).thenReturn(
                Mockito.mock(WebComponentConfigurationRegistry.class));
        return response;
    }

}
