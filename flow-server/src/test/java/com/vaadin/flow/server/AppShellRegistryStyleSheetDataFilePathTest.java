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
package com.vaadin.flow.server;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;

public class AppShellRegistryStyleSheetDataFilePathTest {

    @StyleSheet("/absolute.css")
    @StyleSheet("./relative/path.css")
    @StyleSheet("context://from-context.css")
    @StyleSheet("https://cdn.example.com/remote.css")
    public static class MyShell implements AppShellConfigurator {
    }

    private MockServletServiceSessionSetup mocks;
    private VaadinServletContext context;
    private Document document;

    @Before
    public void setup() {
        mocks = new MockServletServiceSessionSetup();
        context = new VaadinServletContext(mocks.getServletContext());
        // Set current service for potential instantiation
        VaadinService.setCurrent(mocks.getService());
        document = Document.createShell("");
    }

    @After
    public void teardown() throws Exception {
        AppShellRegistry.getInstance(context).reset();
        mocks.cleanup();
    }

    @Test
    public void modifyIndex_addsDataFilePathAttributes_normalized()
            throws Exception {
        // Register our shell class directly in the registry
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(MyShell.class);

        // Use context path to test context:// normalization
        VaadinServletRequest request = createRequest("/", "/ctx");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        Assert.assertEquals(4, links.size());

        // 1) Absolute path: href preserved, data-file-path drops leading '/'
        Element abs = links.get(0);
        Assert.assertEquals("/absolute.css", abs.attr("href"));
        Assert.assertEquals("absolute.css", abs.attr("data-file-path"));

        // 2) Relative with './': href resolved with context path,
        // data-file-path drops './'
        Element rel = links.get(1);
        Assert.assertEquals("/ctx/relative/path.css", rel.attr("href"));
        Assert.assertEquals("relative/path.css", rel.attr("data-file-path"));

        // 3) context:// should resolve to context path in href, and
        // data-file-path strips context protocol prefix
        Element ctx = links.get(2);
        Assert.assertEquals("/ctx/from-context.css", ctx.attr("href"));
        Assert.assertEquals("from-context.css", ctx.attr("data-file-path"));

        // 4) Remote http(s) URL unchanged, data-file-path remains original
        Element remote = links.get(3);
        Assert.assertEquals("https://cdn.example.com/remote.css",
                remote.attr("href"));
        Assert.assertEquals("https://cdn.example.com/remote.css",
                remote.attr("data-file-path"));
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
        return new VaadinServletRequest(req, mocks.getService());
    }
}
