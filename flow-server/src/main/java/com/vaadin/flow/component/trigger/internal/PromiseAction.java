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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

/**
 * Base class for actions that run a JS expression yielding a <a href=
 * "https://developer.mozilla.org/docs/Web/JavaScript/Reference/Global_Objects/Promise">Promise</a>
 * and optionally report the outcome back to the server.
 * <p>
 * Many gesture-bound browser APIs are asynchronous — clipboard, fullscreen,
 * file picker, share, web payment, … — and follow the same shape: call the API,
 * then handle the resolved value or the rejection. This class collapses that
 * pattern into one place so subclasses only need to emit the promise-yielding
 * JS expression by overriding
 * {@link #appendPromiseExpression(JsBuilder, StringBuilder)}.
 * <p>
 * Two construction modes, mirroring the
 * {@link com.vaadin.flow.component.geolocation.Geolocation Geolocation} API:
 * <ul>
 * <li>Fire-and-forget — {@link #PromiseAction()} — the rendered JS is just the
 * promise expression; the server never sees the outcome.</li>
 * <li>With outcome handling —
 * {@link #PromiseAction(SerializableRunnable, SerializableConsumer)} — supply
 * an {@code onSuccess} runnable and an {@code onError} consumer (both required;
 * pass {@code () -> {}} or {@code err -> {}} to opt out of one). The handlers
 * run on the UI thread after the client reports the outcome of the
 * promise.</li>
 * </ul>
 * Under the hood, the with-outcome mode renders one call to a shared
 * {@link JsFunction} that subscribes to the promise and pushes an
 * {@link Outcome} record through a lazily-registered
 * {@link ReturnChannelRegistration} on the trigger host node.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class PromiseAction extends Action {

    /**
     * Wrapper JS: subscribes to {@code promise} and forwards the resolved value
     * or rejection reason to {@code channel} as an {@link Outcome}-shaped
     * object. Shared across all subclasses with callbacks — the only per-call
     * inputs are the promise expression and the return channel.
     */
    private static final JsFunction OBSERVE_PROMISE = JsFunction.of("""
            promise.then(() => channel({ok: true}))\
            .catch(e => channel({ok: false, \
            error: e && e.message ? e.message : String(e)}));""")
            .withArguments("promise", "channel");

    private final @Nullable SerializableRunnable onSuccess;
    private final @Nullable SerializableConsumer<String> onError;
    private final Map<StateNode, ReturnChannelRegistration> channelByNode = new IdentityHashMap<>();

    /**
     * Creates a fire-and-forget action: the promise's outcome is not reported
     * back to the server.
     */
    protected PromiseAction() {
        this.onSuccess = null;
        this.onError = null;
    }

    /**
     * Creates an action whose promise outcome is reported back to the server.
     * {@code onSuccess} runs after the promise resolves; {@code onError}
     * receives the browser's error message after the promise rejects. Both run
     * on the UI thread. Both consumers are required and must be non-null — pass
     * {@code () -> {}} or {@code err -> {}} to opt out of one.
     *
     * @param onSuccess
     *            invoked on the UI thread after the client reports the promise
     *            resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error message
     *            after the client reports the promise rejected, not
     *            {@code null}
     */
    protected PromiseAction(SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        this.onSuccess = Objects.requireNonNull(onSuccess,
                "onSuccess must not be null");
        this.onError = Objects.requireNonNull(onError,
                "onError must not be null");
    }

    /**
     * Subclasses append a JS expression that evaluates to a {@code Promise}
     * when the trigger fires. The result of that promise is what
     * {@code onSuccess}/{@code onError} observe; the resolved value itself is
     * not delivered to the server (only success vs. failure and the rejection
     * message).
     *
     * @param builder
     *            collects element parameter references, not {@code null}
     * @param out
     *            buffer to append into, not {@code null}
     */
    protected abstract void appendPromiseExpression(JsBuilder builder,
            StringBuilder out);

    @Override
    protected final void appendStatement(JsBuilder builder, StringBuilder out) {
        if (onSuccess == null) {
            appendPromiseExpression(builder, out);
            return;
        }
        String observe = builder.capture(OBSERVE_PROMISE);
        String channel = builder
                .capture(channelFor(builder.trigger().getHost().getNode()));
        out.append(observe).append('(');
        appendPromiseExpression(builder, out);
        out.append(", ").append(channel).append(')');
    }

    private ReturnChannelRegistration channelFor(StateNode hostNode) {
        return channelByNode.computeIfAbsent(hostNode,
                node -> node.getFeature(ReturnChannelMap.class)
                        .registerChannel(this::dispatch));
    }

    private void dispatch(ArrayNode args) {
        // Channel is only registered when callbacks were supplied, so these
        // are non-null at the point of dispatch.
        SerializableRunnable success = Objects.requireNonNull(onSuccess);
        SerializableConsumer<String> error = Objects.requireNonNull(onError);
        Outcome outcome = JacksonUtils.readValue(args.get(0), Outcome.class);
        if (outcome.ok()) {
            success.run();
        } else {
            error.accept(outcome.error() == null ? "" : outcome.error());
        }
    }

    /**
     * Wire shape exchanged between {@link #OBSERVE_PROMISE} and
     * {@link #dispatch}. {@code error} is only meaningful when
     * {@code ok == false}.
     */
    private record Outcome(boolean ok,
            @Nullable String error) implements Serializable {
    }
}
