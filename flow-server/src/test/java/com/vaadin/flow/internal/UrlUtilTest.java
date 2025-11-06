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
package com.vaadin.flow.internal;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;

public class UrlUtilTest {

    private String encodeURIShouldNotBeEscaped = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789;,/?:@&=+$-_.!~*'()#";
    private String encodeURIComponentShouldNotBeEscaped = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";

    @Test
    public void isExternal_URLStartsWithTwoSlashes_returnsTrue() {
        Assert.assertTrue(UrlUtil.isExternal("//foo"));
    }

    @Test
    public void isExternal_URLContainsAnySchemaAsPrefix_returnsTrue() {
        Assert.assertTrue(UrlUtil.isExternal("http://foo"));
        Assert.assertTrue(UrlUtil.isExternal("https://foo"));
        Assert.assertTrue(UrlUtil.isExternal("context://foo"));
        Assert.assertTrue(UrlUtil.isExternal("base://foo"));
    }

    @Test
    public void isExternal_URLDoesnotContainSchema_returnsFalse() {
        Assert.assertFalse(UrlUtil.isExternal("foo"));
    }

    @Test
    public void plusAndSpaceHandledCorrectly() {
        Assert.assertEquals("Plus+Spa%20+%20ce",
                UrlUtil.encodeURI("Plus+Spa + ce"));
        Assert.assertEquals("Plus%2BSpa%20%2B%20ce",
                UrlUtil.encodeURIComponent("Plus+Spa + ce"));
    }

    @Test
    public void encodeURI_shouldNotBeEscaped() {
        Assert.assertEquals(encodeURIShouldNotBeEscaped,
                UrlUtil.encodeURI(encodeURIShouldNotBeEscaped));
    }

    @Test
    public void encodeURI_mustBeEscaped() {
        for (char c = 0; c < 255; c++) {
            String s = String.valueOf(c);
            if (encodeURIShouldNotBeEscaped.contains(s)) {
                continue;
            }
            Assert.assertNotEquals(UrlUtil.encodeURI(s), s);
        }
    }

    @Test
    public void encodeURIComponent_shouldNotBeEscaped() {
        Assert.assertEquals(encodeURIComponentShouldNotBeEscaped, UrlUtil
                .encodeURIComponent(encodeURIComponentShouldNotBeEscaped));
    }

    @Test
    public void encodeURIComponent_mustBeEscaped() {
        for (char c = 0; c < 255; c++) {
            String s = String.valueOf(c);
            if (encodeURIComponentShouldNotBeEscaped.contains(s)) {
                continue;
            }
            Assert.assertNotEquals(UrlUtil.encodeURIComponent(s), s);
        }
    }

    @Test
    public void getServletPathRelative() {
        Assert.assertEquals(".", UrlUtil.getServletPathRelative("/foo/bar/",
                createRequest("/foo", "/bar")));
        Assert.assertEquals(".", UrlUtil.getServletPathRelative("/foo/bar",
                createRequest("/foo", "/bar")));
        Assert.assertEquals("..", UrlUtil.getServletPathRelative("/foo/",
                createRequest("/foo", "/bar")));
        Assert.assertEquals("../..", UrlUtil.getServletPathRelative("/",
                createRequest("/foo", "/bar")));
        Assert.assertEquals("..", UrlUtil.getServletPathRelative("/foo",
                createRequest("/foo", "/bar")));
        Assert.assertEquals("../../login", UrlUtil.getServletPathRelative(
                "/login", createRequest("/foo", "/bar")));
        Assert.assertEquals("../login", UrlUtil.getServletPathRelative(
                "/foo/login", createRequest("/foo", "/bar")));
        Assert.assertEquals("login", UrlUtil.getServletPathRelative(
                "/foo/bar/login", createRequest("/foo", "/bar")));
        Assert.assertEquals("baz/login", UrlUtil.getServletPathRelative(
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
    public void decodeURIComponent_percentEncodedSpace_decoded() {
        String result = UrlUtil.decodeURIComponent("test%20file.txt");
        Assert.assertEquals("test file.txt", result);
    }

    @Test
    public void decodeURIComponent_plusSign_notDecodedAsSpace() {
        // Plus signs should remain as plus signs (RFC 3986, not HTML form
        // encoding)
        String result = UrlUtil.decodeURIComponent("test+file.txt");
        Assert.assertEquals("test+file.txt", result);
    }

    @Test
    public void decodeURIComponent_encodedPlusSign_decoded() {
        String result = UrlUtil.decodeURIComponent("test%2Bfile.txt");
        Assert.assertEquals("test+file.txt", result);
    }

    @Test
    public void decodeURIComponent_unicodeCharacters_decoded() {
        // åäö.txt encoded as UTF-8 percent-encoded
        String result = UrlUtil.decodeURIComponent("%C3%A5%C3%A4%C3%B6.txt");
        Assert.assertEquals("åäö.txt", result);
    }

    @Test
    public void decodeURIComponent_specialCharacters_decoded() {
        String result = UrlUtil.decodeURIComponent("special%26%3Dchars.txt");
        Assert.assertEquals("special&=chars.txt", result);
    }

    @Test
    public void decodeURIComponent_nullValue_returnsNull() {
        String result = UrlUtil.decodeURIComponent(null);
        Assert.assertNull(result);
    }

    @Test
    public void decodeURIComponent_emptyValue_returnsEmpty() {
        String result = UrlUtil.decodeURIComponent("");
        Assert.assertEquals("", result);
    }

    @Test
    public void decodeURIComponent_noEncodedChars_returnsSame() {
        String result = UrlUtil.decodeURIComponent("simple.txt");
        Assert.assertEquals("simple.txt", result);
    }
}
