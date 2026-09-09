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
package com.vaadin.flow.spring.security;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PwaResourcesRequestMatcherTest {

    AppShellRegistry shellRegistry;

    VaadinService vaadinService;

    @BeforeEach
    void setup() {
        ServletContext servletContext = new MockServletContext();
        vaadinService = mock(VaadinService.class);
        when(vaadinService.getContext())
                .thenReturn(new VaadinServletContext(servletContext));
        when(vaadinService.getInstantiator())
                .thenReturn(new DefaultInstantiator(vaadinService));
        shellRegistry = AppShellRegistry
                .getInstance(vaadinService.getContext());
        shellRegistry.setShell(CustomOffline.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customOfflinePath_matched(String urlMapping) {
        assertRequestMatching("/" + CustomOffline.OFFLINE_PATH, urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void offlineResources_matched(String urlMapping) {
        assertRequestMatching("/offline-assets/logo.txt", urlMapping);
        // Resources declared with a leading slash are requested from the
        // origin root, not through the Vaadin servlet mapping
        assertAbsolutePathRequestMatching("/offline-assets/style.css",
                urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void defaultOfflinePath_notMatched(String urlMapping) {
        shellRegistry.reset();
        shellRegistry.setShell(DefaultOffline.class);
        assertRequestNotMatching("/" + PwaConfiguration.DEFAULT_OFFLINE_PATH,
                urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void noOfflinePath_notMatched(String urlMapping) {
        shellRegistry.reset();
        shellRegistry.setShell(NoOffline.class);
        assertRequestNotMatching("/" + CustomOffline.OFFLINE_PATH, urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void noPwa_notMatched(String urlMapping) {
        shellRegistry.reset();
        shellRegistry.setShell(NoPwa.class);
        assertRequestNotMatching("/" + CustomOffline.OFFLINE_PATH, urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customOfflinePath_notGetRequest_notMatched(String urlMapping) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                RequestUtil.applyUrlMapping(urlMapping,
                        "/" + CustomOffline.OFFLINE_PATH));
        request.setPathInfo("/" + CustomOffline.OFFLINE_PATH);
        request.setServletPath(urlMapping.replaceFirst("/\\*?$", ""));
        Assertions.assertFalse(createMatcher(urlMapping).matches(request),
                "Expecting only GET requests to be matched");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customManifestPath_matched(String urlMapping) {
        shellRegistry.reset();
        shellRegistry.setShell(CustomManifest.class);
        assertRequestMatching("/" + CustomManifest.MANIFEST_PATH, urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void defaultManifestPath_notMatched(String urlMapping) {
        assertRequestNotMatching("/" + PwaConfiguration.DEFAULT_PATH,
                urlMapping);
    }

    @Test
    void unparseablePath_discardedWithoutHidingTheValidOnes() {
        shellRegistry.reset();
        shellRegistry.setShell(UnparseableOffline.class);
        // A path that cannot be parsed must not make the matcher throw on
        // every request, which would take down the whole application
        assertRequestNotMatching("/" + UnparseableOffline.OFFLINE_PATH, "/*");
        // and it must not discard the resources that are usable
        assertRequestMatching("/" + UnparseableOffline.OFFLINE_RESOURCE, "/*");
    }

    private void assertAbsolutePathRequestMatching(String requestPath,
            String urlMapping) {
        Assertions.assertTrue(
                createMatcher(urlMapping)
                        .matches(createRequest(requestPath, urlMapping, true)),
                "Expecting '" + requestPath + "' to be matched, but was not");
    }

    private void assertRequestMatching(String requestPath, String urlMapping) {
        Assertions.assertTrue(
                createMatcher(urlMapping)
                        .matches(createRequest(requestPath, urlMapping, false)),
                "Expecting '" + requestPath + "' to be matched, but was not");
    }

    private void assertRequestNotMatching(String requestPath,
            String urlMapping) {
        Assertions.assertFalse(
                createMatcher(urlMapping)
                        .matches(createRequest(requestPath, urlMapping, false)),
                "Expecting '" + requestPath + "' not to be matched, but was");
    }

    private PwaResourcesRequestMatcher createMatcher(String urlMapping) {
        return new PwaResourcesRequestMatcher(vaadinService, urlMapping);
    }

    private static HttpServletRequest createRequest(String path,
            String urlMapping, boolean absolutePath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                absolutePath ? path
                        : RequestUtil.applyUrlMapping(urlMapping, path));
        request.setPathInfo(path);
        if (!absolutePath) {
            request.setServletPath(urlMapping.replaceFirst("/\\*?$", ""));
        }
        return request;
    }

    @PWA(name = "app", shortName = "app", offlinePath = CustomOffline.OFFLINE_PATH, offlineResources = {
            "offline-assets/logo.txt", "/offline-assets/style.css" })
    public static class CustomOffline implements AppShellConfigurator {

        public static final String OFFLINE_PATH = "custom-offline.html";
    }

    @PWA(name = "app", shortName = "app", offlinePath = PwaConfiguration.DEFAULT_OFFLINE_PATH)
    public static class DefaultOffline implements AppShellConfigurator {
    }

    @PWA(name = "app", shortName = "app")
    public static class NoOffline implements AppShellConfigurator {
    }

    @PWA(name = "app", shortName = "app", manifestPath = CustomManifest.MANIFEST_PATH)
    public static class CustomManifest implements AppShellConfigurator {

        public static final String MANIFEST_PATH = "custom-manifest.webmanifest";
    }

    @PWA(name = "app", shortName = "app", offlinePath = UnparseableOffline.OFFLINE_PATH, offlineResources = {
            UnparseableOffline.OFFLINE_RESOURCE })
    public static class UnparseableOffline implements AppShellConfigurator {

        public static final String OFFLINE_PATH = "offline{.html";

        public static final String OFFLINE_RESOURCE = "offline-assets/logo.txt";
    }

    public static class NoPwa implements AppShellConfigurator {
    }

}
