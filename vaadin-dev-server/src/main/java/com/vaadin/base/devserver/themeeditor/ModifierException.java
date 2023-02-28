package com.vaadin.base.devserver.themeeditor;

public class ModifierException extends RuntimeException {

    public ModifierException() {
    }

    public ModifierException(String message) {
        super(message);
    }

    public ModifierException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModifierException(Throwable cause) {
        super(cause);
    }

    public ModifierException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
