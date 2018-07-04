/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component;

/**
 * Exception thrown if the UI has been detached when it should not be.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UIDetachedException extends RuntimeException {

    /**
     * Creates an instance of the exception.
     */
    public UIDetachedException() {
        super();
    }

    /**
     * Creates an instance of the exception using the given message and cause.
     *
     * @param message
     *            the message to use
     * @param cause
     *            the cause of the exception
     */
    public UIDetachedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an instance of the exception using the given message.
     *
     * @param message
     *            the message to use
     */
    public UIDetachedException(String message) {
        super(message);
    }

    /**
     * Creates an instance of the exception using the given cause.
     *
     * @param cause
     *            the cause of the exception
     */
    public UIDetachedException(Throwable cause) {
        super(cause);
    }

}
