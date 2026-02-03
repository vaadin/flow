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
package com.vaadin.flow.server;

import jakarta.servlet.ServletContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;

/**
 * Tests for Aura theme auto-loading behavior in AppShellRegistry.
 */
public class AppShellRegistryAuraAutoLoadTest {

    public static class MyAppShell implements AppShellConfigurator {
    }

    private VaadinServletContext context;
    private Document document;
    private VaadinServletService service;

    @Before
    public void setup() {
        Map<String, Object> attributeMap = new HashMap<>();
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getAttribute(Mockito.anyString()))
                .then(invocation -> attributeMap
                        .get(invocation.getArguments()[0].toString()));
        Mockito.doAnswer(invocation -> attributeMap.put(
                invocation.getArguments()[0].toString(),
                invocation.getArguments()[1])).when(servletContext)
                .setAttribute(Mockito.anyString(), Mockito.any());

        Lookup lookup = Mockito.mock(Lookup.class);
        attributeMap.put(Lookup.class.getName(), lookup);

        context = new VaadinServletContext(servletContext);
        document = Document.createShell("");

        // Create a minimal service mock
        service = Mockito.mock(VaadinServletService.class);
        DeploymentConfiguration deploymentConfig = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(deploymentConfig.isProductionMode()).thenReturn(false);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(deploymentConfig);
        Mockito.when(service.getInstantiator()).thenReturn(
                Mockito.mock(com.vaadin.flow.di.Instantiator.class));
    }

    @After
    public void teardown() {
        AppShellRegistry.getInstance(context).reset();
    }

    @Test
    public void noAppShellConfigurator_auraAvailable_auraIsAutoLoaded() {
        // Do not set any shell class - leave appShellClass as null
        AppShellRegistry registry = AppShellRegistry.getInstance(context);

        // Mock Aura resource availability
        Mockito.when(service.isResourceAvailable("aura/aura.css"))
                .thenReturn(true);

        VaadinServletRequest request = createRequest("/", "");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        Assert.assertEquals("Expected Aura stylesheet to be auto-loaded", 1,
                links.size());

        Element aura = links.get(0);
        Assert.assertEquals("aura/aura.css", aura.attr("data-file-path"));
        Assert.assertTrue("Aura href should contain aura.css",
                aura.attr("href").contains("aura.css"));
    }

    @Test
    public void noAppShellConfigurator_auraNotAvailable_auraNotLoaded() {
        // Do not set any shell class - leave appShellClass as null
        AppShellRegistry registry = AppShellRegistry.getInstance(context);

        // Mock Aura resource NOT available
        Mockito.when(service.isResourceAvailable("aura/aura.css"))
                .thenReturn(false);

        VaadinServletRequest request = createRequest("/", "");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        Assert.assertEquals("Aura should NOT be auto-loaded when not available",
                0, links.size());
    }

    @Test
    public void appShellConfiguratorExists_auraNotAutoLoaded() {
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(MyAppShell.class);

        // Set current service for instantiation in modifyIndexHtml
        VaadinService.setCurrent(service);

        // Configure instantiator to return a new instance of MyAppShell
        com.vaadin.flow.di.Instantiator instantiator = Mockito
                .mock(com.vaadin.flow.di.Instantiator.class);
        Mockito.when(service.getInstantiator()).thenReturn(instantiator);
        Mockito.when(instantiator.getOrCreate(MyAppShell.class))
                .thenReturn(new MyAppShell());

        try {
            VaadinServletRequest request = createRequest("/", "");
            registry.modifyIndexHtml(document, request);

            List<Element> links = document.head()
                    .select("link[rel=stylesheet]");
            // Empty AppShellConfigurator has no stylesheets, but Aura should
            // NOT be auto-added
            Assert.assertEquals(
                    "Aura should NOT be auto-loaded when AppShellConfigurator exists",
                    0, links.size());
        } finally {
            VaadinService.setCurrent(null);
        }
    }

    private VaadinServletRequest createRequest(String pathInfo,
            String contextPath) {
        jakarta.servlet.http.HttpServletRequest req = Mockito
                .mock(jakarta.servlet.http.HttpServletRequest.class);
        Mockito.when(req.getServletPath()).thenReturn("");
        Mockito.when(req.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(req.getRequestURL())
                .thenReturn(new StringBuffer(pathInfo));
        Mockito.when(req.getContextPath()).thenReturn(contextPath);
        return new VaadinServletRequest(req, service);
    }
}
