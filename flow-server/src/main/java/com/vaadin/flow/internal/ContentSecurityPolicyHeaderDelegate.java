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

import com.vaadin.flow.server.ContentSecurityPolicy;

/**
 * Writes the {@code Content-Security-Policy} header of an index HTML response
 * in place of Vaadin, in the strict mode.
 * <p>
 * An integration that owns the security headers of the application, such as the
 * one with Spring Security, puts an instance in the request attribute named
 * after this interface. Vaadin then hands it the policy of the page, after all
 * index HTML request listeners have run, and does not set the header itself.
 * The delegate has to make sure the header is sent with the response: without
 * it, the page has no policy.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@FunctionalInterface
public interface ContentSecurityPolicyHeaderDelegate {

    /**
     * Takes over the policy of the page that is being written.
     *
     * @param policy
     *            the policy Vaadin created for the page, with the nonce of the
     *            response in {@code script-src}, not null
     */
    void writeHeader(ContentSecurityPolicy policy);
}
