/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;

class WebIconsRequestMatcherTest {

    AppShellRegistry shellRegistry;

    VaadinService vaadinService;

    @BeforeEach
    void setup() {
        ServletContext servletContext = new MockServletContext();
        vaadinService = Mockito.mock(VaadinService.class);
        Mockito.when(vaadinService.getContext())
                .thenReturn(new VaadinServletContext(servletContext));
        Mockito.when(vaadinService.getInstantiator())
                .thenReturn(new DefaultInstantiator(vaadinService));
        shellRegistry = AppShellRegistry
                .getInstance(vaadinService.getContext());
        shellRegistry.setShell(CustomIcons.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customFavIcon_matched(String urlMapping) {
        assertRequestMatching("/my/fav.ico", urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customFavIcon_defaultPath_notMatched(String urlMapping) {
        assertRequestNotMatching("/favicon.ico", urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void noFavIconCustomization_defaultFavIcon_notMatched(String urlMapping) {
        shellRegistry.reset();
        shellRegistry.setShell(NoIcons.class);
        assertRequestNotMatching("/favicon.ico", urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void defaultFavIcon_notMatched(String urlMapping) {
        shellRegistry.reset();
        shellRegistry.setShell(DefaultFavicon.class);
        assertRequestNotMatching("/favicon.ico", urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customFavIcon_absolutePath_matched(String urlMapping) {
        shellRegistry.reset();
        shellRegistry.setShell(AbsoluteFavicon.class);
        assertAbsolutePathRequestMatching("/my/custom/icon.png", urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customPWAIcon_matched(String urlMapping) {
        // base icon is served from context root
        assertAbsolutePathRequestMatching("/" + CustomIcons.ICON_PATH,
                urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customPWAIcon_variants_matched(String urlMapping) {
        HandlerHelper.getIconVariants(CustomIcons.ICON_PATH).forEach(
                iconPath -> assertRequestMatching(iconPath, urlMapping));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customPWAIcon_defaultIcon_notMatched(String urlMapping) {
        assertRequestNotMatching("/" + PwaConfiguration.DEFAULT_ICON,
                urlMapping);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void customPWAIcon_defaultIconVariants_notMatched(String urlMapping) {
        HandlerHelper.getIconVariants(PwaConfiguration.DEFAULT_ICON).forEach(
                iconPath -> assertRequestNotMatching(iconPath, urlMapping));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/*", "/ui/*" })
    void npPWA_defaultIcon_notMatched(String urlMapping) {
        shellRegistry.reset();
        shellRegistry.setShell(NoIcons.class);
        assertRequestNotMatching("/" + PwaConfiguration.DEFAULT_ICON,
                urlMapping);
        HandlerHelper.getIconVariants(PwaConfiguration.DEFAULT_ICON).forEach(
                iconPath -> assertRequestNotMatching(iconPath, urlMapping));
    }

    private void assertAbsolutePathRequestMatching(String requestPath,
            String urlMapping) {
        WebIconsRequestMatcher matcher = new WebIconsRequestMatcher(
                vaadinService, urlMapping);
        Assertions.assertTrue(
                matcher.matches(createRequest(requestPath, urlMapping, true)),
                "Expecting '" + requestPath + "' to be matched, but was not");
    }

    private void assertRequestMatching(String requestPath, String urlMapping) {
        WebIconsRequestMatcher matcher = new WebIconsRequestMatcher(
                vaadinService, urlMapping);
        Assertions.assertTrue(
                matcher.matches(createRequest(requestPath, urlMapping, false)),
                "Expecting '" + requestPath + "' to be matched, but was not");
    }

    private void assertRequestNotMatching(String requestPath,
            String urlMapping) {
        WebIconsRequestMatcher matcher = new WebIconsRequestMatcher(
                vaadinService, urlMapping);
        Assertions.assertFalse(
                matcher.matches(createRequest(requestPath, urlMapping, false)),
                "Expecting '" + requestPath + "' not to be matched, but was");
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

    @PWA(name = "app", shortName = "app", iconPath = CustomIcons.ICON_PATH)
    public static class CustomIcons implements AppShellConfigurator {

        public static final String ICON_PATH = "my/custom/icon.png";

        @Override
        public void configurePage(AppShellSettings settings) {
            settings.addFavIcon("icon", "my/fav.ico", "192x192");
        }
    }

    public static class NoIcons implements AppShellConfigurator {

    }

    public static class DefaultFavicon implements AppShellConfigurator {
        @Override
        public void configurePage(AppShellSettings settings) {
            settings.addFavIcon("icon", "/favicon.ico", "192x192");
        }
    }

    public static class AbsoluteFavicon implements AppShellConfigurator {
        @Override
        public void configurePage(AppShellSettings settings) {
            settings.addFavIcon("icon", "/my/custom/icon.png", "192x192");
        }
    }

}
