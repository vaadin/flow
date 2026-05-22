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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
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
 * Two construction modes:
 * <ul>
 * <li>Fire-and-forget — {@link #PromiseAction()} — the rendered JS is just the
 * promise expression; the server never sees the outcome.</li>
 * <li>With outcome handling —
 * {@link #PromiseAction(SerializableConsumer, SerializableConsumer)} — supply
 * an {@code onSuccess} consumer receiving a {@link Success} record and an
 * {@code onError} consumer receiving an {@link Error} record (both required;
 * pass {@code s -> {}} or {@code err -> {}} to opt out of one). The handlers
 * run on the UI thread after the client reports the outcome of the
 * promise.</li>
 * </ul>
 * Under the hood, the with-outcome mode renders one call to a shared
 * {@link JsFunction} that subscribes to the promise and pushes an
 * {@link Outcome} through a lazily-registered {@link ReturnChannelRegistration}
 * on the trigger host node.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class PromiseAction extends Action {

    /**
     * Wrapper JS: subscribes to {@code promise} and forwards the resolved value
     * or rejection reason to {@code channel} as an {@link Outcome}-shaped
     * object. Resolved values are forwarded verbatim — subclasses whose promise
     * resolves to a meaningful value (e.g. the text that was just copied)
     * surface it through {@link Success#value()}. Errors are split into
     * {@code name} (typically the {@code DOMException} class such as
     * {@code "NotAllowedError"} — useful for switching) and a free-form
     * {@code message}.
     */
    private static final JsFunction OBSERVE_PROMISE = JsFunction.of("""
            promise.then(value => channel({ok: true, value: value}))\
            .catch(e => channel({ok: false, error: {\
            name: (e && e.name) ? e.name : '',\
            message: (e && e.message) ? e.message : String(e)}}));""")
            .withArguments("promise", "channel");

    private final @Nullable SerializableConsumer<Success> onSuccess;
    private final @Nullable SerializableConsumer<Error> onError;
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
     * {@code onSuccess} runs after the promise resolves and receives a
     * {@link Success} record carrying the resolved value (if the subclass'
     * promise produced one); {@code onError} runs after the promise rejects and
     * receives an {@link Error} record with the browser's error name and
     * message. Both run on the UI thread.
     *
     * @param onSuccess
     *            invoked on the UI thread after the client reports the promise
     *            resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the promise rejected, not {@code null}
     */
    protected PromiseAction(SerializableConsumer<Success> onSuccess,
            SerializableConsumer<Error> onError) {
        this.onSuccess = Objects.requireNonNull(onSuccess,
                "onSuccess must not be null");
        this.onError = Objects.requireNonNull(onError,
                "onError must not be null");
    }

    /**
     * Subclasses append a JS expression that evaluates to a {@code Promise}
     * when the trigger fires. The value the promise resolves with is what
     * {@link Success#value()} receives on the server (a Jackson
     * {@link JsonNode}). To deliver a typed value, subclasses can wrap their
     * API call in an IIFE — for example, copying a string and resolving with
     * it:
     *
     * <pre>{@code
     * ((v) => navigator.clipboard.writeText(v).then(() => v))(<textExpr>)
     * }</pre>
     *
     * @param builder
     *            collects element parameter references, not {@code null}
     * @param out
     *            buffer to append into, not {@code null}
     */
    protected abstract void appendPromiseExpression(JsBuilder builder,
            StringBuilder out);

    /**
     * Final by design — subclasses customise the rendered JS through
     * {@link #appendPromiseExpression}, never by overriding the wiring that
     * subscribes to the promise. Keeping the {@code .then}/{@code .catch} glue
     * identical across subclasses is what makes the {@link Outcome} wire
     * contract stable.
     */
    @Override
    protected final void appendStatement(JsBuilder builder, StringBuilder out) {
        // The constructors enforce that onSuccess and onError are either
        // both null (fire-and-forget) or both non-null (with-outcome) — so
        // checking one already determines the mode. Asserting on both keeps
        // the invariant defensive against constructor changes.
        if (onSuccess == null && onError == null) {
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
        SerializableConsumer<Success> success = Objects
                .requireNonNull(onSuccess);
        SerializableConsumer<Error> error = Objects.requireNonNull(onError);
        Outcome outcome = JacksonUtils.readValue(args.get(0), Outcome.class);
        if (outcome.ok()) {
            success.accept(new Success(outcome.value()));
        } else {
            Error err = outcome.error();
            error.accept(err == null ? new Error("", "") : err);
        }
    }

    /**
     * Information delivered to the {@code onSuccess} consumer after the promise
     * resolves. {@link #value()} is whatever JS value the promise produced (or
     * {@code null} if it was {@code undefined}); subclasses that have nothing
     * meaningful to deliver resolve with {@code undefined} and leave this
     * {@code null}.
     *
     * @param value
     *            the resolved value as a Jackson {@link JsonNode}, or
     *            {@code null} if the promise resolved with {@code undefined}
     */
    public record Success(@Nullable JsonNode value) implements Serializable {
    }

    /**
     * Information delivered to the {@code onError} consumer after the promise
     * rejects. {@link #name()} carries the rejection's class name — typically a
     * {@code DOMException} like {@code "NotAllowedError"},
     * {@code "AbortError"}, … — which is what callers usually switch on.
     * {@link #message()} carries the free-form description and is best used for
     * logging or display.
     *
     * @param name
     *            the rejection's {@code name} property, or the empty string if
     *            the rejection had none
     * @param message
     *            the rejection's {@code message} property, or {@code String(e)}
     *            if there was no {@code message}
     */
    public record Error(String name, String message) implements Serializable {
    }

    /**
     * Wire shape exchanged between {@link #OBSERVE_PROMISE} and
     * {@link #dispatch}. {@code value} is meaningful when {@code ok == true};
     * {@code error} is meaningful when {@code ok == false}.
     */
    private record Outcome(boolean ok, @Nullable JsonNode value,
            @Nullable Error error) implements Serializable {
    }
}
