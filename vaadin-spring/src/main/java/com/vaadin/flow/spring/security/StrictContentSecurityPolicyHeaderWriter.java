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
package com.vaadin.flow.spring.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.HeaderWriterFilter;

import com.vaadin.flow.internal.ContentSecurityPolicyHeaderDelegate;
import com.vaadin.flow.server.ContentSecurityPolicy;

/**
 * Writes the {@code Content-Security-Policy} header of a Vaadin page in the
 * strict mode as one of the header writers of Spring Security, so that the
 * policy an application configures in Spring Security and the nonce-based
 * policy of Vaadin are sent as one header.
 * <p>
 * Spring Security writes its headers when the response is committed, and its
 * own Content Security Policy writer runs before this one. This writer then
 * replaces that header with the policy of the page, which Vaadin hands over
 * through {@link ContentSecurityPolicyHeaderDelegate}, merged with the
 * directives Spring Security wrote. On a response that is not a Vaadin page,
 * nothing is handed over and the header of Spring Security stays as it is.
 * <p>
 * The same instance is a filter that follows the {@link HeaderWriterFilter}. It
 * tells Vaadin, before the page is created, that the header of this request is
 * written here.
 * <p>
 * The merged policy allows what either policy allows, with these rules:
 * <ul>
 * <li>The script directives of the Spring Security policy are ignored, because
 * Vaadin allows scripts only by the nonce of the response.
 * <li>The sources of a directive that both policies set are combined.
 * {@code 'none'} is dropped when the other policy allows a source.
 * <li>A directive that only Spring Security sets, such as
 * {@code frame-ancestors} or {@code report-to}, is copied.
 * <li>The {@code default-src} sources of Spring Security are also added to the
 * resource directives that Vaadin sets and Spring Security does not, because
 * that is where its policy applies them. A directive Vaadin sets to
 * {@code 'none'} stays that way, and so does {@code worker-src}, because
 * workers run script.
 * </ul>
 */
