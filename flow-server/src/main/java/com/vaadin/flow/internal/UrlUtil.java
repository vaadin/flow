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
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinService;

/**
 * Internal utility class for URL handling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class UrlUtil {
    static final Set<String> ALLOW_ALL_URL_SAFE_SCHEMES = Collections
            .singleton(Constants.URL_SAFE_SCHEMES_WILDCARD);

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

    /**
     * Checks whether the scheme of the given URL is considered safe by the
     * current deployment configuration.
     * <p>
     * The set of safe schemes is read from the current {@link VaadinService}'s
     * {@link DeploymentConfiguration#getUrlSafeSchemes()}, falling back to
     * {@code Set.of(Constants.URL_SAFE_SCHEMES_WILDCARD)}, which bypasses the
     * validation and allows all URLs when no {@link VaadinService} is
     * available. Relative URLs (without a scheme) are always considered safe,
     * whereas URLs containing control characters are rejected as they can be
     * used to obfuscate the scheme. A {@code null} URL is considered unsafe.
     *
     * @param url
     *            the URL to check, may be {@code null}
     * @return {@code true} if the URL is safe, {@code false} otherwise
     */
    public static boolean isSafeUrl(String url) {
        VaadinService service = VaadinService.getCurrent();
        Set<String> safeSchemes;
        if (service == null) {
            if (getLogger().isDebugEnabled()) {
                getLogger()
                        .debug("No VaadinService available on current thread; "
                                + "bypassing URL scheme validation. "
                                + "Any custom {} configuration will not apply "
                                + "here.", InitParameters.URL_SAFE_SCHEMES);
            }
            safeSchemes = ALLOW_ALL_URL_SAFE_SCHEMES;
        } else {
            safeSchemes = service.getDeploymentConfiguration()
                    .getUrlSafeSchemes();
        }
        return isSafeUrl(url, safeSchemes);
    }

    /**
     * Builds the message for the {@link IllegalArgumentException} that a
     * validating URL setter throws when given a URL whose scheme is not
     * considered safe. The message points to both the
     * {@link InitParameters#URL_SAFE_SCHEMES} configuration property and the
     * setter that bypasses validation.
     *
     * @param type
     *            the kind of URL being set, for example {@code "href"},
     *            {@code "src"} or {@code "path"}
     * @param url
     *            the rejected URL
     * @param unsafeMethod
     *            the signature of the method that bypasses validation, for
     *            example {@code "setUnsafeHref(String)"}
     * @return the exception message
     */
    public static String getUnsafeUrlMessage(String type, String url,
            String unsafeMethod) {
        return String.format(
                "The %s \"%s\" uses a scheme that is not considered safe. "
                        + "Configure the safe schemes with the \"%s\" property, "
                        + "or use %s if this URL is intentional and trusted.",
                type, url, InitParameters.URL_SAFE_SCHEMES, unsafeMethod);
    }

    /**
     * Checks whether the scheme of the given URL is part of the given set of
     * safe schemes. See {@link #isSafeUrl(String)} for the validation rules. A
     * {@code null} URL is considered unsafe.
     *
     * @param url
     *            the URL to check, may be {@code null}
     * @param safeSchemes
     *            the set of safe lower-case schemes, or a set containing
     *            {@link Constants#URL_SAFE_SCHEMES_WILDCARD} to treat any
     *            scheme as safe
     * @return {@code true} if the URL is safe, {@code false} otherwise
     */
    static boolean isSafeUrl(String url, Set<String> safeSchemes) {
        if (url == null) {
            return false;
        }
        // A wildcard entry treats every scheme as safe, keeping the behaviour
        // fully backwards compatible for applications that opt out.
        if (safeSchemes.contains(Constants.URL_SAFE_SCHEMES_WILDCARD)) {
            return true;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        // Reject control characters which browsers may strip, allowing a
        // different URL to be acted upon than the one validated here (for
        // example "java\tscript:alert(1)").
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isISOControl(trimmed.charAt(i))) {
                return false;
            }
        }
        String scheme = extractScheme(trimmed);
        if (scheme == null) {
            // Relative URLs have no scheme and cannot trigger scheme-based
            // script execution.
            return true;
        }
        return safeSchemes.contains(scheme.toLowerCase(Locale.ROOT));
    }

    /**
     * Extracts the scheme from the given URL, or returns {@code null} if the
     * URL is relative (has no scheme). The scheme is determined according to
     * RFC 3986: a letter followed by any number of letters, digits,
     * {@code '+'}, {@code '-'} or {@code '.'}, terminated by a {@code ':'} that
     * occurs before any {@code '/'}, {@code '?'} or {@code '#'}.
     * <p>
     * The scheme is extracted without parsing the whole URL so that valid
     * relative URLs containing characters that a strict URI parser would reject
     * (such as spaces) are not falsely flagged.
     */
    private static String extractScheme(String url) {
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == ':') {
                return i == 0 ? null : url.substring(0, i);
            }
            boolean validSchemeChar = (i == 0) ? isAlpha(c)
                    : (isAlpha(c) || (c >= '0' && c <= '9') || c == '+'
                            || c == '-' || c == '.');
            if (!validSchemeChar) {
                // The ':' (if any) belongs to the path or query, so there is no
                // scheme and the URL is relative.
                return null;
            }
        }
        return null;
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(UrlUtil.class);
    }
}
