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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

/**
 * Forwards a value from the trigger's handler scope back to the server and
 * hands it to a {@link SerializableConsumer} on the UI thread. The generic
 * counterpart to client-side actions like {@link SetPropertyAction} — same
 * input/source story, except the destination is a Java callback instead of a JS
 * assignment.
 * <p>
 * Use this as the bridge from a trigger to arbitrary server-side state — log a
 * value, fire a custom event, push to a queue, etc. The {@link SetSignalAction}
 * subclass is the named convenience for the common case of forwarding into a
 * {@link com.vaadin.flow.signals.local.ValueSignal}.
 * <p>
 * Example — log the screen-X coordinate of each click:
 *
 * <pre>{@code
 * ClickTrigger click = new ClickTrigger(button);
 * click.triggers(new CallbackAction<>(Integer.class,
 *         x -> logger.info("clicked at {}", x), click.screenX()));
 * }</pre>
 *
 * <p>
 * The rendered JS calls a {@link ReturnChannelRegistration} that, on the
 * server, decodes the JSON value as {@code T} and invokes the callback. A null
 * value on the wire (a JSON {@code null} or a missing argument) is rejected
 * with {@link IllegalStateException} — callbacks see decoded, non-null values
 * only. A separate channel is registered per host node, so the same action
 * instance can be wired to triggers hosted on different elements without
 * leaking registrations across them.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the type the JSON value is decoded to and the callback receives
 */
public class CallbackAction<T> extends Action {

    private final Class<T> valueType;
    private final SerializableConsumer<T> callback;
    private final Action.Input<? extends T> source;
    private final Map<StateNode, ReturnChannelRegistration> channelByNode = new IdentityHashMap<>();

    /**
     * Creates an action that, when the trigger fires, evaluates {@code source}
     * on the client, sends the value to the server, decodes it as
     * {@code valueType}, and hands it to {@code callback} on the UI thread.
     *
     * @param valueType
     *            runtime type the JSON value is decoded to before being passed
     *            to {@code callback}, not {@code null}
     * @param callback
     *            invoked on the UI thread with the decoded value, not
     *            {@code null}
     * @param source
     *            input that produces the value on the client when the trigger
     *            fires, not {@code null}
     */
    public CallbackAction(Class<T> valueType, SerializableConsumer<T> callback,
            Action.Input<? extends T> source) {
        this.valueType = Objects.requireNonNull(valueType,
                "valueType must not be null");
        this.callback = Objects.requireNonNull(callback,
                "callback must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
    }

    @Override
    protected final JsFunction render(Trigger trigger) {
        // $0 = the return channel; $1 = the source input's JsFunction.
        // Invoking the source with `event` produces its value, which is
        // forwarded straight into the channel call.
        return JsFunction.of("$0($1(event));",
                channelFor(trigger.getHost().getNode()), source.toJs(trigger))
                .withArguments("event");
    }

    private ReturnChannelRegistration channelFor(StateNode hostNode) {
        return channelByNode.computeIfAbsent(hostNode,
                node -> node.getFeature(ReturnChannelMap.class)
                        .registerChannel(this::dispatch));
    }

    private void dispatch(ArrayNode args) {
        JsonNode raw = args.get(0);
        // The source input is expected to evaluate to a defined value at fire
        // time; a JSON null on the wire (or a missing argument) almost
        // certainly indicates a misuse — for example, mapping an action's
        // input from a property that doesn't exist on the target.
        if (raw == null || raw.isNull()) {
            throw new IllegalStateException(
                    "CallbackAction received a null value from the client;"
                            + " the source input must produce a non-null"
                            + " value");
        }
        callback.accept(JacksonUtils.readValue(raw, valueType));
    }
}
