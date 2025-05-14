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

import org.springframework.web.util.WebUtils;

/**
 * Persists the signed and serialized JWT using a pair of cookies:
 * "jwt.headerAndPayload" (JS-readable), and "jwt.signature" (HTTP-only).
 */
class SerializedJwtSplitCookieRepository {
    private static final String JWT_HEADER_AND_PAYLOAD_COOKIE_NAME = "jwt.headerAndPayload";
    private static final String JWT_SIGNATURE_COOKIE_NAME = "jwt.signature";

    private long expiresIn = 1800L;

    SerializedJwtSplitCookieRepository() {
    }

    /**
     * Sets max-age limit for cookies.
     *
     * @param expiresIn
     *            max age (seconds), the default is 30 min
     */
    void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Reads the serialized JWT from the request cookies.
     *
     * @param request
     *            the request to read the token from
     * @return serialized token
     */
    String loadSerializedJwt(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        Cookie jwtHeaderAndPayload = WebUtils.getCookie(request,
                JWT_HEADER_AND_PAYLOAD_COOKIE_NAME);
        if (jwtHeaderAndPayload == null) {
            return null;
        }

        Cookie jwtSignature = WebUtils.getCookie(request,
                JWT_SIGNATURE_COOKIE_NAME);
        if (jwtSignature == null) {
            return null;
        }

        return jwtHeaderAndPayload.getValue() + "." + jwtSignature.getValue();
    }

    /**
     * Saves the serialized JWT string using response cookies. If the serialized
     * JWT is null, the cookies are removed.
     *
     * @param serializedJwt
     *            the serialized JWT
     * @param request
     *            the request
     * @param response
     *            the response to send the cookies
     */
    void saveSerializedJwt(String serializedJwt, HttpServletRequest request,
            HttpServletResponse response) {
        if (serializedJwt == null) {
            this.removeJwtSplitCookies(request, response);
        } else {
            this.setJwtSplitCookies(serializedJwt, request, response);
        }
    }

    static boolean containsCookie(HttpServletResponse response) {
        return response.getHeaders("Set-Cookie").stream()
                .anyMatch(cookie -> cookie
                        .startsWith(JWT_HEADER_AND_PAYLOAD_COOKIE_NAME));
    }

    /**
     * Checks the presence of JWT cookies in request.
     *
     * @param request
     *            request for checking
     * @return true when both the JWT cookies are present
     */
    boolean containsSerializedJwt(HttpServletRequest request) {
        Cookie jwtHeaderAndPayload = WebUtils.getCookie(request,
                JWT_HEADER_AND_PAYLOAD_COOKIE_NAME);
        Cookie jwtSignature = WebUtils.getCookie(request,
                JWT_SIGNATURE_COOKIE_NAME);
        return (jwtHeaderAndPayload != null) && (jwtSignature != null);
    }

    private void setJwtSplitCookies(String serializedJwt,
            HttpServletRequest request, HttpServletResponse response) {
        final String[] parts = serializedJwt.split("\\.");
        final String jwtHeaderAndPayload = parts[0] + "." + parts[1];
        final String jwtSignature = parts[2];

        Cookie headerAndPayload = new Cookie(JWT_HEADER_AND_PAYLOAD_COOKIE_NAME,
                jwtHeaderAndPayload);
        headerAndPayload.setHttpOnly(false);
        headerAndPayload.setSecure(request.isSecure());
        headerAndPayload.setPath(getRequestContextPath(request));
        headerAndPayload.setMaxAge((int) expiresIn - 1);
        response.addCookie(headerAndPayload);

        Cookie signature = new Cookie(JWT_SIGNATURE_COOKIE_NAME, jwtSignature);
        signature.setHttpOnly(true);
        signature.setSecure(request.isSecure());
        signature.setPath(getRequestContextPath(request));
        signature.setMaxAge((int) expiresIn - 1);
        response.addCookie(signature);
    }

    private void removeJwtSplitCookies(HttpServletRequest request,
            HttpServletResponse response) {

        // No need to send JWT cookies with max-age 0 if the current request
        // does not contain them
        if (containsSerializedJwt(request)) {
            Cookie jwtHeaderAndPayloadRemove = new Cookie(
                    JWT_HEADER_AND_PAYLOAD_COOKIE_NAME, null);
            jwtHeaderAndPayloadRemove.setPath(getRequestContextPath(request));
            jwtHeaderAndPayloadRemove.setMaxAge(0);
            jwtHeaderAndPayloadRemove.setSecure(request.isSecure());
            jwtHeaderAndPayloadRemove.setHttpOnly(false);
            response.addCookie(jwtHeaderAndPayloadRemove);

            Cookie jwtSignatureRemove = new Cookie(JWT_SIGNATURE_COOKIE_NAME,
                    null);
            jwtSignatureRemove.setPath(getRequestContextPath(request));
            jwtSignatureRemove.setMaxAge(0);
            jwtSignatureRemove.setSecure(request.isSecure());
            jwtSignatureRemove.setHttpOnly(true);
            response.addCookie(jwtSignatureRemove);
        }
    }

    private String getRequestContextPath(HttpServletRequest request) {
        final String contextPath = request.getContextPath();
        return "".equals(contextPath) ? "/" : contextPath;
    }
}
