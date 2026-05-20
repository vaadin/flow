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
 * Fires on the host component's {@code click} DOM event and exposes the click
 * coordinates and modifier-key state as {@link AbstractArgument}s for
 * downstream actions.
 * <p>
 * Example:
 *
 * <pre>{@code
 * ClickTrigger click = new ClickTrigger(button);
 * click.triggers(new SetPropertyAction<>(xField, "value", click.screenX()),
 *         new SetPropertyAction<>(yField, "value", click.screenY()));
 * }</pre>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ClickTrigger extends DomEventTrigger {

    /**
     * Creates a click trigger on the given host component's root element.
     *
     * @param host
     *            the component to listen on, not {@code null}
     */
    public ClickTrigger(Component host) {
        super(host, "click");
    }

    /** {@code event.screenX} — X coordinate relative to the screen. */
    public AbstractArgument<Integer> screenX() {
        return property("screenX");
    }

    /** {@code event.screenY} — Y coordinate relative to the screen. */
    public AbstractArgument<Integer> screenY() {
        return property("screenY");
    }

    /** {@code event.clientX} — X coordinate relative to the viewport. */
    public AbstractArgument<Integer> clientX() {
        return property("clientX");
    }

    /** {@code event.clientY} — Y coordinate relative to the viewport. */
    public AbstractArgument<Integer> clientY() {
        return property("clientY");
    }

    /** {@code event.shiftKey} — whether shift was held during the click. */
    public AbstractArgument<Boolean> shiftKey() {
        return property("shiftKey");
    }

    /** {@code event.ctrlKey} — whether ctrl was held during the click. */
    public AbstractArgument<Boolean> ctrlKey() {
        return property("ctrlKey");
    }

    /** {@code event.altKey} — whether alt was held during the click. */
    public AbstractArgument<Boolean> altKey() {
        return property("altKey");
    }

    /** {@code event.metaKey} — whether meta was held during the click. */
    public AbstractArgument<Boolean> metaKey() {
        return property("metaKey");
    }
}
