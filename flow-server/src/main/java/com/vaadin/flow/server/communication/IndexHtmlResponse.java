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
package com.vaadin.flow.server.communication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ContentSecurityPolicy;
import com.vaadin.flow.server.ContentSecurityPolicyMode;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;

/**
 * This represents the state of a Index HTML response being generated. The Index
 * HTML response contains of the full DOM of the HTML document as well as the
 * HTTP headers that will be included in the corresponding HTTP response.
 *
 * @since 3.0
 */
public class IndexHtmlResponse {

    private final VaadinRequest vaadinRequest;
    private final VaadinResponse vaadinResponse;
    private final Document document;
    private @Nullable String contentSecurityPolicyNonce;
    private @Nullable ContentSecurityPolicy contentSecurityPolicy;
    private final Set<String> mergedContentSecurityPolicies = new HashSet<>();

    private static final String DEFAULT_SRC = "default-src";
    private static final String NONE = "'none'";
    // Vaadin allows scripts by nonce only, so these are never taken from
    // another policy
    private static final Set<String> SCRIPT_DIRECTIVES = Set.of("script-src",
            "script-src-elem", "script-src-attr");
    // The directives a browser reads default-src for when they are not set.
    // worker-src and child-src are left out: they load code that runs as
    // script, and a browser reads script-src for them before default-src
    private static final Set<String> DEFAULT_SRC_FALLBACK_DIRECTIVES = Set.of(
            "connect-src", "font-src", "frame-src", "img-src", "manifest-src",
            "media-src", "object-src", "style-src", "style-src-elem",
            "style-src-attr");

    /**
     * Create a response object for client side bootstrapping with UI.
     *
     * @param vaadinRequest
     *            the vaadin request which is handling
     * @param vaadinResponse
     *            the corresponding vaadin response
     * @param document
     *            the {@link Document} object of the response page
     * @param ui
     *            the UI for the bootstrap
     */
    public IndexHtmlResponse(VaadinRequest vaadinRequest,
            VaadinResponse vaadinResponse, Document document, UI ui) {
        this.vaadinRequest = vaadinRequest;
        this.vaadinResponse = vaadinResponse;
        this.document = document;
    }

    /**
     * Create a response object for client side bootstrapping.
     *
     * @param vaadinRequest
     *            the vaadin request which is handling
     * @param vaadinResponse
     *            the corresponding vaadin response
     * @param document
     *            the {@link Document} object of the response page
     */
    public IndexHtmlResponse(VaadinRequest vaadinRequest,
            VaadinResponse vaadinResponse, Document document) {
        this.vaadinRequest = vaadinRequest;
        this.vaadinResponse = vaadinResponse;
        this.document = document;
    }

    /**
     * Get the request which triggers the Index HTML response.
     *
     * @return the Vaadin request
     */
    public VaadinRequest getVaadinRequest() {
        return vaadinRequest;
    }

    /**
     * Get the Vaadin response object including all the headers which will be
     * sent to browser.
     *
     * @return the Vaadin response
     */
    public VaadinResponse getVaadinResponse() {
        return vaadinResponse;
    }

    /**
     * Get the index.html response in form of a {@link Document} instance.
     *
     * @return the index document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Gets the UI that will be displayed on the generated HTML page.
     *
     * @return the UI
     */
    public Optional<UI> getUI() {
        if (!this.vaadinRequest.getService().getDeploymentConfiguration()
                .isEagerServerLoad()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(UI.getCurrent());
        }
    }

    /**
     * Gets the nonce of this response, which Vaadin puts on every
     * {@code <script>} element of the document and allows in the
     * {@code script-src} directive of the Content Security Policy.
     * <p>
     * A new nonce is created for every response when the application runs with
     * the {@link ContentSecurityPolicyMode#STRICT} mode. Scripts added by an
     * {@link IndexHtmlRequestListener} get the nonce automatically, but the
     * value can also be used for other content that needs it. The nonce is not
     * put on the scripts in development mode.
     *
     * @return the nonce, or {@code null} if the mode is not
     *         {@link ContentSecurityPolicyMode#STRICT}
     * @since 25.4
     */
    public @Nullable String getContentSecurityPolicyNonce() {
        return contentSecurityPolicyNonce;
    }

