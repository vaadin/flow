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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.Trigger;

/**
 * Entry point for the browser Fullscreen API. Bind a fullscreen request to a
 * user gesture by chaining off {@link #on(ClickNotifier)} for the common
 * click-to-fullscreen case, or {@link #on(Trigger)} for any other trigger:
 *
 * <pre>{@code
 * Button fullscreenButton = new Button("Fullscreen");
 * Fullscreen.on(fullscreenButton).requestPage();
 *
 * Fullscreen.on(fullscreenButton).requestComponent(videoPanel);
 * }</pre>
 *
 * For non-click DOM gestures (e.g. {@code dblclick}, {@code keydown}) or custom
 * triggers, construct a {@link Trigger} explicitly and pass it to
 * {@link #on(Trigger)}.
 * <p>
 * The Fullscreen API requires transient user activation for each request, so
 * the request only runs during the DOM event that fires the underlying trigger.
 * Calling {@link Page#exitFullscreen()} to leave fullscreen, or observing
 * {@link Page#fullscreenSignal()} to react to state changes, does not need a
 * gesture and is available imperatively on {@link Page}.
 */
public final class Fullscreen implements Serializable {

    private Fullscreen() {
        // utility class
    }

    /**
     * Returns a binding whose underlying trigger fires when the given component
     * is clicked. Sugar for {@code on(new ClickTrigger(component))} that avoids
     * exposing the trigger framework's internal types for the common case.
     *
     * @param trigger
     *            the component to listen for clicks on, not {@code null}
     * @return a new binding
     */
    public static FullscreenBinding on(ClickNotifier<?> trigger) {
        Objects.requireNonNull(trigger, "trigger must not be null");
        return new FullscreenBinding(new ClickTrigger((Component) trigger));
    }

    /**
     * Returns a binding whose underlying trigger is the given one. Use this for
     * non-click gestures or any add-on trigger.
     *
     * @param trigger
     *            the trigger, not {@code null}
     * @return a new binding
     */
    public static FullscreenBinding on(Trigger trigger) {
        Objects.requireNonNull(trigger, "trigger must not be null");
        return new FullscreenBinding(trigger);
    }
}
