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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.header.ContentSecurityPolicyNonce;
import org.springframework.security.web.header.HeaderWriter;

import com.vaadin.flow.internal.ContentSecurityPolicyNonceProvider;
import com.vaadin.flow.server.ContentSecurityPolicy;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;

/**
 * Makes Vaadin use the Content Security Policy nonce of Spring Security in the
 * strict mode, so that an application configures its policy in Spring Security
 * only.
 * <p>
 * When a Content Security Policy is configured in Spring Security, it creates a
 * nonce for each request and replaces the {@code {nonce}} placeholder of the
 * policy with it, for example in
 * {@code script-src 'nonce-{nonce}' 'strict-dynamic'}. Vaadin then puts that
 * nonce on the scripts of its page and sends no policy of its own, so the page
 * is sent with the policy of Spring Security, as configured.
 * <p>
 * Spring Security creates the nonce for every request, also when it sends no
 * policy that allows it: the policy can be limited to other paths, be
 * report-only, or have no {@code {nonce}} placeholder. As a header writer that
 * runs after the ones of Spring Security, this class then adds the policy of
 * Vaadin to the response, so that the page is not left without a policy that
 * restricts its scripts.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class SpringSecurityContentSecurityPolicyNonceProvider
        implements ContentSecurityPolicyNonceProvider, HeaderWriter {

    private static final String HEADER = "Content-Security-Policy";
    private static final String FALLBACK_POLICY_ATTRIBUTE = SpringSecurityContentSecurityPolicyNonceProvider.class
            .getName() + ".fallbackPolicy";
    private static final String HEADERS_WRITTEN_ATTRIBUTE = SpringSecurityContentSecurityPolicyNonceProvider.class
            .getName() + ".headersWritten";
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SpringSecurityContentSecurityPolicyNonceProvider.class);

    @Override
    public @Nullable String findNonce(VaadinRequest request) {
        return toNonce(request
                .getAttribute(ContentSecurityPolicyNonce.class.getName()));
    }

    @Override
    public void setFallbackPolicy(VaadinRequest request,
            VaadinResponse response, ContentSecurityPolicy policy) {
        if (request.getAttribute(HEADERS_WRITTEN_ATTRIBUTE) != null
                && response instanceof HttpServletResponse servletResponse) {
            // Spring Security is set to write its headers at the start of the
            // request, so what it sends is already known
            addPolicyUnlessNonceAllowed(servletResponse, findNonce(request),
                    policy);
        } else {
            request.setAttribute(FALLBACK_POLICY_ATTRIBUTE, policy);
        }
    }

    @Override
    public void writeHeaders(HttpServletRequest request,
            HttpServletResponse response) {
        request.setAttribute(HEADERS_WRITTEN_ATTRIBUTE, Boolean.TRUE);
        if (request.getAttribute(
                FALLBACK_POLICY_ATTRIBUTE) instanceof ContentSecurityPolicy policy) {
            request.removeAttribute(FALLBACK_POLICY_ATTRIBUTE);
            addPolicyUnlessNonceAllowed(response,
                    toNonce(request.getAttribute(
                            ContentSecurityPolicyNonce.class.getName())),
                    policy);
        }
    }

    // Spring Security sets the attribute for the requests of a filter chain
    // that has a Content Security Policy
    private static @Nullable String toNonce(@Nullable Object attribute) {
        return attribute instanceof ContentSecurityPolicyNonce nonce
                ? nonce.getNonce()
                : null;
    }

    private static void addPolicyUnlessNonceAllowed(
            HttpServletResponse response, @Nullable String nonce,
            ContentSecurityPolicy policy) {
        String nonceSource = "'nonce-" + nonce + "'";
        if (response.getHeaders(HEADER).stream()
                .noneMatch(value -> value.contains(nonceSource))) {
            LOGGER.debug(
                    "Spring Security sends no Content Security Policy that allows the nonce of the request, adding the policy of Vaadin to the response: {}",
                    policy);
            // Added, not set: a policy of Spring Security without the nonce
            // still applies, a browser enforces every policy it gets
            response.addHeader(HEADER, policy.toHeaderValue());
        }
    }
}
