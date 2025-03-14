/*
 * Copyright 2000-2025 Vaadin Ltd.
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
 * Thrown for problems which occur in the {@link VaadinService} layer.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ServiceException extends Exception {

    /**
     * Creates an exception which wraps the given throwable.
     *
     * @param throwable
     *            the throwable to wrap
     */
    public ServiceException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Creates an exception which contains the given message.
     *
     * @param message
     *            the message
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Creates an exception which wraps the given throwable and contains the
     * given message.
     *
     * @param message
     *            the message
     * @param throwable
     *            the throwable to wrap
     */
    public ServiceException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
