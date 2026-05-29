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
package com.vaadin.flow.component.trigger.internal;

import com.vaadin.flow.component.Component;

/**
 * Fires on the host component's {@code click} DOM event. The event's
 * coordinates and modifier-key state are available as static
 * {@link Action.Input} sources on {@link MouseEventTrigger.Output} (also
 * reachable as {@code ClickTrigger.Output} through inheritance).
 * <p>
 * Example — on click, mirror the screen coordinates of the click into two input
 * fields' {@code value} properties:
 *
 * <pre>{@code
 * ClickTrigger click = new ClickTrigger(button);
 * click.triggers(
 *         new SetPropertyAction<>(xField, "value",
 *                 ClickTrigger.Output.screenX),
 *         new SetPropertyAction<>(yField, "value",
 *                 ClickTrigger.Output.screenY));
 * }</pre>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ClickTrigger extends MouseEventTrigger {

    /**
     * Creates a click trigger on the given host component's root element.
     *
     * @param host
     *            the component to listen on, not {@code null}
     */
    public ClickTrigger(Component host) {
        super(host, "click");
    }
}
