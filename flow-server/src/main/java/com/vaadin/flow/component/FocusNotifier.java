/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.io.Serializable;

import com.vaadin.flow.shared.Registration;

/**
 * Mixin interface to handle focus events on components.
 *
 * @param <T>
 *            the type of the component returned at the
 *            {@link FocusEvent#getSource()}
 * @since 1.0
 */
public interface FocusNotifier<T extends Component> extends Serializable {

    /**
     * Add a listener to focus DOM events.
     *
     * @param listener
     *            the focus listener
     * @return a registration that can be used to unregister the listener
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Events/blur">focus
     *      event at MDN</a>
     */
    default Registration addFocusListener(
            ComponentEventListener<FocusEvent<T>> listener) {
        if (this instanceof Component) {
            return ComponentUtil.addListener((Component) this, FocusEvent.class,
                    (ComponentEventListener) listener);
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addFocusListener"));
        }
    }

    /**
     * Represents the DOM event "focus".
     *
     * @param <C>
     *            The source component type.
     */
    @DomEvent("focus")
    class FocusEvent<C extends Component> extends ComponentEvent<C> {

        /**
         * FocusEvent base constructor.
         *
         * @param source
         *            the source component
         * @param fromClient
         *            <code>true</code> if the event originated from the client
         *            side, <code>false</code> otherwise
         * @see ComponentEvent
         */
        public FocusEvent(C source, boolean fromClient) {
            super(source, fromClient);
        }
    }

}
