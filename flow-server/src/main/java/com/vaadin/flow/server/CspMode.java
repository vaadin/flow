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

/**
 * Modes for running the application under a strict Content Security Policy,
 * i.e. a nonce-based {@code script-src} policy without {@code 'unsafe-inline'}
 * and {@code 'unsafe-eval'}.
 * <p>
 * The mode is configured with the {@link InitParameters#CSP} configuration
 * parameter and read with
 * {@link com.vaadin.flow.function.DeploymentConfiguration#getCspMode()}.
 * <p>
 * <strong>Note:</strong> the {@link #WARN} mode does not log anything yet, and
 * the {@link #STRICT} mode so far only sends the Content Security Policy
 * header, which the client engine does not fully work under yet.
 *
 * @since 25.4
 */
public enum CspMode {
    /**
     * The default mode. The application behaves as if no strict Content
     * Security Policy is in use.
     */
    OFF,
    /**
     * The application runs unchanged, but every call that would not work under
     * a strict Content Security Policy is logged. Not implemented yet.
     */
    WARN,
    /**
     * Vaadin sends a nonce-based Content Security Policy, and APIs that need
     * {@code eval} throw.
     * <p>
     * In production mode, every index HTML response gets a new nonce that is
     * put on every {@code <script>} element of the page, and a
     * {@code Content-Security-Policy} header that allows only the scripts with
     * that nonce and the scripts they load. Applications adjust the policy
     * through
     * {@link com.vaadin.flow.server.communication.IndexHtmlResponse#getContentSecurityPolicy()}.
     * No header is sent in development mode, because the development server and
     * the development tools need {@code eval}.
     * <p>
     * <strong>Note:</strong> the policy does not allow {@code eval}, but the
     * client engine still uses it, not only for
     * {@link com.vaadin.flow.dom.Element#executeJs(String, Object...)} but also
     * for the event data and filter expressions of DOM event listeners, such as
     * those of a click listener. Until that is changed, the browser blocks
     * these in production mode, so the mode is not yet usable for applications.
     * Throwing from APIs that need {@code eval} is not implemented yet either.
     */
    STRICT
}
