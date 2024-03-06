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
 * Mixin interface to handle blur events on components.
 *
 * @param <T>
 *            the type of the component returned at the
 *            {@link BlurEvent#getSource()}
 * @since 1.0
 */
public interface BlurNotifier<T extends Component> extends Serializable {

    /**
     * Add a listener to blur DOM events.
     *
     * @param listener
     *            the blur listener
     * @return a registration that can be used to unregister the listener
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Events/blur">blur
     *      event at MDN</a>
     */
    default Registration addBlurListener(
            ComponentEventListener<BlurEvent<T>> listener) {
        if (this instanceof Component) {
            return ComponentUtil.addListener((Component) this, BlurEvent.class,
                    (ComponentEventListener) listener);
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addBlurListener"));
        }
    }

    /**
     * Represents the DOM event "blur".
     *
     * @param <C>
     *            The source component type.
     */
    @DomEvent("blur")
    class BlurEvent<C extends Component> extends ComponentEvent<C> {

        /**
         * BlurEvent base constructor.
         *
         * @param source
         *            the source component
         * @param fromClient
         *            <code>true</code> if the event originated from the client
         *            side, <code>false</code> otherwise
         * @see ComponentEvent
         */
        public BlurEvent(C source, boolean fromClient) {
            super(source, fromClient);
        }
    }
}
