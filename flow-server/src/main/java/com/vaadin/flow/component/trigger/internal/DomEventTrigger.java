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

import java.util.Objects;

import com.vaadin.flow.component.Component;

/**
 * Fires when the host component receives a DOM event with the given name. The
 * bound actions run inside the browser's event handler, preserving the
 * user-gesture context (so downstream actions may invoke APIs gated on a
 * gesture, such as clipboard or fullscreen).
 * <p>
 * Examples:
 *
 * <pre>{@code
 * new DomEventTrigger(button, "click").triggers(action);
 * new DomEventTrigger(input, "input").triggers(action);
 * new DomEventTrigger(panel, "pointerdown").triggers(action);
 * }</pre>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class DomEventTrigger extends Trigger {

    private final String eventName;

    /**
     * Creates a trigger that fires when the host receives a DOM event with the
     * given name.
     *
     * @param host
     *            the component whose root element listens for the event, not
     *            {@code null}
     * @param eventName
     *            the DOM event name (e.g. {@code "click"}, {@code "input"}),
     *            not {@code null}
     */
    public DomEventTrigger(Component host, String eventName) {
        super(host);
        this.eventName = Objects.requireNonNull(eventName);
    }

    /**
     * Returns an {@link Action.Input} that yields {@code event[name]} at fire
     * time — the value of a property on the DOM event object.
     * <p>
     * The returned input is only valid in actions wired to this trigger; using
     * it elsewhere throws {@link IllegalArgumentException} at the
     * {@link #triggers(Action...)} call site.
     *
     * @param name
     *            the event property name (e.g. {@code "screenX"},
     *            {@code "key"}), not {@code null}
     * @return an input that resolves to {@code event[name]} on fire
     * @param <T>
     *            the runtime type of the value produced
     */
    public <T> Action.Input<T> property(String name) {
        Objects.requireNonNull(name);
        return new HandlerInput<>("event[" + JsBuilder.json(name) + "]", this);
    }

    @Override
    protected String installJs() {
        String evt = JsBuilder.json(eventName);
        return "this.addEventListener(" + evt + ", $0);"
                + "return () => this.removeEventListener(" + evt + ", $0);";
    }
}
