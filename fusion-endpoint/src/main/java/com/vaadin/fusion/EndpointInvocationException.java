package com.vaadin.fusion;

public class EndpointInvocationException extends Exception {

    public enum Type {
        NOT_FOUND, INTERNAL_ERROR, ACCESS_DENIED, INVALID_INPUT_DATA
    }

    private Type type;
    private String errorMessage;

    public EndpointInvocationException(Type type) {
        this.type = type;
    }

    public EndpointInvocationException(Type type, String errorMessage) {
        this.type = type;
        this.errorMessage = errorMessage;
    }

    public Type getType() {
        return type;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
