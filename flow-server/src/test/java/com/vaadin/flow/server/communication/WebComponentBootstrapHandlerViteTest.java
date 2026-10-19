/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
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
import com.vaadin.tests.util.TestUtil;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_STATISTICS_JSON;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class WebComponentBootstrapHandlerViteTest {
    @TempDir
    Path temporaryFolder;
    private File projectRootFolder;

    private DeploymentConfiguration deploymentConfiguration;

    @BeforeEach
    void init() throws IOException {
        projectRootFolder = Files.createTempDirectory(temporaryFolder, "temp")
                .toFile();
        TestUtil.createWebComponentHtmlStub(projectRootFolder);
        TestUtil.createStatsJsonStub(projectRootFolder);
        deploymentConfiguration = mock();
        when(deploymentConfiguration.isProductionMode()).thenReturn(true);
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
    void writeBootstrapPage_skipMetaAndStyleHeaderElements()
            throws IOException {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Element head = new Document("").head();
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

        assertThat(resultingScript, containsString("var i=1;"));
        assertThat(resultingScript, not(
                containsString("body {height:100vh;width:100vw;margin:0;}")));
        assertThat(resultingScript, not(containsString("http-equiv")));
    }

    @Test
    void writeBootstrapPage_scriptSrcHasNoDoubleQuotes_attributeIsTransferred()
            throws IOException {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Element head = new Document("").head();

        Element script = head.ownerDocument().createElement("script");
        head.appendChild(script);
        script.attr("src", "foo'bar%20%27?baz%22");

        VaadinResponse response = getMockResponse(stream);
        handler.writeBootstrapPage("", response, head, "");

        String resultingScript = stream.toString(StandardCharsets.UTF_8.name());
        assertThat(resultingScript, containsString("foo'bar%20%27?baz%22"));
    }

    @Test
    void writeBootstrapPage_scriptSrcHasDoubleQuotes_throws()
            throws IOException {
        assertThrows(IllegalStateException.class, () -> {
            WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            Element head = new Document("").head();

            Element script = head.ownerDocument().createElement("script");
            head.appendChild(script);
            script.attr("src", "foo\"");

            VaadinResponse response = getMockResponse(stream);
            handler.writeBootstrapPage("", response, head, "");
        });
    }

    @Test
    void writeBootstrapPage_noPWA() throws IOException, ServiceException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();

        PwaRegistry registry = mock(PwaRegistry.class);

        PwaConfiguration conf = mock(PwaConfiguration.class);

        when(registry.getPwaConfiguration()).thenReturn(conf);

        when(conf.isEnabled()).thenReturn(true);

        when(conf.getManifestPath()).thenReturn("bar");

        PwaIcon icon = mock(PwaIcon.class);
        when(icon.asElement()).thenReturn(new Element("h1"));

        when(registry.getHeaderIcons())
                .thenReturn(Collections.singletonList(icon));

        VaadinServletService service = new MockVaadinServletService() {
            @Override
            protected PwaRegistry getPwaRegistry() {
                return registry;
            };

            @Override
            protected void instrumentMockLookup(Lookup lookup) {
                initLookup(lookup);
            }
        };

        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        MockDeploymentConfiguration config = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        config.setProjectFolder(projectRootFolder);

        VaadinServletRequest request = mock(VaadinServletRequest.class);
        when(request.getService()).thenReturn(service);
        when(request.getServletPath()).thenReturn("/");
        when(request.getMethod()).thenReturn("GET");
        VaadinResponse response = getMockResponse(null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(stream);

        handler.synchronizedHandleRequest(session, request, response);

        String result = stream.toString(StandardCharsets.UTF_8.name());
        assertThat(result, not(containsString("bar")));
        assertThat(result, not(containsString("h1")));
        assertThat(result, not(containsString("baz")));
    }

    @Test
    void writeBootstrapPage_devToolsDisabled()
            throws IOException, ServiceException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();
        VaadinServletService service = new MockVaadinServletService() {
            @Override
            protected void instrumentMockLookup(Lookup lookup) {
                initLookup(lookup);
            }
        };

        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        MockDeploymentConfiguration config = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        config.setProjectFolder(projectRootFolder);

        VaadinServletRequest request = mock(VaadinServletRequest.class);
        when(request.getService()).thenReturn(service);
        when(request.getServletPath()).thenReturn("/");
        when(request.getMethod()).thenReturn("GET");
        VaadinResponse response = getMockResponse(null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(stream);

        handler.synchronizedHandleRequest(session, request, response);

        String result = stream.toString(StandardCharsets.UTF_8.name());

        int scriptIndex = result.indexOf("var hasScript = function(src)");
        assertTrue(scriptIndex >= 0);

        assertTrue(result.contains("\\\"devToolsEnabled\\\" : false"));
    }

    @Test
    void writeBootstrapPage_readyScriptInlined()
            throws IOException, ServiceException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();
        VaadinServletService service = new MockVaadinServletService() {
            @Override
            protected void instrumentMockLookup(Lookup lookup) {
                initLookup(lookup);
            }
        };

        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        MockDeploymentConfiguration config = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        config.setProjectFolder(projectRootFolder);

        VaadinServletRequest request = mock(VaadinServletRequest.class);
        when(request.getService()).thenReturn(service);
        when(request.getServletPath()).thenReturn("/");
        when(request.getMethod()).thenReturn("GET");
        VaadinResponse response = getMockResponse(null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(stream);

        handler.synchronizedHandleRequest(session, request, response);

        String result = stream.toString(StandardCharsets.UTF_8.name());

        assertThat(result, containsString("window.Vaadin.Flow.ready = "));
        assertThat(result, not(containsString("{{READY}}")));
    }

    @Test
    void writeBootstrapPage_spepe() throws Exception {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Element head = new Document("").head();

        VaadinResponse response = getMockResponse(stream);
        handler.writeBootstrapPage("", response, head, "");

        String resultingScript = stream.toString(StandardCharsets.UTF_8.name());
    }

    @Test
    void canHandleRequest_hasNoWebComponentConfigPathIsWebComponentUI_returnsFalse() {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        VaadinRequest request = mockRequest(false);
        assertFalse(handler.canHandleRequest(request));
    }

    @Test
    void canHandleRequest_hasWebComponentConfigPathIsWebComponentUI_returnsTrue() {
        WebComponentBootstrapHandler handler = new WebComponentBootstrapHandler();

        VaadinRequest request = mockRequest(true);
        assertTrue(handler.canHandleRequest(request));
    }

    @Disabled
    @Test
    void writeBootstrapPage_withExportChunk()
            throws IOException, ServiceException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();
        VaadinServletService service = new MockVaadinServletService() {
            @Override
            protected void instrumentMockLookup(Lookup lookup) {
                initLookup(lookup);
            }
        };

        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        MockDeploymentConfiguration config = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        config.setProjectFolder(projectRootFolder);

        VaadinServletRequest request = mock(VaadinServletRequest.class);
        when(request.getService()).thenReturn(service);
        when(request.getServletPath()).thenReturn("/");
        VaadinResponse response = getMockResponse(null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(stream);

        handler.synchronizedHandleRequest(session, request, response);

        String result = stream.toString(StandardCharsets.UTF_8.name());
        assertTrue(result.contains("VAADIN/build/vaadin-export-2222.cache.js"));
        assertFalse(
                result.contains("VAADIN/build/vaadin-bundle-1111.cache.js"));
    }

    @Disabled
    @Test
    void writeBootstrapPage_noExportChunk()
            throws IOException, ServiceException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();
        VaadinServletService service = new MockVaadinServletService() {
            @Override
            protected void instrumentMockLookup(Lookup lookup) {
                initLookup(lookup);
            }
        };

        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        MockDeploymentConfiguration config = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        config.setApplicationOrSystemProperty(SERVLET_PARAMETER_STATISTICS_JSON,
                VAADIN_SERVLET_RESOURCES + "config/stats_no_export.json");
        config.setProjectFolder(projectRootFolder);

        VaadinServletRequest request = mock(VaadinServletRequest.class);
        when(request.getService()).thenReturn(service);
        when(request.getServletPath()).thenReturn("/");
        VaadinResponse response = getMockResponse(null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(stream);

        handler.synchronizedHandleRequest(session, request, response);

        // no "export" chunk, expect "bundle" in result instead
        String result = stream.toString(StandardCharsets.UTF_8.name());
        assertTrue(result.contains("VAADIN/build/vaadin-bundle-1111.cache.js"));
    }

    @Test
    void usageStatistics() throws IOException {
        TestWebComponentBootstrapHandler handler = new TestWebComponentBootstrapHandler();
        VaadinServletService service = new MockVaadinServletService();
        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        MockDeploymentConfiguration config = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        config.setProjectFolder(projectRootFolder);

        VaadinServletRequest request = mock(VaadinServletRequest.class);
        when(request.getService()).thenReturn(service);
        when(request.getServletPath()).thenReturn("/");
        when(request.getMethod()).thenReturn("GET");
        VaadinResponse response = getMockResponse(null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(stream);

        UsageStatistics.removeEntry(Constants.STATISTICS_EXPORTED_WC);

        handler.synchronizedHandleRequest(session, request, response);

        assertTrue(UsageStatistics.getEntries().anyMatch(entry -> entry
                .getName().equals(Constants.STATISTICS_EXPORTED_WC)));
    }

    private VaadinRequest mockRequest(boolean hasConfig) {
        VaadinContext context = mock(VaadinContext.class);
        VaadinService service = mock(VaadinService.class);
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getService()).thenReturn(service);
        when(service.getContext()).thenReturn(context);

        WebComponentConfigurationRegistry registry = mock(
                WebComponentConfigurationRegistry.class);
        when(context.getAttribute(eq(WebComponentConfigurationRegistry.class),
                any())).thenReturn(registry);
        when(registry.hasConfigurations()).thenReturn(hasConfig);

        when(request.getPathInfo())
                .thenReturn("/web-component/web-component-ui.js");

        return request;
    }

    private void initLookup(Lookup lookup) {
        reset(lookup);
        ResourceProvider provider = mock(ResourceProvider.class);

        when(lookup.lookup(ResourceProvider.class)).thenReturn(provider);

        when(provider.getApplicationResource(anyString()))
                .thenAnswer(answer -> WebComponentBootstrapHandlerViteTest.class
                        .getClassLoader().getResource(answer.getArgument(0)));
    }

    private VaadinResponse getMockResponse(ByteArrayOutputStream stream)
            throws IOException {
        VaadinResponse response = mock(VaadinResponse.class);
        VaadinService service = mock(VaadinService.class);
        VaadinContext context = mock(VaadinContext.class);
        when(response.getOutputStream()).thenReturn(stream);
        when(response.getService()).thenReturn(service);
        when(service.getContext()).thenReturn(context);
        when(service.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);
        when(context.getAttribute(eq(WebComponentConfigurationRegistry.class),
                any()))
                .thenReturn(mock(WebComponentConfigurationRegistry.class));
        return response;
    }

}
