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

import org.jspecify.annotations.Nullable;
import org.springframework.security.web.header.ContentSecurityPolicyNonce;

import com.vaadin.flow.internal.ContentSecurityPolicyNonceProvider;
import com.vaadin.flow.server.VaadinRequest;

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
 * For internal use only. May be renamed or removed in a future release.
 */
public final class SpringSecurityContentSecurityPolicyNonceProvider
        implements ContentSecurityPolicyNonceProvider {

    @Override
    public @Nullable String findNonce(VaadinRequest request) {
        // Spring Security sets the attribute for the requests of a filter
        // chain that has a Content Security Policy
        if (request.getAttribute(ContentSecurityPolicyNonce.class
                .getName()) instanceof ContentSecurityPolicyNonce nonce) {
            return nonce.getNonce();
        }
        return null;
    }
}
