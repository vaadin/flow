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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Asks the browser to fullscreen the target component's root element via
 * {@code Element.requestFullscreen()} when the bound trigger fires.
 * <p>
 * The Fullscreen API requires transient user activation (a click, key press, …)
 * — calling {@code requestFullscreen} from a server push or constructor is
 * rejected by the browser. Bind this action to a {@link Trigger} that fires
 * during such a gesture, typically a {@link ClickTrigger}, so the call happens
 * synchronously inside the handler and inherits the gesture.
 * <p>
 * This action is intentionally low-level: it calls {@code requestFullscreen}
 * directly on the target's root element. That doesn't interact well with Vaadin
 * theming or overlay components, which expect the fullscreen element to be
 * {@code document.documentElement}. A higher-level
 * {@code Component.requestFullscreen()} facade — see PR vaadin/flow#24326 —
 * handles the wrapping needed for full Vaadin compatibility; this action is the
 * trigger-framework primitive it builds on (or a direct option when the Vaadin
 * wrapping isn't needed).
 * <p>
 * Outcome handling extends {@link PromiseAction}: use the target-only
 * constructor for fire-and-forget, or the overload taking
 * {@code onSuccess}/{@code onError} consumers. The promise resolves with
 * {@code undefined} so {@code onSuccess} is a {@link SerializableRunnable} with
 * no value, but {@code onError} receives a {@link PromiseAction.Error} record
 * with the browser's error name and message — the spec-documented rejection is
 * {@code NotAllowedError} (no gesture / permissions policy).
 *
 * <pre>{@code
 * RequestFullscreenAction goFs = new RequestFullscreenAction(panel,
 *         () -> notification.show("Fullscreen entered"),
 *         err -> notification.show("Fullscreen denied: " + err.message()));
 * new ClickTrigger(button).triggers(goFs);
 * }</pre>
 *
 * For internal use only. May be renamed or removed in a future release.
 */
public class RequestFullscreenAction extends PromiseAction<Void> {

    private final Element target;

    /**
     * Creates a fire-and-forget fullscreen action: the rendered JS just calls
     * {@code requestFullscreen()} and the server never sees the outcome.
     *
     * @param target
     *            the component whose root element to fullscreen, not
     *            {@code null}
     */
    public RequestFullscreenAction(Component target) {
        super();
        this.target = Objects.requireNonNull(target, "target must not be null")
                .getElement();
    }

    /**
     * Creates a fullscreen action whose outcome is reported back to the server.
     *
     * @param target
     *            the component whose root element to fullscreen, not
     *            {@code null}
     * @param onSuccess
     *            invoked on the UI thread after the client reports
     *            {@code requestFullscreen} resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports {@code requestFullscreen} rejected, not
     *            {@code null}
     */
    public RequestFullscreenAction(Component target,
            SerializableRunnable onSuccess,
            SerializableConsumer<Error> onError) {
        // The promise resolves with undefined — value is always null — so the
        // runnable shape is the natural fit; one-line adapter wires it to the
        // generic Consumer<@Nullable Void> the base class expects.
        super(Void.class, runnableAsConsumer(onSuccess), onError);
        this.target = Objects.requireNonNull(target, "target must not be null")
                .getElement();
    }

    private static SerializableConsumer<@Nullable Void> runnableAsConsumer(
            SerializableRunnable onSuccess) {
        Objects.requireNonNull(onSuccess, "onSuccess must not be null");
        return ignored -> onSuccess.run();
    }

    @Override
    protected JsFunction renderPromiseExpression(Trigger trigger) {
        // $0 = target element captured by JsFunction; reified on the client
        // as the DOM node.
        return JsFunction.of("return $0.requestFullscreen()", target);
    }
}
