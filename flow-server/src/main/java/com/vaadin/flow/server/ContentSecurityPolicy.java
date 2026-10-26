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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A mutable Content Security Policy, i.e. a set of directives and the sources
 * of each directive, that is serialized as the value of a
 * {@code Content-Security-Policy} HTTP header.
 * <p>
 * Directive names are case-insensitive and kept in lower case. Directives are
 * written to the header in the order they were first set. Sources are written
 * as given, so keywords need their quotes, for example {@code "'self'"} or
 * {@code "'none'"}.
 * <p>
 * In the {@link CspMode#STRICT} mode, Vaadin creates the policy for each index
 * HTML response and exposes it through
 * {@link com.vaadin.flow.server.communication.IndexHtmlResponse#getContentSecurityPolicy()},
 * so that an
 * {@link com.vaadin.flow.server.communication.IndexHtmlRequestListener} can
 * adjust it before the header is sent. In that policy, {@code script-src} holds
 * the nonce of the response and should be left unchanged, see
 * {@link com.vaadin.flow.server.communication.IndexHtmlResponse#getContentSecurityPolicy()}.
 */
public class ContentSecurityPolicy implements Serializable {

    private static final Pattern DIRECTIVE_NAME = Pattern.compile("[a-z0-9-]+");
    private static final Pattern INVALID_SOURCE = Pattern.compile("[\\s;,]");

    private final Map<String, List<String>> directives = new LinkedHashMap<>();

    /**
     * Sets the sources of a directive, replacing any sources it had before.
     * Passing no sources sets a directive without a value, such as
     * {@code upgrade-insecure-requests}.
     *
     * @param directive
     *            the directive name, e.g. {@code "img-src"}, not null
     * @param sources
     *            the sources, e.g. {@code "'self'"} or
     *            {@code "https://cdn.example.com"}, not null
     * @return this policy, for chaining
     * @throws IllegalArgumentException
     *             if the directive name or a source is not valid in a header
     */
    public ContentSecurityPolicy setDirective(String directive,
            String... sources) {
        String name = normalizeDirective(directive);
        validateSources(name, sources);
        List<String> list = new ArrayList<>();
        appendSources(list, sources);
        directives.put(name, list);
        return this;
    }

    /**
     * Adds sources to a directive, keeping the sources it already has. The
     * directive is created if it is not set. A source the directive already has
     * is not added again.
     *
     * @param directive
     *            the directive name, e.g. {@code "connect-src"}, not null
     * @param sources
     *            the sources to add, not null
     * @return this policy, for chaining
     * @throws IllegalArgumentException
     *             if the directive name or a source is not valid in a header
     */
    public ContentSecurityPolicy addSources(String directive,
            String... sources) {
        String name = normalizeDirective(directive);
        validateSources(name, sources);
        appendSources(
                directives.computeIfAbsent(name, key -> new ArrayList<>()),
                sources);
        return this;
    }

    /**
     * Removes a directive. Nothing happens if the directive is not set.
     *
     * @param directive
     *            the directive name, not null
     * @return this policy, for chaining
     */
    public ContentSecurityPolicy removeDirective(String directive) {
        directives.remove(normalizeDirective(directive));
        return this;
    }

    /**
     * Gets the sources of a directive.
     *
     * @param directive
     *            the directive name, not null
     * @return an unmodifiable list of the sources, empty if the directive is
     *         not set or has no value
     */
    public List<String> getSources(String directive) {
        List<String> sources = directives.get(normalizeDirective(directive));
        return sources == null ? List.of()
                : Collections.unmodifiableList(sources);
    }

    /**
     * Serializes the policy to the value of a {@code Content-Security-Policy}
     * header.
     *
     * @return the header value, empty if no directive is set
     */
    public String toHeaderValue() {
        return directives.entrySet().stream().map(entry -> {
            if (entry.getValue().isEmpty()) {
                return entry.getKey();
            }
            return entry.getKey() + " " + String.join(" ", entry.getValue());
        }).collect(Collectors.joining("; "));
    }

    @Override
    public String toString() {
        return toHeaderValue();
    }

    private static String normalizeDirective(String directive) {
        Objects.requireNonNull(directive, "Directive must not be null");
        String name = directive.toLowerCase(Locale.ENGLISH);
        if (!DIRECTIVE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Invalid Content Security Policy directive name '"
                            + directive + "'");
        }
        return name;
    }

    // Validates all sources before any is added, so that a rejected call
    // leaves the policy unchanged
    private static void validateSources(String directive, String[] sources) {
        Objects.requireNonNull(sources, () -> "Sources of directive '"
                + directive + "' must not be null");
        for (int i = 0; i < sources.length; i++) {
            String source = sources[i];
            String problem;
            if (source == null) {
                throw new NullPointerException(describeInvalidSource(directive,
                        sources, i, "is null"));
            } else if (source.isEmpty()) {
                problem = "is empty";
            } else if (INVALID_SOURCE.matcher(source).find()) {
                problem = "contains whitespace, ';' or ','";
            } else {
                continue;
            }
            throw new IllegalArgumentException(
                    describeInvalidSource(directive, sources, i, problem));
        }
    }

    private static String describeInvalidSource(String directive,
            String[] sources, int index, String problem) {
        return String.format(
                "Source at index %d of Content Security Policy directive '%s' %s: %s",
                index, directive, problem, Arrays.toString(sources));
    }

    private static void appendSources(List<String> target, String[] sources) {
        for (String source : sources) {
            if (!target.contains(source)) {
                target.add(source);
            }
        }
    }
}
