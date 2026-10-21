/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.trigger;

import com.vaadin.flow.component.Component;

/**
 * Common super class for triggers that fire on a DOM {@code MouseEvent}, such
 * as {@link ClickTrigger} and {@link DoubleClickTrigger}. To listen for another
 * mouse event, such as {@code "mouseenter"}, use {@link DomEventTrigger}.
 *
 * @since 25.4
 */
public abstract class MouseEventTrigger extends DomEventTrigger {

    /**
     * Creates a mouse-event trigger that fires when the host receives a DOM
     * event with the given name.
     *
     * @param host
     *            the component whose root element listens for the event, not
     *            {@code null}
     * @param eventName
     *            the DOM mouse-event name, not {@code null}
     */
    protected MouseEventTrigger(Component host, String eventName) {
        super(host, eventName);
    }
}
