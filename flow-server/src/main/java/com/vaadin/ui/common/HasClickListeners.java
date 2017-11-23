/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.common;

import com.vaadin.ui.Component;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.event.ComponentEventNotifier;

/**
 * Mixin interface to handle click events on components.
 *
 * @param <T>
 *            the type of the component returned at the
 *            {@link ClickEvent#getSource()}
 */
public interface HasClickListeners<T extends Component>
        extends ComponentEventNotifier {

    /**
     * Add a listener to click DOM events.
     *
     * @param listener
     *            The click listener.
     * @return A registration that can be used to unregister the listener.
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Events/click">click
     *      event at MDN</a>
     */
    default Registration addClickListener(
            ComponentEventListener<ClickEvent<T>> listener) {
        return addListener(ClickEvent.class, (ComponentEventListener) listener);
    }

    /**
     * Class that represents the DOM event "click".
     *
     * @param <C>
     *            The source component type.
     */
    @DomEvent("click")
    class ClickEvent<C extends Component> extends ComponentEvent<C> {

        /**
         * ComponentEvent base constructor.
         *
         * @param source
         *            the source component
         * @param fromClient
         *            <code>true</code> if the event originated from the client
         *            side, <code>false</code> otherwise
         * @see ComponentEvent
         */
        public ClickEvent(C source, boolean fromClient) {
            super(source, fromClient);
        }
    }

}