final class StrictContentSecurityPolicyHeaderWriter
        implements HeaderWriter, Filter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(StrictContentSecurityPolicyHeaderWriter.class);

    private static final String HEADER = "Content-Security-Policy";
    private static final String REQUEST_ATTRIBUTE = ContentSecurityPolicyHeaderDelegate.class
            .getName();
    private static final String DEFAULT_SRC = "default-src";
    private static final String NONE = "'none'";
    private static final Set<String> SCRIPT_DIRECTIVES = Set.of("script-src",
            "script-src-elem", "script-src-attr");
    // The directives a browser reads default-src for when they are not set.
    // worker-src and child-src are left out: they load code that runs as
    // script, and a browser reads script-src for them before default-src
    private static final Set<String> DEFAULT_SRC_FALLBACK_DIRECTIVES = Set.of(
            "connect-src", "font-src", "frame-src", "img-src", "manifest-src",
            "media-src", "object-src", "style-src", "style-src-elem",
            "style-src-attr");

    // Policies are fixed in the configuration, so each is reported once
    private final Set<String> reportedPolicies = ConcurrentHashMap.newKeySet();

    private volatile boolean headerWriterFilterPresent;

    /**
     * Records that the security filter chain has a {@link HeaderWriterFilter},
     * which is what runs this writer. Without one, for example when the
     * application disables the headers, Vaadin has to write its header itself.
     */
    void setHeaderWriterFilterPresent() {
        headerWriterFilterPresent = true;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (headerWriterFilterPresent
                && request instanceof HttpServletRequest httpRequest) {
            findOrCreateHandover(httpRequest);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void writeHeaders(HttpServletRequest request,
            HttpServletResponse response) {
        // When Spring Security writes its headers at the start of the
        // request, this runs before the filter and before Vaadin
        findOrCreateHandover(request).writeMergedHeaderTo(response);
    }

    private Handover findOrCreateHandover(HttpServletRequest request) {
        if (request.getAttribute(REQUEST_ATTRIBUTE) instanceof Handover found) {
            return found;
        }
        Handover handover = new Handover();
        request.setAttribute(REQUEST_ATTRIBUTE, handover);
        return handover;
    }

    /**
     * The policy of one request on its way from Vaadin to the response.
     * Whichever of the two sides comes second writes the header: the header
     * writers of Spring Security when the response is committed, or Vaadin when
     * Spring Security wrote its headers at the start of the request.
     */
    private final class Handover
            implements ContentSecurityPolicyHeaderDelegate {

        private @Nullable ContentSecurityPolicy policy;
        private @Nullable HttpServletResponse responseWithHeaders;

        @Override
        public void writeHeader(ContentSecurityPolicy policy) {
            this.policy = policy;
            if (responseWithHeaders != null) {
                writeMergedHeaderTo(responseWithHeaders);
            }
        }

        void writeMergedHeaderTo(HttpServletResponse response) {
            responseWithHeaders = response;
            ContentSecurityPolicy pagePolicy = policy;
            if (pagePolicy == null) {
                // Not a Vaadin page, or not created yet
                return;
            }
            for (String springSecurityPolicy : List
                    .copyOf(response.getHeaders(HEADER))) {
                List<String> ignored;
                try {
                    ignored = mergePolicy(pagePolicy, springSecurityPolicy);
                } catch (IllegalArgumentException e) {
                    // The page is still sent with the policy of Vaadin
                    if (reportedPolicies.add(springSecurityPolicy)) {
                        LOGGER.error(
                                "The Content Security Policy configured in Spring Security cannot be merged into the policy of Vaadin pages and is not applied to them. Policy: {}",
                                springSecurityPolicy, e);
                    }
                    continue;
                }
                if (!ignored.isEmpty()
                        && reportedPolicies.add(springSecurityPolicy)) {
                    LOGGER.warn(
                            """
                                    The Content Security Policy configured in Spring Security \
                                    sets {}, which is not applied to Vaadin pages. With \
                                    contentSecurityPolicy=strict, Vaadin allows scripts only by \
                                    a nonce it creates for each response. The other directives \
                                    of the policy are applied. Policy: {}""",
                            String.join(", ", ignored), springSecurityPolicy);
                }
            }
            response.setHeader(HEADER, pagePolicy.toHeaderValue());
            // Merged, so a second call must not merge it again
            policy = null;
        }
    }

    /**
     * Merges a policy given as a header value into the policy of a page.
     *
     * @return the script directives of the header value, which are ignored
     */
    private static List<String> mergePolicy(ContentSecurityPolicy pagePolicy,
            String headerValue) {
        ContentSecurityPolicy other = new ContentSecurityPolicy();
        List<String> otherDirectives = new ArrayList<>();
        for (String directive : headerValue.split(";")) {
            String[] tokens = directive.strip().split("\\s+");
            String name = tokens[0].toLowerCase(Locale.ENGLISH);
            // A browser uses the first of a repeated directive
            if (!name.isEmpty() && !otherDirectives.contains(name)) {
                other.setDirective(name,
                        Arrays.copyOfRange(tokens, 1, tokens.length));
                otherDirectives.add(name);
            }
        }
        List<String> ignored = new ArrayList<>();
        for (String directive : otherDirectives) {
            if (SCRIPT_DIRECTIVES.contains(directive)) {
                ignored.add(directive);
            } else {
                addAllowedSources(pagePolicy, directive,
                        other.getSources(directive));
            }
        }
        if (otherDirectives.contains(DEFAULT_SRC)) {
            for (String directive : DEFAULT_SRC_FALLBACK_DIRECTIVES) {
                List<String> own = pagePolicy.getSources(directive);
                if (!own.isEmpty() && !own.equals(List.of(NONE))
                        && !otherDirectives.contains(directive)) {
                    addAllowedSources(pagePolicy, directive,
                            other.getSources(DEFAULT_SRC));
                }
            }
        }
        return ignored;
    }

    private static void addAllowedSources(ContentSecurityPolicy policy,
            String directive, List<String> sources) {
        Set<String> combined = new LinkedHashSet<>(
                policy.getSources(directive));
        combined.addAll(sources);
        if (combined.size() > 1) {
            combined.remove(NONE);
        }
        policy.setDirective(directive, combined.toArray(String[]::new));
    }
}
