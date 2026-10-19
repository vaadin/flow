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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for Aura theme auto-loading behavior in AppShellRegistry.
 */
class AppShellRegistryAuraAutoLoadTest {

    public static class MyAppShell implements AppShellConfigurator {
    }

    private VaadinServletContext context;
    private Document document;
    private VaadinServletService service;

    @BeforeEach
    void setup() {
        Map<String, Object> attributeMap = new HashMap<>();
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getAttribute(anyString()))
                .then(invocation -> attributeMap
                        .get(invocation.getArguments()[0].toString()));
        doAnswer(invocation -> attributeMap.put(
                invocation.getArguments()[0].toString(),
                invocation.getArguments()[1])).when(servletContext)
                .setAttribute(anyString(), any());

        Lookup lookup = mock(Lookup.class);
        attributeMap.put(Lookup.class.getName(), lookup);

        context = new VaadinServletContext(servletContext);
        document = Document.createShell("");

        // Create a minimal service mock
        service = mock(VaadinServletService.class);
        DeploymentConfiguration deploymentConfig = mock(
                DeploymentConfiguration.class);
        when(deploymentConfig.isProductionMode()).thenReturn(false);
        when(service.getDeploymentConfiguration()).thenReturn(deploymentConfig);
        when(service.getInstantiator())
                .thenReturn(mock(com.vaadin.flow.di.Instantiator.class));
        when(service.getContext()).thenReturn(context);
        when(service.getContextRootRelativePath(any())).thenReturn("./");
    }

    @AfterEach
    void teardown() {
        AppShellRegistry.getInstance(context).reset();
    }

    @Test
    void noAppShellConfigurator_auraAvailable_auraIsAutoLoaded() {
        // Do not set any shell class - leave appShellClass as null
        AppShellRegistry registry = AppShellRegistry.getInstance(context);

        // Mock Aura resource availability
        when(service.isResourceAvailable("/aura/aura.css")).thenReturn(true);

        VaadinServletRequest request = createRequest("/", "");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        assertEquals(1, links.size(),
                "Expected Aura stylesheet to be auto-loaded");

        Element aura = links.get(0);
        assertEquals("aura/aura.css", aura.attr("data-file-path"));
        assertTrue(aura.attr("href").contains("aura.css"),
                "Aura href should contain aura.css");
    }

    @Test
    void noAppShellConfigurator_auraNotAvailable_auraNotLoaded() {
        // Do not set any shell class - leave appShellClass as null
        AppShellRegistry registry = AppShellRegistry.getInstance(context);

        // Mock Aura resource NOT available
        when(service.isResourceAvailable("/aura/aura.css")).thenReturn(false);

        VaadinServletRequest request = createRequest("/", "");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        assertEquals(0, links.size(),
                "Aura should NOT be auto-loaded when not available");
    }

    @Test
    void appShellConfiguratorExists_auraNotAutoLoaded() {
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(MyAppShell.class);

        // Set current service for instantiation in modifyIndexHtml
        VaadinService.setCurrent(service);

        // Configure instantiator to return a new instance of MyAppShell
        com.vaadin.flow.di.Instantiator instantiator = mock(
                com.vaadin.flow.di.Instantiator.class);
        when(service.getInstantiator()).thenReturn(instantiator);
        when(instantiator.getOrCreate(MyAppShell.class))
                .thenReturn(new MyAppShell());

        try {
            VaadinServletRequest request = createRequest("/", "");
            registry.modifyIndexHtml(document, request);

            List<Element> links = document.head()
                    .select("link[rel=stylesheet]");
            // Empty AppShellConfigurator has no stylesheets, but Aura should
            // NOT be auto-added
            assertEquals(0, links.size(),
                    "Aura should NOT be auto-loaded when AppShellConfigurator exists");
        } finally {
            VaadinService.setCurrent(null);
        }
    }

    private VaadinServletRequest createRequest(String pathInfo,
            String contextPath) {
        jakarta.servlet.http.HttpServletRequest req = mock(
                jakarta.servlet.http.HttpServletRequest.class);
        when(req.getServletPath()).thenReturn("");
        when(req.getPathInfo()).thenReturn(pathInfo);
        when(req.getRequestURL()).thenReturn(new StringBuffer(pathInfo));
        when(req.getContextPath()).thenReturn(contextPath);
        return new VaadinServletRequest(req, service);
    }
}
