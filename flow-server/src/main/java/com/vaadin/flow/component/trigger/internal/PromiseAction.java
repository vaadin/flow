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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
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
 * Under the hood, the with-outcome mode lazily registers one
 * {@link ReturnChannelRegistration} per trigger host node and appends
 * {@code .then(()=>$N(true,null)).catch(e=>$N(false, msg))} to the promise
 * expression, so the client invokes the channel after the promise resolves or
 * rejects.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class PromiseAction extends Action {

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
     * not delivered to the server (only the success/failure flag and the
     * rejection message).
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
        appendPromiseExpression(builder, out);
        if (onSuccess == null) {
            return;
        }
        StateNode hostNode = builder.trigger().getHost().getNode();
        ReturnChannelRegistration channel = channelByNode.computeIfAbsent(
                hostNode, node -> node.getFeature(ReturnChannelMap.class)
                        .registerChannel(this::dispatch));
        String channelRef = builder.capture(channel);
        out.append(".then(()=>").append(channelRef).append("(true,null))")
                .append(".catch(e=>").append(channelRef)
                .append("(false, e && e.message ? e.message : String(e)))");
    }

    private void dispatch(ArrayNode args) {
        // Channel is only registered when callbacks were supplied, so these
        // are non-null at the point of dispatch.
        SerializableRunnable success = Objects.requireNonNull(onSuccess);
        SerializableConsumer<String> error = Objects.requireNonNull(onError);
        boolean succeeded = !args.isEmpty() && args.get(0).asBoolean(false);
        if (succeeded) {
            success.run();
        } else {
            String message = (args.size() > 1 && !args.get(1).isNull())
                    ? args.get(1).asString()
                    : "";
            error.accept(message);
        }
    }
}
