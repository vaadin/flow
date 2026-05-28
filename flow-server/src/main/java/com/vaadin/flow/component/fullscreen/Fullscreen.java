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
package com.vaadin.flow.component.fullscreen;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;

/**
 * Entry point for the browser Fullscreen API. Bind a fullscreen request to a
 * user gesture by chaining off {@link #onClick(Component)}:
 *
 * <pre>{@code
 * Button fullscreenButton = new Button("Fullscreen");
 * Fullscreen.onClick(fullscreenButton).enter();
 *
 * Fullscreen.onClick(fullscreenButton).enter(videoPanel);
 * }</pre>
 *
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
     * Registers the given component as a clickable trigger for a fullscreen
     * request — the common shape for fullscreen buttons. Equivalent to
     * {@code new ClickTrigger(component)}, without making callers reach for the
     * trigger framework's internal types.
     *
     * @param component
     *            the component to listen for clicks on, not {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a new binding that can chain a fullscreen request to this trigger
     */
    public static <T extends Component & ClickNotifier<?>> FullscreenBinding onClick(
            T component) {
        Objects.requireNonNull(component, "component must not be null");
        return new FullscreenBinding(new ClickTrigger(component));
    }
}
