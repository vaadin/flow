/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.server.VaadinRequest;

/**
 * Internal utility class for URL handling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class UrlUtil {

    private UrlUtil() {
    }

    /**
     * checks if the given url is an external URL (e.g. staring with http:// or
     * https://) or not.
     *
     * @param url
     *            is the url to be checked.
     * @return true if the url is external otherwise false.
     */
    public static boolean isExternal(String url) {
        if (url.startsWith("//")) {
            return true;
        }
        return url.contains("://");
    }

    /**
     * Encodes a full URI.
     * <p>
     * Corresponds to encodeURI in JavaScript
     * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURI
     * <p>
     * The path can contain {@code /} and other special URL characters as these
     * will not be encoded. See {@link #encodeURIComponent(String)} if you want
     * to encode all special characters.
     * <p>
     * The following characters are not escaped:
     * {@literal A-Za-z0-9;,/?:@&=+$-_.!~*'()#}
     *
     * @param uri
     *            the uri to encode
     */
    public static String encodeURI(String uri) {
        try {
            return URLEncoder.encode(uri, StandardCharsets.UTF_8.name())
                    .replace("+", "%20").replace("%2F", "/").replace("%40", "@")
                    .replace("%3B", ";").replace("%2C", ",").replace("%3F", "?")
                    .replace("%3A", ":").replace("%26", "&").replace("%3D", "=")
                    .replace("%2B", "+").replace("%24", "$").replace("%21", "!")
                    .replace("%7E", "~").replace("%27", "'").replace("%28", "(")
                    .replace("%29", ")").replace("%23", "#");
        } catch (UnsupportedEncodingException e) {
            // Runtime exception as this doesn't really happen
            throw new RuntimeException("Encoding the URI failed", e); // NOSONAR
        }
    }

    /**
     * Encodes a path segment of a URI.
     * <p>
     * Corresponds to encodeURIComponent in JavaScript
     * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent
     * <p>
     * The following characters are not escaped: {@literal A-Za-z0-9-_.!~*'()}
     *
     * @param path
     *            the path to encode
     */
    public static String encodeURIComponent(String path) {
        try {
            return URLEncoder.encode(path, StandardCharsets.UTF_8.name())
                    .replace("+", "%20").replace("%21", "!").replace("%7E", "~")
                    .replace("%27", "'").replace("%28", "(")
                    .replace("%29", ")");
        } catch (UnsupportedEncodingException e) {
            // Runtime exception as this doesn't really happen
            throw new RuntimeException("Encoding the URI failed", e); // NOSONAR
        }
    }

    /**
     * Gets the path info for a /VAADIN/something request.
     *
     * @param request
     *            the servlet request
     * @return the path info starting with /VAADIN/ or <code>null</code> if no
     *         path information is available
     */
    public static String getStaticVaadinPathInfo(VaadinRequest request) {
        if (request instanceof HttpServletRequest) {
            return getStaticVaadinPathInfo(((HttpServletRequest) request));
        }

        return request.getPathInfo();
    }

    /**
     * Gets the path info for a /VAADIN/something request.
     *
     * @param request
     *            the servlet request
     * @return the path info starting with /VAADIN/ or <code>null</code> if no
     *         path information is available
     */
    public static String getStaticVaadinPathInfo(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            return null;
        }

        if ("/VAADIN".equals(request.getServletPath())) {
            // If the servlet is deployed with a separate /VAADIN/* mapping, we
            // need to add the missing /VAADIN/
            String prefix = pathInfo.startsWith("/") ? "/VAADIN" : "/VAADIN/";
            pathInfo = prefix + pathInfo;
        }
        return pathInfo;
    }
}
