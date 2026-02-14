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

import java.util.List;
import java.util.regex.Pattern;

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

    @Test
    public void productionMode_hrefContainsHash_dataFilePathUnchanged()
            throws Exception {
        mocks.getDeploymentConfiguration().setProductionMode(true);

        // Register stylesheet resources so the hash can be computed.
        // Paths must match what resolveResource() produces for each
        // annotation value.
        mocks.getServlet().addServletContextResource("/absolute.css",
                "body { color: red; }");
        mocks.getServlet().addServletContextResource("/from-context.css",
                "body { color: blue; }");
        // ./relative/path.css is passed through resolveResource unchanged
        mocks.getServlet().addServletContextResource("./relative/path.css",
                "body { color: green; }");

        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(MyShell.class);

        VaadinServletRequest request = createRequest("/", "/ctx");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        Assert.assertEquals(4, links.size());

        Pattern hashPattern = Pattern.compile("\\?v=[0-9a-f]{8}$");

        // In production mode, data-file-path uses the original annotation
        // value (no stripping like dev mode does)

        // 1) Absolute path: href has hash appended, data-file-path unchanged
        Element abs = links.get(0);
        Assert.assertTrue("Absolute href should contain ?v=<hash>",
                hashPattern.matcher(abs.attr("href")).find());
        Assert.assertTrue("Absolute href should start with /absolute.css",
                abs.attr("href").startsWith("/absolute.css"));
        Assert.assertEquals("/absolute.css", abs.attr("data-file-path"));

        // 2) Relative path: href has hash appended, data-file-path unchanged
        Element rel = links.get(1);
        Assert.assertTrue("Relative href should contain ?v=<hash>",
                hashPattern.matcher(rel.attr("href")).find());
        Assert.assertTrue("Relative href should start with /ctx/",
                rel.attr("href").startsWith("/ctx/relative/path.css"));
        Assert.assertEquals("./relative/path.css", rel.attr("data-file-path"));

        // 3) Context path: href has hash appended, data-file-path unchanged
        Element ctx = links.get(2);
        Assert.assertTrue("Context href should contain ?v=<hash>",
                hashPattern.matcher(ctx.attr("href")).find());
        Assert.assertTrue("Context href should start with /ctx/",
                ctx.attr("href").startsWith("/ctx/from-context.css"));
        Assert.assertEquals("context://from-context.css",
                ctx.attr("data-file-path"));

        // 4) External URL: no hash appended, data-file-path unchanged
        Element remote = links.get(3);
        Assert.assertEquals("https://cdn.example.com/remote.css",
                remote.attr("href"));
        Assert.assertFalse("External href should not have hash",
                hashPattern.matcher(remote.attr("href")).find());
        Assert.assertEquals("https://cdn.example.com/remote.css",
                remote.attr("data-file-path"));
    }

    @Test
    public void productionMode_missingResource_fallsBackToOriginalUrl()
            throws Exception {
        mocks.getDeploymentConfiguration().setProductionMode(true);

        // Do NOT register any resources — getResourceAsStream returns null

        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(MyShell.class);

        VaadinServletRequest request = createRequest("/", "/ctx");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        // The links should still be present, just without ?v= hash
        for (Element link : links) {
            Assert.assertFalse(
                    "Missing resource href should not have hash: "
                            + link.attr("href"),
                    link.attr("href").contains("?v="));
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
        return new VaadinServletRequest(req, mocks.getService());
    }
}
