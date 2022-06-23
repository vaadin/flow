/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
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
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_STATISTICS_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class WebComponentBootstrapHandlerWebpackTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private VaadinContext context;
    private VaadinServletService service;
    private Lookup lookup;

    @Before
    public void before() throws IOException {
        ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);
        context = Mockito.mock(VaadinContext.class);

        lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);

        File propertiesDir = temporaryFolder.newFolder();
        FileUtils.write(
                new File(propertiesDir, FeatureFlags.PROPERTIES_FILENAME),
                "com.vaadin.experimental.webpackForFrontendBuild=true\n",
                StandardCharsets.UTF_8);

        Mockito.when(context.getAttribute(Mockito.eq(ApplicationConfiguration.class),
                Mockito.any(Supplier.class))).thenReturn(configuration);
        Mockito.when(configuration.getJavaResourceFolder()).thenReturn(propertiesDir);

        service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return context;
            }
        };
    }

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

        MatcherAssert.assertThat(resultingScript,
                CoreMatchers.containsString("var i=1;"));
        MatcherAssert.assertThat(resultingScript, CoreMatchers.not(CoreMatchers
                .containsString("body {height:100vh;width:100vw;margin:0;}")));
        MatcherAssert.assertThat(resultingScript,
                CoreMatchers.not(CoreMatchers.containsString("http-equiv")));
    }

    @Test
    public void writeBootstrapPage_scriptSrcHasNoDoubleQuotes_attributeIsTransferred()
            throws IOException {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Element head = new Document("").normalise().head();

        Element script = head.ownerDocument().createElement("script");
        head.appendChild(script);
        script.attr("src", "foo'bar%20%27?baz%22");

        VaadinResponse response = getMockResponse(stream);
        handler.writeBootstrapPage("", response, head, "");

        String resultingScript = stream.toString(StandardCharsets.UTF_8.name());
        MatcherAssert.assertThat(resultingScript,
                CoreMatchers.containsString("foo'bar%20%27?baz%22"));
    }

    @Test(expected = IllegalStateException.class)
    public void writeBootstrapPage_scriptSrcHasDoubleQuotes_throws()
            throws IOException {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Element head = new Document("").normalise().head();

        Element script = head.ownerDocument().createElement("script");
        head.appendChild(script);
        script.attr("src", "foo\"");

        VaadinResponse response = getMockResponse(stream);
        handler.writeBootstrapPage("", response, head, "");
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

        VaadinServletService service = new MockVaadinServletService() {
            @Override
            protected PwaRegistry getPwaRegistry() {
                return registry;
            };

            @Override
            public VaadinContext getContext() {
                return context;
            }
        };

        initLookup();

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
        MatcherAssert.assertThat(result,
                CoreMatchers.not(CoreMatchers.containsString("bar")));
        MatcherAssert.assertThat(result,
                CoreMatchers.not(CoreMatchers.containsString("h1")));
        MatcherAssert.assertThat(result,
                CoreMatchers.not(CoreMatchers.containsString("baz")));
    }

    @Test
    public void writeBootstrapPage_scriptGuardedAndDevToolsDisabled()
            throws IOException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();

        initLookup();

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

        int scriptIndex = result.indexOf("var hasScript = function(src)");
        Assert.assertTrue(scriptIndex >= 0);

        int guardIndex = result.indexOf(
                "if (!hasScript(\"/VAADIN/build/vaadin-export-2222.cache.js\")) {");
        Assert.assertTrue(guardIndex > scriptIndex);

        int createScriptIndex = result
                .indexOf("document.createElement('script')");
        Assert.assertTrue(createScriptIndex > guardIndex);

        Assert.assertTrue(result.contains("\\\"devToolsEnabled\\\": false"));
    }

    @Test
    public void writeBootstrapPage_spepe() throws Exception {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Element head = new Document("").normalise().head();

        VaadinResponse response = getMockResponse(stream);
        handler.writeBootstrapPage("", response, head, "");

        String resultingScript = stream.toString(StandardCharsets.UTF_8.name());
    }

    @Test
    public void canHandleRequest_hasNoWebComponentConfigPathIsWebComponentUI_returnsFalse() {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        VaadinRequest request = mockRequest(false);
        Assert.assertFalse(handler.canHandleRequest(request));
    }

    @Test
    public void canHandleRequest_hasWebComponentConfigPathIsWebComponentUI_returnsTrue() {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        VaadinRequest request = mockRequest(true);
        Assert.assertTrue(handler.canHandleRequest(request));
    }

    @Test
    public void writeBootstrapPage_withExportChunk()
            throws IOException, ServiceException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();

        initLookup();

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
        Assert.assertTrue(
                result.contains("VAADIN/build/vaadin-export-2222.cache.js"));
        Assert.assertFalse(
                result.contains("VAADIN/build/vaadin-bundle-1111.cache.js"));
    }

    @Test
    public void writeBootstrapPage_noExportChunk()
            throws IOException, ServiceException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();

        initLookup();

        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        session.setConfiguration(service.getDeploymentConfiguration());
        MockDeploymentConfiguration config = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        config.setApplicationOrSystemProperty(SERVLET_PARAMETER_STATISTICS_JSON,
                VAADIN_SERVLET_RESOURCES + "config/stats_no_export.json");
        config.setEnableDevServer(false);

        VaadinServletRequest request = Mockito.mock(VaadinServletRequest.class);
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(request.getServletPath()).thenReturn("/");
        VaadinResponse response = getMockResponse(null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(stream);

        handler.synchronizedHandleRequest(session, request, response);

        // no "export" chunk, expect "bundle" in result instead
        String result = stream.toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue(
                result.contains("VAADIN/build/vaadin-bundle-1111.cache.js"));
    }

    private VaadinRequest mockRequest(boolean hasConfig) {
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

        Mockito.when(request.getPathInfo())
                .thenReturn("/web-component/web-component-ui.js");

        return request;
    }

    private void initLookup() throws IOException {
        ResourceProvider provider = Mockito.mock(ResourceProvider.class);

        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(provider);

        Mockito.when(provider.getApplicationResource(Mockito.anyString()))
                .thenAnswer(answer -> WebComponentBootstrapHandlerWebpackTest.class
                        .getClassLoader().getResource(answer.getArgument(0)));

        Mockito.when(provider.getClientResourceAsStream(
                "META-INF/resources/" + ApplicationConstants.CLIENT_ENGINE_PATH
                        + "/compile.properties"))
                .thenAnswer(invocation -> new ByteArrayInputStream(
                        "jsFile=foo".getBytes(StandardCharsets.UTF_8)));
    }

    private VaadinResponse getMockResponse(ByteArrayOutputStream stream)
            throws IOException {
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(response.getOutputStream()).thenReturn(stream);
        Mockito.when(response.getService()).thenReturn(service);
        Mockito.when(service.getContext()).thenReturn(context);
        Mockito.when(context.getAttribute(
                eq(WebComponentConfigurationRegistry.class), any())).thenReturn(
                        Mockito.mock(WebComponentConfigurationRegistry.class));
        return response;
    }

}
