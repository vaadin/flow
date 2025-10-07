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

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.HandlerHelper.RequestType;

public class HandlerHelperTest {

    private HttpServletRequest createRequest(String pathInfo,
            RequestType type) {
        return createRequest(pathInfo,
                type == null ? null : type.getIdentifier());
    }

    private HttpServletRequest createRequest(String pathInfo,
            String typeString) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        if ("".equals(pathInfo)) {
            pathInfo = null;
        }

        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(request.getParameter("v-r")).thenReturn(typeString);
        return request;
    }

    @Test
    public void isFrameworkInternalRequest_validType_nullPathInfo() {
        HttpServletRequest request = createRequest(null, RequestType.INIT);

        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
    }

    @Test
    public void isFrameworkInternalRequest_validType_emptyPathinfo() {
        HttpServletRequest request = createRequest("", RequestType.INIT);

        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));
    }

    @Test
    public void isFrameworkInternalRequest_validType_slashPathinfo() {
        // This is how requests to /vaadinServlet/ are interpreted
        HttpServletRequest request = createRequest("/", RequestType.INIT);

        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));
    }

    @Test
    public void isFrameworkInternalRequest_unknownType() {
        HttpServletRequest request = createRequest(null, "unknown");

        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));

    }

    @Test
    public void isFrameworkInternalRequest_noType() {
        HttpServletRequest request = createRequest(null, (RequestType) null);

        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));

    }

    @Test
    public void isFrameworkInternalRequest_validType_withPath() {
        HttpServletRequest request = createRequest("hello", RequestType.INIT);

        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/hello", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/hello/*", request));
    }

    @Test
    public void isFrameworkInternalRequest_validType_withServletMappingAndPath() {
        HttpServletRequest request = createRequest("", RequestType.INIT);
        Mockito.when(request.getServletPath()).thenReturn("/servlet");

        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/servlet", request));
    }

    @Test
    public void isFrameworkInternalRequest_noType_withServletMappingAndPath() {
        HttpServletRequest request = createRequest("/", (RequestType) null);
        Mockito.when(request.getServletPath()).thenReturn("/servlet");

        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/servlet", request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_servletRoot() {
        VaadinRequest request = createVaadinRequest("", "/*", RequestType.INIT);

        Assert.assertTrue(BootstrapHandler.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_servletRoot_noType() {
        VaadinRequest request = createVaadinRequest("", "/*", null);

        Assert.assertFalse(
                BootstrapHandler.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_pathInsideServlet() {
        VaadinRequest request = createVaadinRequest("/foo", "/*",
                RequestType.INIT);

        Assert.assertFalse(
                BootstrapHandler.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_pathInsideServlet_noType() {
        VaadinRequest request = createVaadinRequest("/foo", "/*", null);

        Assert.assertFalse(
                BootstrapHandler.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_nonRootServlet() {
        VaadinRequest request = createVaadinRequest("", "/myservlet/",
                RequestType.INIT);

        Assert.assertTrue(BootstrapHandler.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_nonRootServlet_pathInsideServlet() {
        VaadinRequest request = createVaadinRequest("/hello", "/myservlet",
                null);

        Assert.assertFalse(
                BootstrapHandler.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_uploadUrl() {
        VaadinServletRequest request = createVaadinRequest(
                "VAADIN/dynamic/resource/1/e83d6b6d-2b75-4960-8922-5431f4a23e49/upload",
                "", null);

        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest("/*",
                request.getHttpServletRequest()));
    }

    @Test
    public void isFrameworkInternalRequest_dynamicResourceUrl_withoutPostfix() {
        // Test for ElementRequestHandler requests without URL postfix
        VaadinServletRequest request = createVaadinRequest(
                "VAADIN/dynamic/resource/1/e83d6b6d-2b75-4960-8922-5431f4a23e49/",
                "", null);

        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest("/*",
                request.getHttpServletRequest()));
    }

    @Test
    public void isFrameworkInternalRequest_dynamicResourceUrl_withCustomPostfix() {
        // Test for ElementRequestHandler requests with custom postfix
        VaadinServletRequest request = createVaadinRequest(
                "VAADIN/dynamic/resource/1/e83d6b6d-2b75-4960-8922-5431f4a23e49/custom.pdf",
                "", null);

        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest("/*",
                request.getHttpServletRequest()));
    }

    @Test
    public void isFrameworkInternalRequest_hillaPushUrl() {
        VaadinServletRequest request = createVaadinRequest("HILLA/push", "",
                null);

        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest("/*",
                request.getHttpServletRequest()));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinServletMapping_hillaPushUrl() {
        VaadinServletRequest request = createVaadinRequest("HILLA/push", "",
                null);

        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest("/ui/*",
                request.getHttpServletRequest()));
    }

    @Test
    public void isFrameworkInternalRequest_flowPushUrl() {
        VaadinServletRequest request = createVaadinRequest("VAADIN/push", "",
                RequestType.PUSH);

        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest("/*",
                request.getHttpServletRequest()));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinServletMapping_flowPushUrl() {
        VaadinServletRequest request = createVaadinRequest("/VAADIN/push",
                "/ui", RequestType.PUSH);

        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest("/ui/*",
                request.getHttpServletRequest()));
    }

    @Test
    public void isFrameworkInternalRequest_fakeUploadUrl() {
        VaadinServletRequest request = createVaadinRequest(
                "VAADIN/dynamic/resource/../../../upload", "", null);

        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest("/*",
                request.getHttpServletRequest()));
    }

    @Test
    public void isFrameworkInternalRequest_staticFileUrl() {
        VaadinServletRequest request = createVaadinRequest(
                "VAADIN/static/file.png", "", null);

        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest("/*",
                request.getHttpServletRequest()));
    }

    @Test
    public void getPathIfInsideServlet_default_servlet() {
        String servletMapping = "/*";
        Assert.assertEquals(Optional.of(""),
                HandlerHelper.getPathIfInsideServlet(servletMapping, ""));
        Assert.assertEquals(Optional.of("/"),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/"));
        Assert.assertEquals(Optional.of("foo"),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "foo"));
        Assert.assertEquals(Optional.of("/foo"),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/foo"));
    }

    @Test
    public void getPathIfInsideServlet_root_only_servlet() {
        String servletMapping = "";
        Assert.assertEquals(Optional.of(""),
                HandlerHelper.getPathIfInsideServlet(servletMapping, ""));
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/"));
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "foo"));
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/foo"));
    }

    @Test
    public void getPathIfInsideServlet_all_urls_servlet() {
        String servletMapping = "/";
        Assert.assertEquals(Optional.of(""),
                HandlerHelper.getPathIfInsideServlet(servletMapping, ""));
        Assert.assertEquals(Optional.of("/"),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/"));
        Assert.assertEquals(Optional.of("foo"),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "foo"));
        Assert.assertEquals(Optional.of("/foo"),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/foo"));
    }

    @Test
    public void getPathIfInsideServlet_sevlet_using_single_path() {
        String servletMapping = "/foo";
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, ""));
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "bar"));
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/bar"));
        Assert.assertEquals(Optional.of(""),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "foo"));
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "foo/"));
        Assert.assertEquals(Optional.empty(), HandlerHelper
                .getPathIfInsideServlet(servletMapping, "foo/bar"));
    }

    @Test
    public void getPathIfInsideServlet_sevlet_with_context_path() {
        String servletMapping = "/foo/*";
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, ""));
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "bar"));
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/bar"));
        Assert.assertEquals(Optional.of(""),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "foo"));
        Assert.assertEquals(Optional.of(""),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "foo/"));
        Assert.assertEquals(Optional.of(""),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/foo"));
        Assert.assertEquals(Optional.empty(),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/foos"));
        Assert.assertEquals(Optional.empty(), HandlerHelper
                .getPathIfInsideServlet(servletMapping, "/foos/bar"));
        Assert.assertEquals(Optional.of(""),
                HandlerHelper.getPathIfInsideServlet(servletMapping, "/foo/"));
        Assert.assertEquals(Optional.of("bar"), HandlerHelper
                .getPathIfInsideServlet(servletMapping, "/foo/bar"));
    }

    private VaadinServletRequest createVaadinRequest(String requestPath,
            String servletPath, RequestType type) {
        HttpServletRequest servletRequest = createRequest(requestPath, type);
        if (servletPath.equals("/*")) {
            // This is what the spec says
            // HttpServletRequest#getServletPath
            servletPath = "";
        }
        Mockito.when(servletRequest.getServletPath()).thenReturn(servletPath);
        return new VaadinServletRequest(servletRequest,
                Mockito.mock(VaadinServletService.class));
    }

    @Test
    public void publicResources() {
        Set<String> expected = new HashSet<>();
        expected.add("/manifest.webmanifest");
        expected.add("/sw.js");
        expected.add("/sw-runtime-resources-precache.js");
        expected.add("/offline.html");
        expected.add("/offline-stub.html");
        expected.add("/icons/icon.png");
        expected.add("/icons/icon-144x144.png");
        expected.add("/icons/icon-192x192.png");
        expected.add("/icons/icon-512x512.png");
        expected.add("/icons/icon-16x16.png");
        expected.add("/icons/icon-32x32.png");
        expected.add("/icons/icon-96x96.png");
        expected.add("/icons/icon-180x180.png");
        expected.add("/icons/icon-2048x2732.png");
        expected.add("/icons/icon-2732x2048.png");
        expected.add("/icons/icon-1668x2388.png");
        expected.add("/icons/icon-2388x1668.png");
        expected.add("/icons/icon-1668x2224.png");
        expected.add("/icons/icon-2224x1668.png");
        expected.add("/icons/icon-1620x2160.png");
        expected.add("/icons/icon-2160x1620.png");
        expected.add("/icons/icon-1536x2048.png");
        expected.add("/icons/icon-2048x1536.png");
        expected.add("/icons/icon-1284x2778.png");
        expected.add("/icons/icon-2778x1284.png");
        expected.add("/icons/icon-1170x2532.png");
        expected.add("/icons/icon-2532x1170.png");
        expected.add("/icons/icon-1125x2436.png");
        expected.add("/icons/icon-2436x1125.png");
        expected.add("/icons/icon-1242x2688.png");
        expected.add("/icons/icon-2688x1242.png");
        expected.add("/icons/icon-828x1792.png");
        expected.add("/icons/icon-1792x828.png");
        expected.add("/icons/icon-1242x2208.png");
        expected.add("/icons/icon-2208x1242.png");
        expected.add("/icons/icon-750x1334.png");
        expected.add("/icons/icon-1334x750.png");
        expected.add("/icons/icon-640x1136.png");
        expected.add("/icons/icon-1136x640.png");
        expected.add("/themes/**");

        Set<String> actual = new HashSet<>();
        Collections.addAll(actual, HandlerHelper.getPublicResources());
        Assert.assertEquals(expected, actual);

        Set<String> expectedRoot = Set.of("/favicon.ico");

        Set<String> actualRoot = new HashSet<>();
        Collections.addAll(actualRoot, HandlerHelper.getPublicResourcesRoot());
        Assert.assertEquals(expectedRoot, actualRoot);
    }
}