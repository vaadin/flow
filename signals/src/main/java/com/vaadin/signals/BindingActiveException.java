package com.vaadin.signals;

/**
 * Exception thrown when an operation could not be performed because a binding
 * is active.
 */
public class BindingActiveException extends IllegalStateException {

    public BindingActiveException() {
        super("Operation could not be performed because a binding is active.");
    }

}
