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
package com.vaadin.flow.dom;

import com.vaadin.flow.shared.Registration;

/**
 * A registration for configuring or removing a DOM event listener added to an
 * element.
 *
 * @see Element#addEventListener(String, DomEventListener)
 *
 * @author Vaadin Ltd
 */
public interface DomListenerRegistration extends Registration {
    /**
     * Add a JavaScript expression for extracting event data. When an event is
     * fired in the browser, the expression is evaluated and its value is sent
     * back to the server. The expression is evaluated in a context where
     * <code>element</code> refers to this element and <code>event</code> refers
     * to the fired event.
     * <p>
     * The result of the evaluation is available in
     * {@link DomEvent#getEventData()} with the expression as the key in the
     * JSON object. An expression might be e.g.
     *
     * <ul>
     * <li><code>element.value</code> the get the value of an input element for
     * a change event.
     * <li><code>event.button === 0</code> to get true for click events
     * triggered by the primary mouse button.
     * </ul>
     *
     * @param eventData
     *            definition for data that should be passed back to the server
     *            together with the event, not <code>null</code>
     * @return this registration, for chaining
     */
    DomListenerRegistration addEventData(String eventData);

    /**
     * Configure whether this listener will be called even in cases when the
     * element is disabled.
     *
     * @param disabledUpdateMode
     *            controls RPC communication from the client side to the server
     *            side when the element is disabled, not {@code null}
     *
     * @return this registration, for chaining
     */
    DomListenerRegistration setDisabledUpdateMode(
            DisabledUpdateMode disabledUpdateMode);
}
