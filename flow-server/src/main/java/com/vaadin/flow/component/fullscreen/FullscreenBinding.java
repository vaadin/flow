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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.PromiseAction.Error;
import com.vaadin.flow.component.trigger.internal.RequestFullscreenAction;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Fluent surface returned from {@link Fullscreen#onClick}, used to declare what
 * a click should make fullscreen. Call {@link #enter()} for the whole page or
 * {@link #enter(Component)} for a single component. The request runs in the
 * browser at click time, while the user gesture required by the Fullscreen API
 * is still valid.
 * <p>
 * Actions come in two flavours: fire-and-forget (no callbacks) and observed
 * (with {@code onSuccess}/{@code onError} callbacks). {@code onError} receives
 * an {@link Error} record with the browser's error name and message — the
 * spec-documented rejection is {@code NotAllowedError} (no gesture /
 * permissions policy). Both consumers are required in the observed form — pass
 * {@code () -> {}} or {@code err -> {}} to opt out of one.
 *
 * <pre>{@code
 * Button fullscreenButton = new Button("Fullscreen");
 * Fullscreen.onClick(fullscreenButton).enter(videoPanel,
 *         () -> Notification.show("Entered fullscreen"), err -> Notification
 *                 .show("Could not enter fullscreen: " + err.message()));
 * }</pre>
 *
 * Component fullscreen moves the target into a Vaadin wrapper element so that
 * themes and overlay components (Notification, ComboBox popups, …) keep working
 * while fullscreen is active. The component is restored to its original
 * position when fullscreen exits, whether via {@link Fullscreen#exit()}, the
 * user pressing Escape, or a later request superseding this one.
 *
 * @since 25.2
 */
public final class FullscreenBinding implements Serializable {

    private final Trigger trigger;

    FullscreenBinding(Trigger trigger) {
        this.trigger = Objects.requireNonNull(trigger);
    }

    /**
     * Enters fullscreen for the entire page ({@code document.documentElement})
     * when the underlying trigger fires. Themes and overlay components work
     * correctly in this mode. Use {@link #enter(Component)} to fullscreen a
     * single component instead.
     */
    public void enter() {
        bind(new RequestFullscreenAction());
    }

    /**
     * Like {@link #enter()} but reports the outcome back to the server.
     *
     * @param onSuccess
     *            UI-thread callback on success, not {@code null}
     * @param onError
     *            UI-thread callback on failure with the browser's error, not
     *            {@code null}
     */
    public void enter(SerializableRunnable onSuccess,
            SerializableConsumer<Error> onError) {
        bind(new RequestFullscreenAction(onSuccess, onError));
    }

    /**
     * Enters fullscreen for the given component when the underlying trigger
     * fires. The component is moved into a Vaadin wrapper element and the rest
     * of the view is hidden so themes and overlay components keep working. The
     * component is restored to its original DOM position when fullscreen exits.
     * <p>
     * Safe to call before {@code component} is attached: the action is wired to
     * the trigger as soon as the component attaches.
     *
     * @param component
     *            the component to fullscreen, not {@code null}
     */
    public void enter(Component component) {
        Objects.requireNonNull(component, "component must not be null");
        whenAttached(component,
                () -> bind(new RequestFullscreenAction(component)));
    }

    /**
     * Like {@link #enter(Component)} but reports the outcome back to the
     * server.
     *
     * @param component
     *            the component to fullscreen, not {@code null}
     * @param onSuccess
     *            UI-thread callback on success, not {@code null}
     * @param onError
     *            UI-thread callback on failure with the browser's error, not
     *            {@code null}
     */
    public void enter(Component component, SerializableRunnable onSuccess,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(component, "component must not be null");
        whenAttached(component, () -> bind(
                new RequestFullscreenAction(component, onSuccess, onError)));
    }

    private static void whenAttached(Component component, Runnable task) {
        // Defer wiring until attach so the action's wrapper-element lookup
        // (target.getUI().getInternals().getWrapperElement()) has a UI to
        // resolve against, and so that RequestFullscreenAction's visibility
        // check sees the same visibility state as the install JS would.
        // runWhenAttached fires immediately if already attached and is
        // one-shot otherwise.
        component.getElement().getNode().runWhenAttached(ui -> task.run());
    }

    private void bind(RequestFullscreenAction action) {
        trigger.triggers(action);
    }
}
