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
package com.vaadin.flow.router;

import java.io.Serializable;

/**
 * Navigation error parameter class containing thrown exception and possibly a
 * custom message.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 * @param <T>
 *            the type of the error parameter
 */
public class ErrorParameter<T extends Exception> implements Serializable {

    private final T exception;
    private final Exception caughtException;
    private final String customMessage;

    /**
     * Create ErrorParameter for exception.
     *
     * @param parameterType
     *            the exception type of this error parameter
     * @param caughtException
     *            the caught exception
     */
    public ErrorParameter(Class<T> parameterType, Exception caughtException) {
        this(parameterType, caughtException, null);
    }

    /**
     * Create ErrorParameter for exception with a custom message.
     *
     * @param parameterType
     *            the exception type of this error parameter
     * @param caughtException
     *            the caught exception
     * @param customMessage
     *            custom message to go with exception
     */
    public ErrorParameter(Class<T> parameterType, Exception caughtException,
            String customMessage) {
        this.caughtException = caughtException;
        this.customMessage = customMessage;
        this.exception = findCauseByType(parameterType, caughtException);
    }

    private static <T extends Exception> T findCauseByType(Class<T> type,
            Exception exception) {
        if (type.isInstance(exception)) {
            return type.cast(exception);
        } else {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                return findCauseByType(type, (Exception) cause);
            } else {
                return null;
            }
        }
    }

    /**
     * Get the error parameter exception. This will be the same as
     * {@link #getCaughtException()} if that exception is of the type of this
     * error parameter. Otherwise, it will be the first
     * {@link Exception#getCause()} which is of the right type.
     *
     * @see #getCaughtException()
     *
     * @return exception
     */
    public T getException() {
        return exception;
    }

    /**
     * Gets the originally caught exception. This exception might not match the
     * type of this error parameter if it has been created based on an exception
     * that is the cause of the caught exception.
     *
     * @see #getException()
     *
     * @return the caught exception
     */
    public Exception getCaughtException() {
        return caughtException;
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
