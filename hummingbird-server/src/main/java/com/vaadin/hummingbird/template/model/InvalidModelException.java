/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.model;

/**
 * Exception thrown when encountering an invalid model type.
 *
 * @author Vaadin Ltd
 */
public class InvalidModelException extends RuntimeException {
    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message
     *            the exception message
     * @param cause
     *            the cause of the exception
     */
    public InvalidModelException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with the given message.
     *
     * @param message
     *            the exception message
     */
    public InvalidModelException(String message) {
        super(message);
    }

}
