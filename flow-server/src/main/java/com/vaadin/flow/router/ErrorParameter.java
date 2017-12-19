/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.router;

import java.io.Serializable;

/**
 * Navigation error parameter class containing thrown exception and possibly a
 * custom message.
 * 
 * @author Vaadin Ltd.
 */
public class ErrorParameter<T extends Exception> implements Serializable {

    private T exception;
    private String customMessage;

    /**
     * Create ErrorParameter for exception.
     * 
     * @param exception
     *            exception to create error parameter for
     */
    public ErrorParameter(T exception) {
        this.exception = exception;
    }

    /**
     * Create ErrorParameter for exception with a custom message.
     * 
     * @param exception
     *            exception to create error parameter for
     * @param customMessage
     *            custom message to go with exception
     */
    public ErrorParameter(T exception, String customMessage) {
        this.exception = exception;
        this.customMessage = customMessage;
    }

    /**
     * Get the handled exception.
     * 
     * @return exception
     */
    public T getException() {
        return exception;
    }

    /**
     * Check if we have a custom message for the exception.
     * 
     * @return true if a custom message is set
     */
    public boolean hasCustomMessage() {
        return customMessage != null && !customMessage.isEmpty();
    }

    /**
     * Get the set custom message.
     * 
     * @return custom message or empty if not defined
     */
    public String getCustomMessage() {
        return customMessage == null ? "" : customMessage;
    }
}
