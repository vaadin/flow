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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Asks the browser to enter fullscreen when the bound trigger fires.
 * <p>
 * Two modes:
 * <ul>
 * <li><b>Page</b> — calls {@code window.Vaadin.Flow.fullscreen
 * .requestPageFullscreen()} which fullscreens {@code document.documentElement}.
 * Themes and overlay components keep working.</li>
 * <li><b>Component</b> — calls {@code window.Vaadin.Flow.fullscreen
 * .requestComponentFullscreen(element, wrapper)} which moves the target into
 * the Vaadin wrapper element, hides the rest of the view, and fullscreens
 * {@code document.documentElement}. The component is restored to its original
 * position when fullscreen exits (programmatically, via Escape, or a
 * superseding request).</li>
 * </ul>
 * <p>
 * The Fullscreen API requires transient user activation (a click, key press, …)
 * — calling {@code requestFullscreen} from a server push or constructor is
 * rejected by the browser. Bind this action to a {@link Trigger} that fires
 * during such a gesture, typically a {@link ClickTrigger}, so the call happens
 * synchronously inside the handler and inherits the gesture.
 * <p>
 * Outcome handling extends {@link PromiseAction}: use the fire-and-forget
 * constructors, or the overloads taking {@code onSuccess}/{@code onError}
 * consumers. The promise resolves with {@code undefined} so {@code onSuccess}
 * is a {@link SerializableRunnable} with no value, but {@code onError} receives
 * a {@link PromiseAction.Error} record with the browser's error name and
 * message — the spec-documented rejection is {@code NotAllowedError} (no
 * gesture / permissions policy).
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

    private final @Nullable Element target;
    private final @Nullable Element wrapper;

    /**
     * Creates a fire-and-forget page-level fullscreen action. The rendered JS
     * calls {@code requestPageFullscreen()} and the server never sees the
     * outcome.
     */
    public RequestFullscreenAction() {
        super();
        this.target = null;
        this.wrapper = null;
    }

    /**
     * Creates a page-level fullscreen action whose outcome is reported back to
     * the server.
     *
     * @param onSuccess
     *            invoked on the UI thread after the client reports the request
     *            resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the request rejected, not {@code null}
     */
    public RequestFullscreenAction(SerializableRunnable onSuccess,
            SerializableConsumer<Error> onError) {
        super(Void.class, runnableAsConsumer(onSuccess), onError);
        this.target = null;
        this.wrapper = null;
    }

    /**
     * Creates a fire-and-forget component-level fullscreen action. The
     * component is moved into the UI's wrapper element so themes and overlay
     * components keep working.
     *
     * @param target
     *            the component to fullscreen, not {@code null}; must be
     *            attached to a UI
     */
    public RequestFullscreenAction(Component target) {
        super();
        Objects.requireNonNull(target, "target must not be null");
        this.target = target.getElement();
        this.wrapper = resolveWrapper(target);
    }

    /**
     * Creates a component-level fullscreen action whose outcome is reported
     * back to the server.
     *
     * @param target
     *            the component to fullscreen, not {@code null}; must be
     *            attached to a UI
     * @param onSuccess
     *            invoked on the UI thread after the client reports the request
     *            resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the request rejected, not {@code null}
     */
    public RequestFullscreenAction(Component target,
            SerializableRunnable onSuccess,
            SerializableConsumer<Error> onError) {
        super(Void.class, runnableAsConsumer(onSuccess), onError);
        Objects.requireNonNull(target, "target must not be null");
        this.target = target.getElement();
        this.wrapper = resolveWrapper(target);
    }

    private static Element resolveWrapper(Component target) {
        UI ui = target.getUI().orElseThrow(() -> new IllegalStateException(
                "Component must be attached to a UI to fullscreen it"));
        Element wrapper = ui.getInternals().getWrapperElement();
        if (wrapper == null) {
            // Real applications create the wrapper during UI init
            // (UI.doInit). The only place this is null in practice is a unit
            // test bypassing init — surface that clearly so the test can call
            // ui.getInternals().createWrapperElement() during setup.
            throw new IllegalStateException(
                    "UI wrapper element is not available — UI init has not run yet");
        }
        return wrapper;
    }

    private static SerializableConsumer<@Nullable Void> runnableAsConsumer(
            SerializableRunnable onSuccess) {
        Objects.requireNonNull(onSuccess, "onSuccess must not be null");
        return ignored -> onSuccess.run();
    }

    @Override
    protected void appendPromiseExpression(JsBuilder builder,
            StringBuilder out) {
        if (target == null) {
            out.append("window.Vaadin.Flow.fullscreen.requestPageFullscreen()");
        } else {
            // wrapper is non-null when target is non-null
            out.append(
                    "window.Vaadin.Flow.fullscreen.requestComponentFullscreen(")
                    .append(builder.reference(target)).append(", ")
                    .append(builder.reference(Objects.requireNonNull(wrapper)))
                    .append(")");
        }
    }
}
