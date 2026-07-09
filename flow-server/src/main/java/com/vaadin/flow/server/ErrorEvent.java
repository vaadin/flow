/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;

/**
 * An error thrown by the framework and handled by an {@link ErrorHandler}.
 * Typically handled by {@link VaadinSession#getErrorHandler()}.
 *
 * @since 1.0
 */
public class ErrorEvent implements Serializable {

    private final Throwable throwable;
    private final StateNode componentNode;

    /**
     * Creates an error event which wraps the given throwable.
     *
     * @param throwable
     *            the throwable to wrap
     */
    public ErrorEvent(Throwable throwable) {
        this.throwable = throwable;
        componentNode = null;
    }

    /**
     * Create an error event which wraps the given throwable and component for
     * exception.
     *
     * @param throwable
     *            the throwable to wrap
     * @param componentNode
     *            stateNode of for exception component.
     * @since 24.3
     */
    public ErrorEvent(Throwable throwable, StateNode componentNode) {
        this.throwable = throwable;
        this.componentNode = componentNode;
    }

    /**
     * Gets the contained throwable, the cause of the error.
     *
     * @return the throwable that caused the error
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Get the Component that the error was thrown for. If not known returns
     * empty optional.
     *
     * @return Component that error happened for if available
     * @since 24.3
     */
    public Optional<Component> getComponent() {
        return getElement().flatMap(Element::getComponent);
    }

    /**
     * Get the Element that the error was thrown for. If not known return empty
     * optional.
     *
     * @return Element that error happened for if available
     * @since 24.3
     */
    public Optional<Element> getElement() {
        if (componentNode != null) {
            try {
                return Optional.ofNullable(Element.get(componentNode));
            } catch (IllegalArgumentException iae) {
                // NO-OP return Optional.empty
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the error handler for the given session.
     *
     * @param session
     *            the active session
     * @return An ErrorHandler for the session or null if none was found
     */
    public static ErrorHandler findErrorHandler(VaadinSession session) {
        if (session == null) {
            return null;
        }
        return session.getErrorHandler();
    }

}
