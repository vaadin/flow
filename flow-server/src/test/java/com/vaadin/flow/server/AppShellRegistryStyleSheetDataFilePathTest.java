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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.shared.ApplicationConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppShellRegistryStyleSheetDataFilePathTest {

    @StyleSheet("/absolute.css")
    @StyleSheet("./relative/path.css")
    @StyleSheet("context://from-context.css")
    @StyleSheet("https://cdn.example.com/remote.css")
    public static class MyShell implements AppShellConfigurator {
    }

    private MockServletServiceSessionSetup mocks;
    private VaadinServletContext context;
    private Document document;

    @BeforeEach
    void setup() {
        mocks = new MockServletServiceSessionSetup();
        context = new VaadinServletContext(mocks.getServletContext());
        // Set current service for potential instantiation
        VaadinService.setCurrent(mocks.getService());
        document = Document.createShell("");
    }

    @AfterEach
    void teardown() throws Exception {
        AppShellRegistry.getInstance(context).reset();
        mocks.cleanup();
    }

    @Test
    void modifyIndex_addsDataFilePathAttributes_normalized() throws Exception {
        // Register our shell class directly in the registry
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(MyShell.class);

        // Use context path to test context:// normalization
        VaadinServletRequest request = createRequest("/", "/ctx");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        assertEquals(4, links.size());

        // The href values are servlet-relative (resolved through <base> by
        // the browser). With the test request's empty servletPath, the
        // context:// prefix expands to "./".

        // 1) Absolute path: href preserved, data-file-path drops leading '/'
        Element abs = links.get(0);
        assertEquals("/absolute.css", abs.attr("href"));
        assertEquals("absolute.css", abs.attr("data-file-path"));

        // 2) Relative with './': href is servlet-relative,
        // data-file-path drops './'
        Element rel = links.get(1);
        assertEquals("./relative/path.css", rel.attr("href"));
        assertEquals("relative/path.css", rel.attr("data-file-path"));

        // 3) context:// expands to servlet-relative path; data-file-path
        // strips the context protocol prefix
        Element ctx = links.get(2);
        assertEquals("./from-context.css", ctx.attr("href"));
        assertEquals("from-context.css", ctx.attr("data-file-path"));

        // 4) Remote http(s) URL unchanged, data-file-path remains original
        Element remote = links.get(3);
        assertEquals("https://cdn.example.com/remote.css", remote.attr("href"));
        assertEquals("https://cdn.example.com/remote.css",
                remote.attr("data-file-path"));
    }

    @Test
    void productionMode_hrefContainsHash_dataFilePathUnchanged()
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
        assertEquals(4, links.size());

        Pattern hashPattern = Pattern
                .compile("\\?" + ApplicationConstants.CONTENT_HASH_PARAMETER
                        + "=[0-9a-f]{8}$");

        // In production mode, data-file-path uses the original annotation
        // value (no stripping like dev mode does)

        // 1) Absolute path: href has hash appended, data-file-path unchanged
        Element abs = links.get(0);
        assertTrue(hashPattern.matcher(abs.attr("href")).find(),
                "Absolute href should contain hash parameter");
        assertTrue(abs.attr("href").startsWith("/absolute.css"),
                "Absolute href should start with /absolute.css");
        assertEquals("/absolute.css", abs.attr("data-file-path"));

        // 2) Relative path: href is servlet-relative, hash appended,
        // data-file-path unchanged
        Element rel = links.get(1);
        assertTrue(hashPattern.matcher(rel.attr("href")).find(),
                "Relative href should contain hash parameter");
        assertTrue(rel.attr("href").startsWith("./relative/path.css"),
                "Relative href should start with ./");
        assertEquals("./relative/path.css", rel.attr("data-file-path"));

        // 3) Context path: href is servlet-relative (context:// expanded),
        // hash appended, data-file-path unchanged
        Element ctx = links.get(2);
        assertTrue(hashPattern.matcher(ctx.attr("href")).find(),
                "Context href should contain hash parameter");
        assertTrue(ctx.attr("href").startsWith("./from-context.css"),
                "Context href should start with ./");
        assertEquals("context://from-context.css", ctx.attr("data-file-path"));

        // 4) External URL: no hash appended, data-file-path unchanged
        Element remote = links.get(3);
        assertEquals("https://cdn.example.com/remote.css", remote.attr("href"));
        assertFalse(hashPattern.matcher(remote.attr("href")).find(),
                "External href should not have hash");
        assertEquals("https://cdn.example.com/remote.css",
                remote.attr("data-file-path"));
    }

    @Test
    void modifyIndex_customServletMapping_hrefIsServletRelative()
            throws Exception {
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(MyShell.class);

        // Servlet mapped to "/myservlet/*", context path "/ctx".
        // contextRootRelativePath becomes "./../" so relative and
        // context:// hrefs must step one level up from the servlet path.
        VaadinServletRequest request = createRequest("/", "/ctx", "/myservlet");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        assertEquals(4, links.size());

        // Absolute path remains unchanged
        assertEquals("/absolute.css", links.get(0).attr("href"));
        // Relative href steps up out of the servlet path
        assertEquals("./../relative/path.css", links.get(1).attr("href"));
        // context:// expands the same way
        assertEquals("./../from-context.css", links.get(2).attr("href"));
        // Remote URL untouched
        assertEquals("https://cdn.example.com/remote.css",
                links.get(3).attr("href"));
    }

    @Test
    void productionMode_missingResource_fallsBackToOriginalUrl()
            throws Exception {
        mocks.getDeploymentConfiguration().setProductionMode(true);

        // Do NOT register any resources — getResourceAsStream returns null

        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(MyShell.class);

        VaadinServletRequest request = createRequest("/", "/ctx");
        registry.modifyIndexHtml(document, request);

        List<Element> links = document.head().select("link[rel=stylesheet]");
        // The links should still be present, just without hash
        for (Element link : links) {
            assertFalse(link.attr("href").contains(
                    "?" + ApplicationConstants.CONTENT_HASH_PARAMETER + "="),
                    "Missing resource href should not have hash: "
                            + link.attr("href"));
        }
    }

    private VaadinServletRequest createRequest(String pathInfo,
            String contextPath) {
        return createRequest(pathInfo, contextPath, "");
    }

    private VaadinServletRequest createRequest(String pathInfo,
            String contextPath, String servletPath) {
        jakarta.servlet.http.HttpServletRequest req = Mockito
                .mock(jakarta.servlet.http.HttpServletRequest.class);
        Mockito.when(req.getServletPath()).thenReturn(servletPath);
        Mockito.when(req.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(req.getRequestURL())
                .thenReturn(new StringBuffer(pathInfo));
        Mockito.when(req.getContextPath()).thenReturn(contextPath);
        return new VaadinServletRequest(req, mocks.getService());
    }
}
