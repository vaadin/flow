package com.vaadin.flow.server;

/**
 * Exception thrown when {@link PWARegistry} initialization fails.
 *
 */
public class PWAInitializationException extends RuntimeException {

    /**
     * Constructs {@link PWAInitializationException} with message and underlying
     * cause.
     *
     * @param message the detail message.
     * @param cause underlying cause of exception.
     */
    public PWAInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
