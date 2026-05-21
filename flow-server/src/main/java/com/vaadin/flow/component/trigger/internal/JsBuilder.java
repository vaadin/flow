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
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

/**
 * Collects element references and produces JS placeholders for them while a
 * trigger builds its {@link com.vaadin.flow.dom.Element#addJsInitializer
 * addJsInitializer} expression.
 * <p>
 * The host element is always {@code this} inside the wrapper; other elements
 * are appended to the parameter list and referenced as {@code $0}, {@code $1},
 * … (reusing the same index when the same element is referenced more than
 * once).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class JsBuilder implements Serializable {

    private final Trigger trigger;
    private final List<@Nullable Object> params = new ArrayList<>();
    private final Map<Element, String> paramByElement = new IdentityHashMap<>();

    JsBuilder(Trigger trigger) {
        this.trigger = trigger;
    }

    /**
     * The trigger this builder is collecting JS for. Used by handler-scoped
     * arguments ({@link HandlerInput}) to refuse being rendered into a
     * different trigger's handler.
     */
    Trigger trigger() {
        return trigger;
    }

    /**
     * Returns a JS expression that evaluates to the given element at runtime.
     * Returns {@code "this"} for the host; otherwise allocates a parameter
     * placeholder.
     */
    String reference(Element element) {
        if (element == trigger.getHost()) {
            return "this";
        }
        String ref = paramByElement.get(element);
        if (ref == null) {
            ref = "$" + params.size();
            params.add(element);
            paramByElement.put(element, ref);
        }
        return ref;
    }

    /**
     * Allocates a fresh {@code $N} placeholder for an arbitrary capture value
     * and returns the reference. Use this for non-{@link Element} values such
     * as {@link com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration}
     * — which materialises on the client as a function that calls the
     * server-side handler — so an action can splice the resulting JS function
     * into its expression. Each call allocates a new placeholder; values are
     * not de-duplicated.
     *
     * @param value
     *            the value to capture; must be a type supported as a parameter
     *            to {@link com.vaadin.flow.dom.Element#executeJs}, may be
     *            {@code null}
     * @return the JS placeholder ({@code "$N"}) referencing the captured value
     */
    String capture(@Nullable Object value) {
        String ref = "$" + params.size();
        params.add(value);
        return ref;
    }

    /**
     * Allocates a {@code $N} placeholder for a server-side callback function: a
     * JS function {@code (payload) => …} that, when called from client JS,
     * deserialises its first argument to {@code T} via Jackson and invokes
     * {@code handler} on the UI thread.
     * <p>
     * Use this from {@link Action#appendStatement} when an action needs to ship
     * a typed payload back from the browser — async API outcomes, extracted
     * event data, anything Jackson can deserialise.
     * <p>
     * Each call registers a fresh {@link ReturnChannelRegistration} on the
     * trigger's host node; channels are not deduplicated across calls.
     *
     * @param payloadType
     *            type to deserialise the first JS argument into; never
     *            {@code null}
     * @param handler
     *            invoked on the UI thread with the deserialised payload, or
     *            {@code null} if the JS argument is {@code null}/missing; never
     *            {@code null}
     * @return the JS placeholder ({@code "$N"}) referencing the callback
     * @param <T>
     *            payload type
     */
    <T> String callback(Class<T> payloadType,
            SerializableConsumer<@Nullable T> handler) {
        return capture(registerChannel(arg -> handler.accept(arg == null ? null
                : JacksonUtils.readValue(arg, payloadType))));
    }

    /**
     * Like {@link #callback(Class, SerializableConsumer)} but accepts a
     * {@link TypeReference} so the payload type can be a parameterised type
     * (e.g. {@code new TypeReference<List<Foo>>(){}}).
     *
     * @param payloadType
     *            type reference to deserialise the first JS argument into;
     *            never {@code null}
     * @param handler
     *            invoked on the UI thread with the deserialised payload, or
     *            {@code null} if the JS argument is {@code null}/missing; never
     *            {@code null}
     * @return the JS placeholder ({@code "$N"}) referencing the callback
     * @param <T>
     *            payload type
     */
    <T> String callback(TypeReference<T> payloadType,
            SerializableConsumer<@Nullable T> handler) {
        return capture(registerChannel(arg -> handler.accept(arg == null ? null
                : JacksonUtils.readValue(arg, payloadType))));
    }

    private ReturnChannelRegistration registerChannel(
            SerializableConsumer<@Nullable JsonNode> dispatcher) {
        return trigger.getHost().getNode().getFeature(ReturnChannelMap.class)
                .registerChannel(args -> dispatcher
                        .accept(args.isEmpty() || args.get(0).isNull() ? null
                                : args.get(0)));
    }

    /**
     * Returns the captures collected by this builder, in the order they were
     * first referenced — these become the captures of the handler
     * {@link com.vaadin.flow.dom.JsFunction}.
     */
    Object[] captures() {
        return params.toArray();
    }

    /**
     * Encodes a value as a JS literal via Jackson. Strings are JSON-quoted,
     * numbers/booleans/null become themselves, records and POJOs become JS
     * object literals.
     */
    static String json(@Nullable Object value) {
        return JacksonUtils.createNode(value).toString();
    }
}
