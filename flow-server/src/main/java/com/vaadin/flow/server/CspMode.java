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
     * a strict Content Security Policy is logged.
     */
    WARN,
    /**
     * Vaadin sends a nonce-based Content Security Policy, and APIs that need
     * {@code eval} throw.
     */
    STRICT
}
