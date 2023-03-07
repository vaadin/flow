package com.vaadin.base.devserver.themeeditor.utils;

public class ThemeEditorException extends RuntimeException {
    public ThemeEditorException() {
    }

    public ThemeEditorException(String message) {
        super(message);
    }

    public ThemeEditorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ThemeEditorException(Throwable cause) {
        super(cause);
    }

    public ThemeEditorException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
