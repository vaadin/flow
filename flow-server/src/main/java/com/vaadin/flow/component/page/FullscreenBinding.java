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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.PromiseAction;
import com.vaadin.flow.component.trigger.internal.RequestFullscreenAction;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Fluent surface returned from {@link Fullscreen#on}. Each {@code request*}
 * verb attaches one {@link RequestFullscreenAction} to the underlying
 * {@link Trigger} and returns a {@link FullscreenRequest} for removal.
 * <p>
 * Verbs come in two flavours: fire-and-forget (no callbacks) and observed (with
 * {@code onSuccess}/{@code onError} callbacks). Both consumers are required in
 * the observed form — pass {@code () -> {}} or {@code err -> {}} to opt out of
 * one.
 *
 * <pre>{@code
 * Button enter = new Button("Fullscreen");
 * Fullscreen.on(enter).requestComponent(videoPanel,
 *         () -> Notification.show("Entered fullscreen"),
 *         err -> Notification.show("Could not enter fullscreen: " + err));
 * }</pre>
 *
 * Component-fullscreen requests move the target into a Vaadin wrapper element
 * so that themes and overlay components (Notification, ComboBox popups, …) keep
 * working while fullscreen is active. The component is restored to its original
 * position when fullscreen exits, whether via {@link Page#exitFullscreen()},
 * the user pressing Escape, or a later request superseding this one.
 */
public final class FullscreenBinding implements Serializable {

    private final Trigger trigger;

    FullscreenBinding(Trigger trigger) {
        this.trigger = Objects.requireNonNull(trigger);
    }

    /**
     * Fullscreens the entire page ({@code document.documentElement}) when the
     * underlying trigger fires. Themes and overlay components work correctly in
     * this mode. Use {@link #requestComponent(Component)} to fullscreen a
     * single component instead.
     *
     * @return a registration that removes the binding when removed
     */
    public FullscreenRequest requestPage() {
        return bind(new RequestFullscreenAction());
    }

    /**
     * Like {@link #requestPage()} but reports the outcome back to the server.
     *
     * @param onSuccess
     *            UI-thread callback on success, not {@code null}
     * @param onError
     *            UI-thread callback on failure with the browser's error
     *            message, not {@code null}
     * @return a registration that removes the binding when removed
     */
    public FullscreenRequest requestPage(SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        return bind(new RequestFullscreenAction(onSuccess,
                wrapErrorConsumer(onError)));
    }

    /**
     * Fullscreens the given component when the underlying trigger fires. The
     * component is moved into a Vaadin wrapper element and the rest of the view
     * is hidden so themes and overlay components keep working. The component is
     * restored to its original DOM position when fullscreen exits.
     *
     * @param component
     *            the component to fullscreen, not {@code null}; must be
     *            attached to a UI when the binding is created
     * @return a registration that removes the binding when removed
     * @throws IllegalStateException
     *             if {@code component} is not attached to a UI
     */
    public FullscreenRequest requestComponent(Component component) {
        Objects.requireNonNull(component, "component must not be null");
        return bind(new RequestFullscreenAction(component));
    }

    /**
     * Like {@link #requestComponent(Component)} but reports the outcome back to
     * the server.
     *
     * @param component
     *            the component to fullscreen, not {@code null}
     * @param onSuccess
     *            UI-thread callback on success, not {@code null}
     * @param onError
     *            UI-thread callback on failure with the browser's error
     *            message, not {@code null}
     * @return a registration that removes the binding when removed
     * @throws IllegalStateException
     *             if {@code component} is not attached to a UI
     */
    public FullscreenRequest requestComponent(Component component,
            SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        Objects.requireNonNull(component, "component must not be null");
        return bind(new RequestFullscreenAction(component, onSuccess,
                wrapErrorConsumer(onError)));
    }

    private static SerializableConsumer<PromiseAction.Error> wrapErrorConsumer(
            SerializableConsumer<String> onError) {
        Objects.requireNonNull(onError, "onError must not be null");
        return err -> onError.accept(err.message());
    }

    private FullscreenRequest bind(RequestFullscreenAction action) {
        trigger.triggers(action);
        return trigger::remove;
    }
}
