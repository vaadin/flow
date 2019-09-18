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
