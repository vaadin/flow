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
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.signals.Signal;

/**
 * Entry point for the browser Fullscreen API.
 * <p>
 * To enter fullscreen, bind a request to a user gesture via
 * {@link #onClick(Component)} — the browser requires transient user activation
 * for each request, so the call only runs during the DOM event that fires the
 * underlying trigger:
 *
 * <pre>{@code
 * Button fullscreenButton = new Button("Fullscreen");
 * Fullscreen.onClick(fullscreenButton).enter(); // whole page
 * Fullscreen.onClick(fullscreenButton).enter(videoPanel); // single component
 * }</pre>
 *
 * To leave fullscreen, call {@link #exit()}; to observe the current state,
 * subscribe to {@link #stateSignal()}. Neither needs a user gesture.
 *
 * @since 25.2
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

    /**
     * Returns a read-only signal that tracks the browser's fullscreen state for
     * the current UI.
     * <p>
     * The signal distinguishes between {@link FullscreenState#FULLSCREEN
     * FULLSCREEN} (the page is currently in fullscreen),
     * {@link FullscreenState#NOT_FULLSCREEN NOT_FULLSCREEN} (fullscreen is
     * supported but the page is not in it), {@link FullscreenState#UNSUPPORTED
     * UNSUPPORTED} (the browser does not support fullscreen or the document is
     * not permitted to enter it), and {@link FullscreenState#UNKNOWN UNKNOWN}
     * (the initial value, replaced with a real one before any user code
     * observes the signal).
     * <p>
     * The signal value is seeded from the initial client bootstrap, so user
     * code always sees a real value. Subscribe with
     * {@code Signal.effect(owner, ...)} to react to changes; call
     * {@code stateSignal().peek()} for a snapshot outside a reactive context,
     * and {@code .get()} inside one.
     * <p>
     * <b>Example: toggle a CSS class while fullscreen</b>
     *
     * <pre>{@code
     * viewLayout.bindClassName("is-fullscreen",
     *         Fullscreen.stateSignal().map(s -> s == FullscreenState.FULLSCREEN));
     * }</pre>
     *
     * <b>Example: react to state changes with a side effect</b>
     *
     * <pre>{@code
     * Signal.effect(this, () -> {
     *     if (Fullscreen.stateSignal().get() == FullscreenState.UNSUPPORTED) {
     *         fullscreenButton.setVisible(false);
     *     }
     * });
     * }</pre>
     *
     * @return the read-only fullscreen signal for the current UI
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static Signal<FullscreenState> stateSignal() {
        return support(currentUi()).stateSignal();
    }

    /**
     * Exits fullscreen mode for the current UI if the page is currently in
     * fullscreen; otherwise a no-op. The current state can be observed via
     * {@link #stateSignal()}.
     * <p>
     * If a component was previously fullscreened via
     * {@link FullscreenBinding#enter(Component)}, it is automatically restored
     * to its original position in the DOM.
     *
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void exit() {
        support(currentUi()).exit();
    }

    /**
     * Sets the fullscreen state of the given UI from a raw client-side value
     * (e.g. from the initial bootstrap parameters). {@code null} and unknown
     * values are ignored.
     * <p>
     * Called from the bootstrap path in {@code ExtendedClientDetails} that
     * seeds the signal before any user code observes it. Not intended for
     * application code.
     *
     * @param ui
     *            the UI whose fullscreen state to seed, not {@code null}
     * @param value
     *            the raw value, or {@code null}
     */
    public static void setStateFromClient(UI ui, String value) {
        Objects.requireNonNull(ui, "ui must not be null");
        support(ui).setStateFromClient(value);
    }

    private static UI currentUi() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException(
                    "Fullscreen API requires a current UI — UI.getCurrent() is null");
        }
        return ui;
    }

    private static FullscreenSupport support(UI ui) {
        FullscreenSupport support = ComponentUtil.getData(ui,
                FullscreenSupport.class);
        if (support == null) {
            support = new FullscreenSupport(ui);
            ComponentUtil.setData(ui, FullscreenSupport.class, support);
        }
        return support;
    }
}
