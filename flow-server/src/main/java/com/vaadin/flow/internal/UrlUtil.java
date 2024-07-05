/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.internal;

import com.vaadin.flow.shared.ApplicationConstants;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
        return url.contains("://") && !url
                .startsWith(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);
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

}
