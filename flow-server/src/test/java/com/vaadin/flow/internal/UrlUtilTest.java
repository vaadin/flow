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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void isSafeUrl_allowedScheme_returnsTrue() {
        assertTrue(UrlUtil.isSafeUrl("https://vaadin.com",
                UrlUtil.DEFAULT_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_disallowedScheme_returnsFalse() {
        assertFalse(UrlUtil.isSafeUrl("javascript:alert(1)",
                UrlUtil.DEFAULT_SAFE_SCHEMES));
        assertFalse(UrlUtil.isSafeUrl("data:text/html,<script>",
                UrlUtil.DEFAULT_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_schemeMatchIsCaseInsensitive_returnsFalse() {
        assertFalse(UrlUtil.isSafeUrl("JavaScript:alert(1)",
                UrlUtil.DEFAULT_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_relativeUrl_returnsTrue() {
        assertTrue(UrlUtil.isSafeUrl("/path/to/view",
                UrlUtil.DEFAULT_SAFE_SCHEMES));
        assertTrue(UrlUtil.isSafeUrl("foo", UrlUtil.DEFAULT_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_relativeUrlWithSpecialCharacters_returnsTrue() {
        // A strict URI parser would reject this, but it is a valid relative URL
        assertTrue(UrlUtil.isSafeUrl("/search?q=a b&x=[1]",
                UrlUtil.DEFAULT_SAFE_SCHEMES));
        // A colon in the path must not be mistaken for a scheme separator
        assertTrue(UrlUtil.isSafeUrl("/path:with:colon",
                UrlUtil.DEFAULT_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_emptyOrBlank_returnsTrue() {
        assertTrue(UrlUtil.isSafeUrl("", UrlUtil.DEFAULT_SAFE_SCHEMES));
        assertTrue(UrlUtil.isSafeUrl("   ", UrlUtil.DEFAULT_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_null_returnsFalse() {
        assertFalse(UrlUtil.isSafeUrl(null, UrlUtil.DEFAULT_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_controlCharacterObfuscation_returnsFalse() {
        assertFalse(UrlUtil.isSafeUrl("java\tscript:alert(1)",
                UrlUtil.DEFAULT_SAFE_SCHEMES));
    }

    @Test
    void isSafeUrl_wildcard_allowsAnyScheme() {
        assertTrue(UrlUtil.isSafeUrl("javascript:alert(1)",
                Set.of(UrlUtil.ALL_SCHEMES_SAFE)));
    }

    @Test
    void parseSafeSchemes_nullOrBlank_returnsDefault() {
        assertEquals(UrlUtil.DEFAULT_SAFE_SCHEMES,
                UrlUtil.parseSafeSchemes(null));
        assertEquals(UrlUtil.DEFAULT_SAFE_SCHEMES,
                UrlUtil.parseSafeSchemes("   "));
    }

    @Test
    void parseSafeSchemes_commaSeparated_isTrimmedAndLowerCased() {
        assertEquals(Set.of("https", "myapp"),
                UrlUtil.parseSafeSchemes(" HTTPS , MyApp "));
    }
}