    /**
     * Gets the Content Security Policy that is sent with this response.
     * <p>
     * The policy is created when the application runs with the
     * {@link ContentSecurityPolicyMode#STRICT} mode. An
     * {@link IndexHtmlRequestListener} can add or override directives for
     * resources of the application, such as {@code connect-src},
     * {@code img-src}, {@code frame-src} or {@code report-to}.
     * <p>
     * Do not change {@code script-src}: it holds the nonce and
     * {@code 'strict-dynamic'}, so replacing it with
     * {@link ContentSecurityPolicy#setDirective(String, String...)} blocks
     * every script of the page, and host sources added to it are ignored by the
     * browser. To load a script from another origin, add a {@code <script>}
     * element to the document, which gets the nonce, or load it from a script
     * that is already trusted.
     * <p>
     * The header is written after all listeners have run. It is not written in
     * development mode, because the development server and the development
     * tools need {@code eval}. In production mode, the request fails with an
     * {@link IllegalStateException} if the response already has a
     * {@code Content-Security-Policy} header when the page is written, for
     * example from a servlet filter that runs before Vaadin, unless that header
     * was merged with {@link #mergeContentSecurityPolicy(String)}. A policy
     * configured in Spring Security is merged automatically.
     *
     * @return the mutable policy, or {@code null} if the mode is not
     *         {@link ContentSecurityPolicyMode#STRICT}
     * @since 25.4
     */
    public @Nullable ContentSecurityPolicy getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    /**
     * Merges another Content Security Policy, given as the value of a
     * {@code Content-Security-Policy} header, into the policy of this response.
     * This is how a policy that the application configures elsewhere, for
     * example in Spring Security, is combined with the policy that Vaadin
     * creates for the page.
     * <p>
     * The result allows what either policy allows, with these rules:
     * <ul>
     * <li>The script directives ({@code script-src}, {@code script-src-elem},
     * {@code script-src-attr}) of the other policy are ignored, because Vaadin
     * allows scripts only by the nonce of the response. They are returned so
     * that the caller can report them.
     * <li>The sources of a directive that both policies set are combined.
     * {@code 'none'} is dropped when the other policy allows a source.
     * <li>A directive that only the other policy sets, such as
     * {@code frame-ancestors} or {@code report-to}, is copied.
     * <li>The {@code default-src} sources of the other policy are also added to
     * the resource directives that Vaadin sets and the other policy does not,
     * because that is where the other policy applies them. A directive that
     * Vaadin sets to {@code 'none'} stays that way, and so does
     * {@code worker-src}, because workers run script.
     * </ul>
     *
     * @param headerValue
     *            the header value of the other policy, not null
     * @return the script directives of the other policy that were ignored, in
     *         the order they were given, empty if there were none
     * @throws IllegalStateException
     *             if this response has no policy, i.e. the mode is not
     *             {@link ContentSecurityPolicyMode#STRICT}
     * @throws IllegalArgumentException
     *             if a directive name is not valid, or if the value holds
     *             several policies separated by a comma
     */
    public List<String> mergeContentSecurityPolicy(String headerValue) {
        ContentSecurityPolicy policy = contentSecurityPolicy;
        if (policy == null) {
            throw new IllegalStateException(
                    "This response has no Content Security Policy to merge into. "
                            + "A policy is only created in the strict mode.");
        }
        ContentSecurityPolicy other = new ContentSecurityPolicy();
        List<String> otherDirectives = new ArrayList<>();
        for (String directive : headerValue.split(";")) {
            String[] tokens = directive.strip().split("\\s+");
            String name = tokens[0].toLowerCase(Locale.ENGLISH);
            // A browser uses the first of a repeated directive
            if (!name.isEmpty() && !otherDirectives.contains(name)) {
                // Also rejects what is not valid in a header
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
                addAllowedSources(policy, directive,
                        other.getSources(directive));
            }
        }
        if (otherDirectives.contains(DEFAULT_SRC)) {
            for (String directive : DEFAULT_SRC_FALLBACK_DIRECTIVES) {
                List<String> own = policy.getSources(directive);
                if (!own.isEmpty() && !own.equals(List.of(NONE))
                        && !otherDirectives.contains(directive)) {
                    addAllowedSources(policy, directive,
                            other.getSources(DEFAULT_SRC));
                }
            }
        }
        mergedContentSecurityPolicies.add(headerValue);
        return ignored;
    }

    /**
     * Checks whether a header value was merged into the policy of this response
     * with {@link #mergeContentSecurityPolicy(String)}.
     */
    boolean hasMergedContentSecurityPolicy(String headerValue) {
        return mergedContentSecurityPolicies.contains(headerValue);
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

    void setContentSecurityPolicy(String contentSecurityPolicyNonce,
            ContentSecurityPolicy contentSecurityPolicy) {
        this.contentSecurityPolicyNonce = contentSecurityPolicyNonce;
        this.contentSecurityPolicy = contentSecurityPolicy;
    }
}
