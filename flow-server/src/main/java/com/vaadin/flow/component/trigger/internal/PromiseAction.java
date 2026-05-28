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
 * JS function by overriding {@link #toPromiseJs(Trigger)}.
 * <p>
 * The type parameter {@code T} is the type of value the JS promise resolves
 * with: it gets Jackson-decoded once before {@code onSuccess} sees it, so
 * subclasses don't write per-action adapters. Use {@code Void} when the promise
 * resolves with no meaningful value (e.g. {@code requestFullscreen}).
 * <p>
 * Two construction modes:
 * <ul>
 * <li>Fire-and-forget — {@link #PromiseAction()} — the rendered JS is just the
 * promise expression; the server never sees the outcome.</li>
 * <li>With outcome handling —
 * {@link #PromiseAction(Class, SerializableConsumer, SerializableConsumer)} —
 * supply the payload type, an {@code onSuccess} consumer receiving the decoded
 * value (or {@code null} if the promise resolved with {@code undefined}), and
 * an {@code onError} consumer receiving an {@link Error} record (all required;
 * pass {@code v -> {}} or {@code err -> {}} to opt out). The handlers run on
 * the UI thread after the client reports the outcome of the promise.</li>
 * </ul>
 * Under the hood, the with-outcome mode wraps the subclass JsFunction in a
 * shared observer JsFunction that subscribes to the promise and pushes an
 * {@link Outcome} through a lazily-registered {@link ReturnChannelRegistration}
 * on the trigger host node.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            type the JS promise resolves with, decoded once on the server
 */
public abstract class PromiseAction<T> extends Action {

    /**
     * Wrapper JS: subscribes to {@code promise} and forwards the resolved value
     * or rejection reason to {@code channel} as an {@link Outcome}-shaped
     * object. Resolved values are forwarded verbatim — the framework decodes
     * them once on the server before invoking {@code onSuccess}. Errors are
     * split into {@code name} (typically the {@code DOMException} class such as
     * {@code "NotAllowedError"} — useful for switching) and a free-form
     * {@code message}.
     */
    private static final JsFunction OBSERVE_PROMISE = JsFunction.of("""
            promise.then(value => channel({ok: true, value: value}))\
            .catch(e => channel({ok: false, error: {\
            name: (e && e.name) ? e.name : '',\
            message: (e && e.message) ? e.message : String(e)}}));""")
            .withArguments("promise", "channel");

    private final @Nullable Class<T> payloadType;
    private final @Nullable SerializableConsumer<@Nullable T> onSuccess;
    private final @Nullable SerializableConsumer<Error> onError;
    private final Map<StateNode, ReturnChannelRegistration> channelByNode = new IdentityHashMap<>();

    /**
     * Creates a fire-and-forget action: the promise's outcome is not reported
     * back to the server.
     */
    protected PromiseAction() {
        this.payloadType = null;
        this.onSuccess = null;
        this.onError = null;
    }

    /**
     * Creates an action whose promise outcome is reported back to the server.
     * {@code onSuccess} runs after the promise resolves and receives the
     * decoded value (or {@code null} if the promise resolved with
     * {@code undefined}); {@code onError} runs after the promise rejects and
     * receives an {@link Error} record with the browser's error name and
     * message. Both run on the UI thread.
     *
     * @param payloadType
     *            type to deserialise the promise's resolved value to, not
     *            {@code null}; use {@code Void.class} when the promise has no
     *            meaningful value
     * @param onSuccess
     *            invoked on the UI thread with the decoded value after the
     *            client reports the promise resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the promise rejected, not {@code null}
     */
    protected PromiseAction(Class<T> payloadType,
            SerializableConsumer<@Nullable T> onSuccess,
            SerializableConsumer<Error> onError) {
        this.payloadType = Objects.requireNonNull(payloadType,
                "payloadType must not be null");
        this.onSuccess = Objects.requireNonNull(onSuccess,
                "onSuccess must not be null");
        this.onError = Objects.requireNonNull(onError,
                "onError must not be null");
    }

    /**
     * Subclasses return a {@link JsFunction} that, when invoked with the
     * trigger's event, evaluates to a {@code Promise}. The value the promise
     * resolves with is decoded to {@code T} on the server and handed to
     * {@code onSuccess}. To deliver a typed value, subclasses can wrap their
     * API call in an IIFE inside the function body — for example, copying a
     * string and resolving with it:
     *
     * <pre>{@code
     * return JsFunction.of(
     *         "return ((v) => navigator.clipboard.writeText(v).then(() => v))($0(event))",
     *         textInput.toJs(trigger)).withArguments("event");
     * }</pre>
     *
     * @param trigger
     *            the surrounding trigger this render is for, not {@code null}
     * @return the promise-yielding JS function, not {@code null}
     */
    protected abstract JsFunction toPromiseJs(Trigger trigger);

    /**
     * Final by design — subclasses customise the rendered JS through
     * {@link #toPromiseJs}, never by overriding the wiring that subscribes to
     * the promise. Keeping the {@code .then}/{@code .catch} glue identical
     * across subclasses is what makes the {@link Outcome} wire contract stable.
     */
    @Override
    protected final JsFunction toJs(Trigger trigger) {
        JsFunction inner = toPromiseJs(trigger);
        // The constructors enforce that onSuccess and onError are either
        // both null (fire-and-forget) or both non-null (with-outcome) — so
        // checking one already determines the mode.
        if (onSuccess == null && onError == null) {
            // Fire-and-forget: the trigger handler invokes the inner function
            // directly with event; the returned Promise is discarded.
            return inner;
        }
        ReturnChannelRegistration channel = channelFor(
                trigger.getHost().getNode());
        // Invoke the inner function to get a Promise, then hand it plus the
        // return channel to the shared observer JsFunction which subscribes
        // to .then/.catch.
        return JsFunction.of("observer(inner(event), channel)")
                .withParameter("observer", OBSERVE_PROMISE)
                .withParameter("inner", inner).withParameter("channel", channel)
                .withArguments("event");
    }

    private ReturnChannelRegistration channelFor(StateNode hostNode) {
        return channelByNode.computeIfAbsent(hostNode,
                node -> node.getFeature(ReturnChannelMap.class)
                        .registerChannel(this::dispatch));
    }

    private void dispatch(ArrayNode args) {
        // Channel is only registered when callbacks were supplied, so these
        // are non-null at the point of dispatch.
        var success = Objects.requireNonNull(onSuccess);
        var error = Objects.requireNonNull(onError);
        var type = Objects.requireNonNull(payloadType);
        Outcome outcome = JacksonUtils.readValue(args.get(0), Outcome.class);
        if (outcome.ok()) {
            JsonNode rawValue = outcome.value();
            // Void.class deserialisation always yields null — letting Jackson
            // handle it keeps the no-value branch out of this dispatch.
            T value = (rawValue == null || rawValue.isNull()) ? null
                    : JacksonUtils.readValue(rawValue, type);
            success.accept(value);
        } else {
            Error err = outcome.error();
            error.accept(err == null ? new Error("", "") : err);
        }
    }

    /**
     * Information delivered to the {@code onError} consumer after the promise
     * rejects. {@link #name()} carries the rejection's class name — typically a
     * {@code DOMException} like {@code "NotAllowedError"} — which is what
     * callers usually switch on. {@link #message()} carries the free-form
     * description and is best used for logging or display.
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
