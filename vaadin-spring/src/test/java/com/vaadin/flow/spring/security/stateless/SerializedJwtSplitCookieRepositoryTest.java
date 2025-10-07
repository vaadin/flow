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
package com.vaadin.flow.spring.security.stateless;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class SerializedJwtSplitCookieRepositoryTest {
    private static final int DEFAULT_MAX_AGE = 1800;
    private static final int CUSTOM_MAX_AGE = 3600;
    private static final String JWT_HEADER_AND_PAYLOAD = "foo.bar";
    private static final String JWT_SIGNATURE = "baz";
    private static final String JWT = JWT_HEADER_AND_PAYLOAD + "."
            + JWT_SIGNATURE;
    private static final String JWT_SIGNATURE_NAME = "jwt.signature";
    private static final String JWT_HEADER_AND_PAYLOAD_NAME = "jwt.headerAndPayload";
    private static final Cookie JWT_HEADER_AND_PAYLOAD_COOKIE;
    private static final Cookie JWT_SIGNATURE_COOKIE;
    private static final String CONTEXT_PATH = "/context-path/";

    static {
        JWT_HEADER_AND_PAYLOAD_COOKIE = new Cookie(JWT_HEADER_AND_PAYLOAD_NAME,
                JWT_HEADER_AND_PAYLOAD);
        JWT_SIGNATURE_COOKIE = new Cookie(JWT_SIGNATURE_NAME, JWT_SIGNATURE);
    }

    private SerializedJwtSplitCookieRepository serializedJwtSplitCookieRepository;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setup() {
        serializedJwtSplitCookieRepository = new SerializedJwtSplitCookieRepository();
        request = Mockito.mock(HttpServletRequest.class);
        Mockito.doReturn(CONTEXT_PATH).when(request).getContextPath();
        Mockito.doReturn(true).when(request).isSecure();
        response = Mockito.mock(HttpServletResponse.class);
    }

    @Test
    public void containsSerializedJwt_true_when_bothCookiesPreset() {
        Mockito.doReturn(new Cookie[] { JWT_HEADER_AND_PAYLOAD_COOKIE,
                JWT_SIGNATURE_COOKIE }).when(request).getCookies();
        Assert.assertTrue(serializedJwtSplitCookieRepository
                .containsSerializedJwt(request));
    }

    @Test
    public void containsSerializedJwt_false_when_signatureCookieMissing() {
        Mockito.doReturn(new Cookie[] { JWT_HEADER_AND_PAYLOAD_COOKIE })
                .when(request).getCookies();
        Assert.assertFalse(serializedJwtSplitCookieRepository
                .containsSerializedJwt(request));
    }

    @Test
    public void containsSerializedJwt_false_when_headerAndPayloadCookieMissing() {
        Mockito.doReturn(new Cookie[] { JWT_SIGNATURE_COOKIE }).when(request)
                .getCookies();
        Assert.assertFalse(serializedJwtSplitCookieRepository
                .containsSerializedJwt(request));
    }

    @Test
    public void containsSerializedJwt_false_when_bothCookiesMissing() {
        Mockito.doReturn(new Cookie[] {}).when(request).getCookies();
        Assert.assertFalse(serializedJwtSplitCookieRepository
                .containsSerializedJwt(request));
    }

    @Test
    public void containsSerializedJwt_false_when_cookiesNull() {
        Mockito.doReturn(null).when(request).getCookies();
        Assert.assertFalse(serializedJwtSplitCookieRepository
                .containsSerializedJwt(request));
    }

    @Test
    public void loadSerializedJwt_returnsString_when_cookiesPresent() {
        Mockito.doReturn(new Cookie[] { JWT_HEADER_AND_PAYLOAD_COOKIE,
                JWT_SIGNATURE_COOKIE }).when(request).getCookies();

        String serializedJwt = serializedJwtSplitCookieRepository
                .loadSerializedJwt(request);
        Assert.assertEquals(JWT, serializedJwt);
    }

    @Test
    public void loadSerializedJwt_returnsNull_when_headerAndPayloadCookieMissing() {
        Mockito.doReturn(new Cookie[] { JWT_SIGNATURE_COOKIE }).when(request)
                .getCookies();

        String serializedJwt;
        serializedJwt = serializedJwtSplitCookieRepository
                .loadSerializedJwt(request);
        Assert.assertNull(JWT, serializedJwt);
    }

    @Test
    public void loadSerializedJwt_returnsNull_when_signatureCookieMissing() {
        Mockito.doReturn(new Cookie[] { JWT_HEADER_AND_PAYLOAD_COOKIE })
                .when(request).getCookies();

        String serializedJwt;
        serializedJwt = serializedJwtSplitCookieRepository
                .loadSerializedJwt(request);
        Assert.assertNull(JWT, serializedJwt);
    }

    @Test
    public void loadSerializedJwt_returnsNull_when_bothCookiesMissing() {
        Mockito.doReturn(new Cookie[] {}).when(request).getCookies();

        String serializedJwt;
        serializedJwt = serializedJwtSplitCookieRepository
                .loadSerializedJwt(request);
        Assert.assertNull(JWT, serializedJwt);
    }

    @Test
    public void loadSerializedJwt_returnsNull_when_cookiesNull() {
        Mockito.doReturn(null).when(request).getCookies();

        String serializedJwt;
        serializedJwt = serializedJwtSplitCookieRepository
                .loadSerializedJwt(request);
        Assert.assertNull(JWT, serializedJwt);
    }

    @Test
    public void saveSerializedJwt_sets_cookiePair() {
        serializedJwtSplitCookieRepository.saveSerializedJwt(JWT, request,
                response);
        checkResponseCookiePair(JWT_HEADER_AND_PAYLOAD, JWT_SIGNATURE, true,
                DEFAULT_MAX_AGE - 1, CONTEXT_PATH);
    }

    @Test
    public void saveSerializedJwt_unauthenticatedRequest_doNotSet_cookiePair() {
        serializedJwtSplitCookieRepository.saveSerializedJwt(null, request,
                response);
        Mockito.verify(response, Mockito.never())
                .addCookie(ArgumentMatchers.any());
    }

    @Test
    public void saveSerializedJwt_authenticatedRequest_resets_cookiePair() {
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] {
                JWT_SIGNATURE_COOKIE, JWT_HEADER_AND_PAYLOAD_COOKIE });
        serializedJwtSplitCookieRepository.saveSerializedJwt(null, request,
                response);
        checkResponseCookiePair(null, null, true, 0, CONTEXT_PATH);
    }

    @Test
    public void saveSerializedJwt_setsWithMaxAge_after_setExpireIn() {
        serializedJwtSplitCookieRepository.setExpiresIn(CUSTOM_MAX_AGE);
        serializedJwtSplitCookieRepository.saveSerializedJwt(JWT, request,
                response);
        checkResponseCookiePair(JWT_HEADER_AND_PAYLOAD, JWT_SIGNATURE, true,
                CUSTOM_MAX_AGE - 1, CONTEXT_PATH);
    }

    @Test
    public void saveSerializedJwt_resetsWithoutMaxAge_after_setExpireIn() {
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] {
                JWT_SIGNATURE_COOKIE, JWT_HEADER_AND_PAYLOAD_COOKIE });
        serializedJwtSplitCookieRepository.setExpiresIn(CUSTOM_MAX_AGE);
        serializedJwtSplitCookieRepository.saveSerializedJwt(null, request,
                response);
        checkResponseCookiePair(null, null, true, 0, CONTEXT_PATH);
    }

    @Test
    public void saveSerializedJwt_sets_withNonSecure_request() {
        Mockito.doReturn(false).when(request).isSecure();
        serializedJwtSplitCookieRepository.saveSerializedJwt(JWT, request,
                response);
        checkResponseCookiePair(JWT_HEADER_AND_PAYLOAD, JWT_SIGNATURE, false,
                DEFAULT_MAX_AGE - 1, CONTEXT_PATH);
    }

    @Test
    public void saveSerializedJwt_sets_withEmptyContextPath() {
        Mockito.doReturn("").when(request).getContextPath();
        serializedJwtSplitCookieRepository.saveSerializedJwt(JWT, request,
                response);
        checkResponseCookiePair(JWT_HEADER_AND_PAYLOAD, JWT_SIGNATURE, true,
                DEFAULT_MAX_AGE - 1, "/");
    }

    private void checkResponseCookiePair(String expectedHeaderAndPayload,
            String expectedSignature, boolean expectedIsSecure, int maxAge,
            String expectedPath) {
        ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor
                .forClass(Cookie.class);
        Mockito.verify(response, Mockito.times(2))
                .addCookie(cookieArgumentCaptor.capture());
        List<Cookie> cookieList = cookieArgumentCaptor.getAllValues();

        Cookie headerAndPayloadCookie = cookieList.get(0);
        Assert.assertNotNull(headerAndPayloadCookie);
        Assert.assertEquals(JWT_HEADER_AND_PAYLOAD_NAME,
                headerAndPayloadCookie.getName());
        Assert.assertEquals(expectedHeaderAndPayload,
                headerAndPayloadCookie.getValue());
        Assert.assertFalse(headerAndPayloadCookie.isHttpOnly());
        Assert.assertEquals(expectedIsSecure,
                headerAndPayloadCookie.getSecure());
        Assert.assertEquals(expectedPath, headerAndPayloadCookie.getPath());
        Assert.assertEquals(maxAge, headerAndPayloadCookie.getMaxAge());

        Cookie signatureCookie = cookieList.get(1);
        Assert.assertNotNull(signatureCookie);
        Assert.assertEquals(JWT_SIGNATURE_NAME, signatureCookie.getName());
        Assert.assertEquals(expectedSignature, signatureCookie.getValue());
        Assert.assertTrue(signatureCookie.isHttpOnly());
        Assert.assertEquals(expectedIsSecure, signatureCookie.getSecure());
        Assert.assertEquals(expectedPath, signatureCookie.getPath());
        Assert.assertEquals(maxAge, signatureCookie.getMaxAge());
    }
}