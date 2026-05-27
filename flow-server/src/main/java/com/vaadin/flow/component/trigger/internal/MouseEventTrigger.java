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

import java.io.Serializable;

import com.vaadin.flow.component.Component;

/**
 * Common super class for triggers that fire on a DOM {@code MouseEvent} —
 * {@link ClickTrigger}, {@link DoubleClickTrigger}, and any future mouse-event
 * trigger. Exposes the {@code MouseEvent} properties shared by all of them as
 * static {@link Action.Input} fields on {@link Output}.
 * <p>
 * The {@code Output} fields are bound to {@code MouseEventTrigger.class}, so
 * the same field instance can be used as the source for any
 * {@code MouseEventTrigger} subclass:
 *
 * <pre>{@code
 * ClickTrigger click = new ClickTrigger(button);
 * click.triggers(new SetPropertyAction<>(xField, "value",
 *         ClickTrigger.Output.screenX));
 *
 * DoubleClickTrigger dbl = new DoubleClickTrigger(button);
 * dbl.triggers(new SetPropertyAction<>(xField, "value",
 *         DoubleClickTrigger.Output.screenX));
 * }</pre>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MouseEventTrigger extends DomEventTrigger {

    /**
     * Creates a mouse-event trigger that fires when the host receives a DOM
     * event with the given name. Subclasses such as {@link ClickTrigger} bind a
     * specific event name; instantiate this class directly only when none of
     * the dedicated subclasses fits (e.g. for {@code "mouseover"}).
     *
     * @param host
     *            the component whose root element listens for the event, not
     *            {@code null}
     * @param eventName
     *            the DOM mouse-event name (e.g. {@code "click"},
     *            {@code "mousedown"}), not {@code null}
     */
    public MouseEventTrigger(Component host, String eventName) {
        super(host, eventName);
    }

    /**
     * The {@code MouseEvent} properties exposed as static {@link Action.Input}
     * sources. Use these as the value source of an {@link Action} wired to any
     * {@link MouseEventTrigger} subclass.
     * <p>
     * Each field is bound to {@link MouseEventTrigger}; using it in the handler
     * of an unrelated trigger (e.g. a keyboard trigger) throws
     * {@link IllegalArgumentException} at {@link Trigger#triggers(Action...)}
     * time.
     * <p>
     * Trigger subclasses that need their own properties may declare their own
     * nested {@code Output} extending this class — the inherited static fields
     * remain reachable through the subclass (so {@code ClickTrigger.Output
     * .screenX} continues to resolve to {@link #screenX}).
     */
    public abstract static class Output implements Serializable {

        /**
         * The class exists purely as a namespace for the static
         * {@link Action.Input} fields.
         */
        protected Output() {
        }

        /** {@code event.screenX} — X coordinate relative to the screen. */
        public static final Action.Input<Integer> screenX = eventProperty(
                "screenX", MouseEventTrigger.class);

        /** {@code event.screenY} — Y coordinate relative to the screen. */
        public static final Action.Input<Integer> screenY = eventProperty(
                "screenY", MouseEventTrigger.class);

        /** {@code event.clientX} — X coordinate relative to the viewport. */
        public static final Action.Input<Integer> clientX = eventProperty(
                "clientX", MouseEventTrigger.class);

        /** {@code event.clientY} — Y coordinate relative to the viewport. */
        public static final Action.Input<Integer> clientY = eventProperty(
                "clientY", MouseEventTrigger.class);

        /**
         * {@code event.button} — which mouse button changed state: {@code 0}
         * main (usually left), {@code 1} auxiliary (usually middle), {@code 2}
         * secondary (usually right), {@code 3}/{@code 4} fourth (back) / fifth
         * (forward) browser buttons.
         */
        public static final Action.Input<Integer> button = eventProperty(
                "button", MouseEventTrigger.class);

        /** {@code event.shiftKey} — whether shift was held during the event. */
        public static final Action.Input<Boolean> shiftKey = eventProperty(
                "shiftKey", MouseEventTrigger.class);

        /** {@code event.ctrlKey} — whether ctrl was held during the event. */
        public static final Action.Input<Boolean> ctrlKey = eventProperty(
                "ctrlKey", MouseEventTrigger.class);

        /** {@code event.altKey} — whether alt was held during the event. */
        public static final Action.Input<Boolean> altKey = eventProperty(
                "altKey", MouseEventTrigger.class);

        /** {@code event.metaKey} — whether meta was held during the event. */
        public static final Action.Input<Boolean> metaKey = eventProperty(
                "metaKey", MouseEventTrigger.class);
    }
}
