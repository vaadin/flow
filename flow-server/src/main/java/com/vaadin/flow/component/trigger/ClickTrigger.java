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
import com.vaadin.flow.dom.Element;

/**
 * Fires when the host element receives a {@code click} DOM event. The bound
 * actions run inside the click handler, preserving the browser's user-gesture
 * context (so downstream actions may invoke APIs gated on a gesture, such as
 * clipboard or fullscreen).
 */
public class ClickTrigger extends AbstractTrigger {

    public static final String TYPE_ID = "flow:click";

    /**
     * Creates a click trigger bound to the given host.
     *
     * @param host
     *            the element whose click event should fire this trigger, not
     *            {@code null}
     */
    public ClickTrigger(Element host) {
        super(TYPE_ID, host);
    }

    /**
     * Creates a click trigger bound to the given component's root element.
     *
     * @param host
     *            the component whose click event should fire this trigger, not
     *            {@code null}
     */
    public ClickTrigger(Component host) {
        super(TYPE_ID, host);
    }
}
