package com.vaadin.flow.server.frontend.webpush;

public class WebPushException extends Exception {

    public WebPushException() {
        super();
    }

    public WebPushException(String message) {
        super(message);
    }

    public WebPushException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebPushException(Throwable cause) {
        super(cause);
    }

    public WebPushException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
