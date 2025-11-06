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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal utility class for URL handling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class UrlUtil {

    private static final Pattern PERCENT_ENCODED = Pattern
            .compile("%([0-9A-Fa-f]{2})");

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
     * @return the encoded URI
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
     * @return the encoded path
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
     * Decodes a percent-encoded string according to RFC 3986.
     * <p>
     * Corresponds to decodeURIComponent in JavaScript
     * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/decodeURIComponent
     * <p>
     * Unlike {@link java.net.URLDecoder}, this method does not treat '+' as a
     * space character, making it suitable for decoding strings encoded with
     * JavaScript's {@code encodeURIComponent()} or {@link #encodeURIComponent(String)}.
     *
     * @param encoded
     *            the percent-encoded string
     * @return the decoded string
     */
    public static String decodeURIComponent(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return encoded;
        }

        Matcher matcher = PERCENT_ENCODED.matcher(encoded);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // Append text before the match
            result.append(encoded, lastEnd, matcher.start());

            // Decode the hex value
            String hex = matcher.group(1);
            int value = Integer.parseInt(hex, 16);
            result.append((char) value);

            lastEnd = matcher.end();
        }

        // Append remaining text
        result.append(encoded, lastEnd, encoded.length());

        // Handle multi-byte UTF-8 sequences
        byte[] bytes = new byte[result.length()];
        boolean hasMultibyte = false;
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (c > 127) {
                hasMultibyte = true;
            }
            bytes[i] = (byte) c;
        }

        if (hasMultibyte) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        return result.toString();
    }

    /**
     * Returns the given absolute path as a path relative to the servlet path.
     *
     * @param absolutePath
     *            the path to make relative
     * @param request
     *            a request with information about the servlet path
     * @return a relative path that when applied to the servlet path, refers to
     *         the absolute path without containing the context path or servlet
     *         path
     */
    public static String getServletPathRelative(String absolutePath,
            HttpServletRequest request) {
        String pathToServlet = request.getContextPath()
                + request.getServletPath();
        if (pathToServlet.startsWith("/")) {
            pathToServlet = pathToServlet.substring(1);
        }
        if (absolutePath.startsWith("/")) {
            absolutePath = absolutePath.substring(1);
        }
        String[] servletPathSegments = pathToServlet.isEmpty() ? new String[0]
                : pathToServlet.split("/");
        String[] absolutePathSegments = absolutePath.isEmpty() ? new String[0]
                : absolutePath.split("/");
        int startFrom = 0;
        while (absolutePathSegments.length > startFrom
                && servletPathSegments.length > startFrom
                && absolutePathSegments[startFrom]
                        .equals(servletPathSegments[startFrom])) {
            startFrom++;
        }

        String ret = "";

        for (int i = startFrom; i < servletPathSegments.length; i++) {
            ret += "../";
        }
        for (int i = startFrom; i < absolutePathSegments.length; i++) {
            ret += absolutePathSegments[i] + "/";
        }
        if (ret.isEmpty()) {
            return ".";
        }
        return ret.substring(0, ret.length() - 1);
    }
}
