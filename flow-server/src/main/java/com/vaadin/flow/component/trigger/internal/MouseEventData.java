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

import com.vaadin.flow.component.trigger.Action;
import com.vaadin.flow.component.trigger.MouseEventTrigger;
import com.vaadin.flow.component.trigger.Trigger;

/**
 * The {@code MouseEvent} properties of a {@link MouseEventTrigger} exposed as
 * static {@link ActionInput} sources. Each field reads one property off the DOM
 * event that fires the trigger; pass any of them as the value source of an
 * {@link Action} wired to a {@link MouseEventTrigger} subclass.
 * <p>
 * Each field is bound to {@link MouseEventTrigger}; using it in the handler of
 * an unrelated trigger (e.g. a keyboard trigger) throws
 * {@link IllegalArgumentException} at {@link Trigger#triggers(Action...)} time.
 * <p>
 * For internal use only. May be renamed or removed in any release.
 *
 * @since 25.4
 */
public final class MouseEventData {

    private MouseEventData() {
    }

    /** {@code event.screenX} — X coordinate relative to the screen. */
    public static final ActionInput<Integer> screenX = new HandlerInput<>(
            "screenX", MouseEventTrigger.class);

    /** {@code event.screenY} — Y coordinate relative to the screen. */
    public static final ActionInput<Integer> screenY = new HandlerInput<>(
            "screenY", MouseEventTrigger.class);

    /** {@code event.clientX} — X coordinate relative to the viewport. */
    public static final ActionInput<Integer> clientX = new HandlerInput<>(
            "clientX", MouseEventTrigger.class);

    /** {@code event.clientY} — Y coordinate relative to the viewport. */
    public static final ActionInput<Integer> clientY = new HandlerInput<>(
            "clientY", MouseEventTrigger.class);

    /**
     * {@code event.button} — which mouse button changed state: {@code 0} main
     * (usually left), {@code 1} auxiliary (usually middle), {@code 2} secondary
     * (usually right), {@code 3}/{@code 4} fourth (back) / fifth (forward)
     * browser buttons.
     */
    public static final ActionInput<Integer> button = new HandlerInput<>(
            "button", MouseEventTrigger.class);

    /** {@code event.shiftKey} — whether shift was held during the event. */
    public static final ActionInput<Boolean> shiftKey = new HandlerInput<>(
            "shiftKey", MouseEventTrigger.class);

    /** {@code event.ctrlKey} — whether ctrl was held during the event. */
    public static final ActionInput<Boolean> ctrlKey = new HandlerInput<>(
            "ctrlKey", MouseEventTrigger.class);

    /** {@code event.altKey} — whether alt was held during the event. */
    public static final ActionInput<Boolean> altKey = new HandlerInput<>(
            "altKey", MouseEventTrigger.class);

    /** {@code event.metaKey} — whether meta was held during the event. */
    public static final ActionInput<Boolean> metaKey = new HandlerInput<>(
            "metaKey", MouseEventTrigger.class);
}
