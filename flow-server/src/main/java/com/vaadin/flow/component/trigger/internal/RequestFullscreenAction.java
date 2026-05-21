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
import com.vaadin.flow.dom.Element;
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
 * Note that fullscreening an arbitrary component element does not interact well
 * with Vaadin theming or overlay components — those expect the fullscreen
 * element to be {@code document.documentElement}. This action is intentionally
 * low-level; the higher-level {@code Component.requestFullscreen()} facade
 * handles the wrapping needed for full Vaadin compatibility.
 * <p>
 * Outcome handling is inherited from {@link PromiseAction}: use the target-only
 * constructor for fire-and-forget, or the overload taking
 * {@code onSuccess}/{@code onError} consumers to react to the
 * {@code requestFullscreen} promise on the UI thread.
 *
 * <pre>{@code
 * RequestFullscreenAction goFs = new RequestFullscreenAction(panel,
 *         () -> notification.show("Fullscreen entered"),
 *         err -> notification.show("Fullscreen denied: " + err));
 * new ClickTrigger(button).triggers(goFs);
 * }</pre>
 *
 * For internal use only. May be renamed or removed in a future release.
 */
public class RequestFullscreenAction extends PromiseAction {

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
     * Creates a fullscreen action whose outcome is reported back to the server
     * via {@code onSuccess}/{@code onError}. See {@link PromiseAction} for the
     * contract on both consumers.
     *
     * @param target
     *            the component whose root element to fullscreen, not
     *            {@code null}
     * @param onSuccess
     *            invoked on the UI thread after the client reports
     *            {@code requestFullscreen} resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error message
     *            after the client reports {@code requestFullscreen} rejected,
     *            not {@code null}
     */
    public RequestFullscreenAction(Component target,
            SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        super(onSuccess, onError);
        this.target = Objects.requireNonNull(target, "target must not be null")
                .getElement();
    }

    @Override
    protected void appendPromiseExpression(JsBuilder builder,
            StringBuilder out) {
        out.append(builder.reference(target)).append(".requestFullscreen()");
    }
}
