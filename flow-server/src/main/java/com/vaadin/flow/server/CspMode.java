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
 * The {@link #WARN} and {@link #STRICT} modes act on the APIs that send
 * JavaScript to the browser as a string, such as
 * {@link com.vaadin.flow.dom.Element#executeJs(String, Object...)}, because the
 * browser has to compile it, which a strict policy blocks. This applies to the
 * calls that Vaadin itself makes as well, and in development mode as much as in
 * production mode. The {@link #STRICT} mode also sends the Content Security
 * Policy header in production mode.
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
     * a strict Content Security Policy is logged as a warning, once per call
     * site, with a stack trace that leads from the API to its caller.
     */
    WARN,
    /**
     * Vaadin sends a nonce-based Content Security Policy, and APIs that need
     * {@code eval} throw a {@link CspViolationException}, whose stack trace
     * leads from the API to its caller.
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
     * <strong>Note:</strong> Vaadin itself still uses APIs that need
     * {@code eval}, not only for its own components but also for the event data
     * and filter expressions of DOM event listeners, such as those of a click
     * listener. Until that is changed, those calls throw, so the mode is not
     * yet usable for applications.
     */
    STRICT
}
