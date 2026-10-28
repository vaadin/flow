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
 * Thrown in the {@link CspMode#STRICT} Content Security Policy mode by an API
 * that sends JavaScript to the browser as a string, such as
 * {@link com.vaadin.flow.dom.Element#executeJs(String, Object...)}. The browser
 * would compile such a string with <code>new Function</code>, which a strict
 * policy without <code>unsafe-eval</code> blocks, so the call fails on the
 * server instead, where the stack trace points at the code that made it.
 * <p>
 * The stack trace starts at the API and leads to the code that called it.
 */
public class CspViolationException extends IllegalStateException {

    /**
     * Creates a new exception with the given detail message.
     *
     * @param message
     *            the detail message, not <code>null</code>
     */
    public CspViolationException(String message) {
        super(message);
    }
}
