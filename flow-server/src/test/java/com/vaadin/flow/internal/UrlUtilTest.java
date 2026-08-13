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
package com.vaadin.flow.internal;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class UrlUtilTest {

    private String encodeURIShouldNotBeEscaped = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789;,/?:@&=+$-_.!~*'()#";
    private String encodeURIComponentShouldNotBeEscaped = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";

    @Test
    void isExternal_URLStartsWithTwoSlashes_returnsTrue() {
        assertTrue(UrlUtil.isExternal("//foo"));
    }

    @Test
    void isExternal_URLContainsAnySchemaAsPrefix_returnsTrue() {
        assertTrue(UrlUtil.isExternal("http://foo"));
        assertTrue(UrlUtil.isExternal("https://foo"));
        assertTrue(UrlUtil.isExternal("context://foo"));
        assertTrue(UrlUtil.isExternal("base://foo"));
    }

    @Test
    void isExternal_URLDoesnotContainSchema_returnsFalse() {
        assertFalse(UrlUtil.isExternal("foo"));
    }

    @Test
    void plusAndSpaceHandledCorrectly() {
        assertEquals("Plus+Spa%20+%20ce", UrlUtil.encodeURI("Plus+Spa + ce"));
        assertEquals("Plus%2BSpa%20%2B%20ce",
                UrlUtil.encodeURIComponent("Plus+Spa + ce"));
    }

    @Test
    void encodeURI_shouldNotBeEscaped() {
        assertEquals(encodeURIShouldNotBeEscaped,
                UrlUtil.encodeURI(encodeURIShouldNotBeEscaped));
    }

    @Test
    void encodeURI_mustBeEscaped() {
        for (char c = 0; c < 255; c++) {
            String s = String.valueOf(c);
            if (encodeURIShouldNotBeEscaped.contains(s)) {
                continue;
            }
            assertNotEquals(UrlUtil.encodeURI(s), s);
        }
    }

    @Test
    void encodeURIComponent_shouldNotBeEscaped() {
        assertEquals(encodeURIComponentShouldNotBeEscaped, UrlUtil
                .encodeURIComponent(encodeURIComponentShouldNotBeEscaped));
    }

    @Test
    void encodeURIComponent_mustBeEscaped() {
        for (char c = 0; c < 255; c++) {
            String s = String.valueOf(c);
            if (encodeURIComponentShouldNotBeEscaped.contains(s)) {
                continue;
            }
            assertNotEquals(UrlUtil.encodeURIComponent(s), s);
        }
    }

    @Test
    void getServletPathRelative() {
        assertEquals(".", UrlUtil.getServletPathRelative("/foo/bar/",
                createRequest("/foo", "/bar")));
        assertEquals(".", UrlUtil.getServletPathRelative("/foo/bar",
                createRequest("/foo", "/bar")));
        assertEquals("..", UrlUtil.getServletPathRelative("/foo/",
                createRequest("/foo", "/bar")));
        assertEquals("../..", UrlUtil.getServletPathRelative("/",
                createRequest("/foo", "/bar")));
        assertEquals("..", UrlUtil.getServletPathRelative("/foo",
                createRequest("/foo", "/bar")));
        assertEquals("../../login", UrlUtil.getServletPathRelative("/login",
                createRequest("/foo", "/bar")));
        assertEquals("../login", UrlUtil.getServletPathRelative("/foo/login",
                createRequest("/foo", "/bar")));
        assertEquals("login", UrlUtil.getServletPathRelative("/foo/bar/login",
                createRequest("/foo", "/bar")));
        assertEquals("baz/login", UrlUtil.getServletPathRelative(
                "/foo/bar/baz/login", createRequest("/foo", "/bar")));
    }

    private VaadinServletRequest createRequest(String contextPath,
            String servletPath) {
        if (!servletPath.equals("") && !servletPath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "A servlet path always starts with / except for the empty mapping \"\"");
        }
        if (!contextPath.equals("") && (!contextPath.startsWith("/")
                || contextPath.endsWith("/"))) {
            throw new IllegalArgumentException(
                    "A context path is either empty or starts, but not ends with, a slash");
        }
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn(servletPath);
        Mockito.when(request.getContextPath()).thenReturn(contextPath);
        return new VaadinServletRequest(request,
                Mockito.mock(VaadinServletService.class));
    }

    @Test
    void decodeURIComponent_percentEncodedSpace_decoded() {
        String result = UrlUtil.decodeURIComponent("test%20file.txt");
        assertEquals("test file.txt", result);
    }

    @Test
    void decodeURIComponent_plusSign_notDecodedAsSpace() {
        // Plus signs should remain as plus signs (RFC 3986, not HTML form
        // encoding)
        String result = UrlUtil.decodeURIComponent("test+file.txt");
        assertEquals("test+file.txt", result);
    }

    @Test
    void decodeURIComponent_encodedPlusSign_decoded() {
        String result = UrlUtil.decodeURIComponent("test%2Bfile.txt");
        assertEquals("test+file.txt", result);
    }

    @Test
    void decodeURIComponent_unicodeCharacters_decoded() {
        // åäö.txt encoded as UTF-8 percent-encoded
        String result = UrlUtil.decodeURIComponent("%C3%A5%C3%A4%C3%B6.txt");
        assertEquals("åäö.txt", result);
    }

    @Test
    void decodeURIComponent_specialCharacters_decoded() {
        String result = UrlUtil.decodeURIComponent("special%26%3Dchars.txt");
        assertEquals("special&=chars.txt", result);
    }

    @Test
    void decodeURIComponent_nullValue_returnsNull() {
        String result = UrlUtil.decodeURIComponent(null);
        assertNull(result);
    }

    @Test
    void decodeURIComponent_emptyValue_returnsEmpty() {
        String result = UrlUtil.decodeURIComponent("");
        assertEquals("", result);
    }

    @Test
    void decodeURIComponent_noEncodedChars_returnsSame() {
        String result = UrlUtil.decodeURIComponent("simple.txt");
        assertEquals("simple.txt", result);
    }

    @Test
    void appendQueryParameter_noExistingParams_usesQuestionMark() {
        String result = UrlUtil.appendQueryParameter("/styles.css", "v-c",
                "abcd1234");
        assertEquals("/styles.css?v-c=abcd1234", result);
    }

    @Test
    void appendQueryParameter_existingParams_usesAmpersand() {
        String result = UrlUtil.appendQueryParameter("/styles.css?theme=dark",
                "v-c", "abcd1234");
        assertEquals("/styles.css?theme=dark&v-c=abcd1234", result);
    }

    @Test
    void appendQueryParameter_nullValue_returnsOriginalUrl() {
        String result = UrlUtil.appendQueryParameter("/styles.css", "v-c",
                null);
        assertEquals("/styles.css", result);
    }

    @Test
    void isSafeUrl_safeScheme_returnsTrue() {
        assertTrue(UrlUtil.isSafeUrl("https://vaadin.com",
                Constants.DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_unsafeScheme_returnsFalse() {
        assertFalse(UrlUtil.isSafeUrl("javascript:alert(1)",
                Constants.DEFAULT_URL_SAFE_SCHEMES));
        assertFalse(UrlUtil.isSafeUrl("data:text/html,<script>",
                Constants.DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_schemeMatchIsCaseInsensitive_returnsFalse() {
        assertFalse(UrlUtil.isSafeUrl("JavaScript:alert(1)",
                Constants.DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_relativeUrl_returnsTrue() {
        assertTrue(UrlUtil.isSafeUrl("/path/to/view",
                Constants.DEFAULT_URL_SAFE_SCHEMES));
        assertTrue(
                UrlUtil.isSafeUrl("foo", Constants.DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_relativeUrlWithSpecialCharacters_returnsTrue() {
        // A strict URI parser would reject this, but it is a valid relative URL
        assertTrue(UrlUtil.isSafeUrl("/search?q=a b&x=[1]",
                Constants.DEFAULT_URL_SAFE_SCHEMES));
        // A colon in the path must not be mistaken for a scheme separator
        assertTrue(UrlUtil.isSafeUrl("/path:with:colon",
                Constants.DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_emptyOrBlank_returnsTrue() {
        assertTrue(UrlUtil.isSafeUrl("", Constants.DEFAULT_URL_SAFE_SCHEMES));
        assertTrue(
                UrlUtil.isSafeUrl("   ", Constants.DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_null_returnsFalse() {
        assertFalse(
                UrlUtil.isSafeUrl(null, Constants.DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_controlCharacterObfuscation_returnsFalse() {
        assertFalse(UrlUtil.isSafeUrl("java\tscript:alert(1)",
                Constants.DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_wildcard_allowsAnyScheme() {
        assertTrue(UrlUtil.isSafeUrl("javascript:alert(1)",
                Set.of(Constants.URL_SAFE_SCHEMES_WILDCARD)));
    }

    @Test
    void isSafeUrl_configuredSchemes_replaceDefaults() {
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(config.getUrlSafeSchemes())
                .thenReturn(Set.of("custom", "https"));
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);

        try (MockedStatic<VaadinService> mock = Mockito
                .mockStatic(VaadinService.class)) {
            mock.when(VaadinService::getCurrent).thenReturn(service);
            assertTrue(UrlUtil.isSafeUrl("custom:foo"));
            assertFalse(UrlUtil.isSafeUrl("mailto:a@b.com"));
            assertFalse(UrlUtil.isSafeUrl("javascript:alert(1)"));
        }
    }

    @Test
    void validateUrl_attachedComponentWithUnsafeUrl_throwsImmediately() {
        TestComponent component = new TestComponent();
        createUiWithSafeUrlSchemes("https").add(component);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UrlUtil.validateUrl(component, "href",
                        "http://example.com", "setUnsafeHref(String)"));

        assertTrue(exception.getMessage().contains("http://example.com"));
        assertTrue(exception.getMessage().contains("setUnsafeHref(String)"));
    }

    @Test
    void validateUrl_attachedComponentWithSchemeAllowedByConfiguration_doesNotThrow() {
        TestComponent component = new TestComponent();
        createUiWithSafeUrlSchemes("custom").add(component);

        UrlUtil.validateUrl(component, "href", "custom:foo",
                "setUnsafeHref(String)");
    }

    @Test
    void validateUrl_detachedComponent_notCheckedAgainstDefaults() {
        TestComponent component = new TestComponent();

        // The application may allow schemes that the framework default doesn't,
        // so nothing is rejected before the configuration is known
        UrlUtil.validateUrl(component, "href", "custom:foo",
                "setUnsafeHref(String)",
                () -> fail("The value should not be cleared"));

        createUiWithSafeUrlSchemes("custom").add(component);
    }

    @Test
    void validateUrl_detachedComponentWithCurrentService_currentServiceNotUsed() {
        TestComponent component = new TestComponent();
        VaadinService service = createService("https");

        try (MockedStatic<VaadinService> mock = Mockito
                .mockStatic(VaadinService.class)) {
            mock.when(VaadinService::getCurrent).thenReturn(service);
            // The component may end up in an application other than the one
            // that happens to be current while the value is being set
            UrlUtil.validateUrl(component, "href", "custom:foo",
                    "setUnsafeHref(String)",
                    () -> fail("The value should not be cleared"));
        }

        createUiWithSafeUrlSchemes("custom").add(component);
    }

    @Test
    void validateUrl_unsafeInOwnUi_clearsValueAndThrowsOnAttach() {
        TestComponent component = new TestComponent();
        AtomicBoolean cleared = new AtomicBoolean();
        UrlUtil.validateUrl(component, "href", "http://example.com",
                "setUnsafeHref(String)", () -> cleared.set(true));

        UI ui = createUiWithSafeUrlSchemes("https");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> ui.add(component));

        assertTrue(exception.getMessage().contains("http://example.com"));
        assertTrue(exception.getMessage().contains("setUnsafeHref(String)"));
        assertTrue(cleared.get(),
                "The value should be cleared before the exception is thrown");
    }

    @Test
    void validateUrl_safeInOwnUi_attachSucceeds() {
        TestComponent component = new TestComponent();
        AtomicBoolean cleared = new AtomicBoolean();
        UrlUtil.validateUrl(component, "href", "https://example.com",
                "setUnsafeHref(String)", () -> cleared.set(true));

        createUiWithSafeUrlSchemes("https").add(component);

        assertFalse(cleared.get());
    }

    @Test
    void validateUrl_deferredTwice_onlyLatestValueValidated() {
        TestComponent component = new TestComponent();
        AtomicBoolean cleared = new AtomicBoolean();
        UrlUtil.validateUrl(component, "href", "http://example.com",
                "setUnsafeHref(String)", () -> cleared.set(true));
        UrlUtil.validateUrl(component, "href", "https://example.com",
                "setUnsafeHref(String)", () -> cleared.set(true));

        createUiWithSafeUrlSchemes("https").add(component);

        assertFalse(cleared.get());
    }

    @Test
    void cancelUrlValidation_beforeAttach_notValidated() {
        TestComponent component = new TestComponent();
        AtomicBoolean cleared = new AtomicBoolean();
        UrlUtil.validateUrl(component, "href", "http://example.com",
                "setUnsafeHref(String)", () -> cleared.set(true));

        UrlUtil.cancelUrlValidation(component, "href");
        createUiWithSafeUrlSchemes("https").add(component);

        assertFalse(cleared.get());
    }

    @Test
    void cancelUrlValidation_afterAttachAndWithoutScheduling_doesNotFail() {
        TestComponent component = new TestComponent();
        UrlUtil.cancelUrlValidation(component, "href");

        UrlUtil.validateUrl(component, "href", "https://example.com",
                "setUnsafeHref(String)", () -> {
                });
        createUiWithSafeUrlSchemes("https").add(component);

        // The listener has already removed itself through the attach event
        UrlUtil.cancelUrlValidation(component, "href");
    }

    private static UI createUiWithSafeUrlSchemes(String safeUrlSchemes) {
        UI ui = new MockUI(
                new AlwaysLockedVaadinSession(createService(safeUrlSchemes)));
        // The interesting scenarios are the ones where the configuration has to
        // be found through the UI rather than through the current instances
        CurrentInstance.clearAll();
        return ui;
    }

    private static VaadinService createService(String safeUrlSchemes) {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setApplicationOrSystemProperty(
                InitParameters.URL_SAFE_SCHEMES, safeUrlSchemes);
        VaadinService service = new MockVaadinServletService(configuration);
        CurrentInstance.clearAll();
        return service;
    }

    @Tag("div")
    private static class TestComponent extends Component {
    }

    @Test
    void isSafeUrl_configuredWildcard_allowsAnyScheme() {
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(config.getUrlSafeSchemes())
                .thenReturn(Set.of(Constants.URL_SAFE_SCHEMES_WILDCARD));
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);

        try (MockedStatic<VaadinService> mock = Mockito
                .mockStatic(VaadinService.class)) {
            mock.when(VaadinService::getCurrent).thenReturn(service);
            assertTrue(UrlUtil.isSafeUrl("javascript:alert(1)"));
        }
    }
}
