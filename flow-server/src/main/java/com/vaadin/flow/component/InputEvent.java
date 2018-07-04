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

/**
 * Event fired when the component has received any type of input (e.g. click,
 * key press).
 * <p>
 * This event is specifically intended to the used for the <code>input</code>
 * event in the DOM API.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@DomEvent("input")
public class InputEvent extends ComponentEvent<Component> {
    /**
     * Creates a new input event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     */
    public InputEvent(Component source, boolean fromClient) {
        super(source, fromClient);
    }
}
