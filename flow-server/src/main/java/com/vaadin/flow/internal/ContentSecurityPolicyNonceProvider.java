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
package com.vaadin.flow.internal;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinRequest;

/**
 * Provides the Content Security Policy nonce of a request when something other
 * than Vaadin sends the policy of the application, such as Spring Security.
 * <p>
 * In the strict mode, Vaadin asks the implementation it finds through
 * {@link Lookup} for the nonce of each index HTML request. When there is one,
 * Vaadin puts that nonce on the scripts of the page and sends no policy of its
 * own: the policy that allows the nonce is sent by whoever created it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@FunctionalInterface
public interface ContentSecurityPolicyNonceProvider {

    /**
     * Finds the nonce that the Content Security Policy of the response to a
     * request allows.
     *
     * @param request
     *            the request the page is written for, not null
     * @return the nonce, or {@code null} if no policy with a nonce is sent for
     *         the request, in which case Vaadin sends its own
     */
    @Nullable
    String findNonce(VaadinRequest request);
}
