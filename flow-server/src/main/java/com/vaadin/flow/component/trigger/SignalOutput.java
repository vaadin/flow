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
package com.vaadin.flow.component.trigger;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.trigger.internal.ConfigContext;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Output backed by a server-side {@link Signal}. The signal's current value is
 * snapshotted into the client config at each emit; a host-scoped effect
 * subscribes to the signal so that every change re-emits the snapshot.
 * <p>
 * Snapshot semantics: actions read the latest value at trigger fire time. This
 * is a value reader, not a graph builder — composing computed outputs remains
 * the signal layer's job (use {@link Signal#cached}).
 *
 * <pre>{@code
 * ValueSignal<String> locale = ...;
 * new ClickTrigger(button).triggers(
 *         new ClipboardCopyAction(new SignalOutput<>(String.class, locale)));
 * }</pre>
 *
 * The effect that wires the re-emit is created lazily on the first
 * {@code buildClientConfig} call against an attached host, and is cleaned up
 * automatically by {@link ElementEffect} when the host detaches.
 *
 * @param <T>
 *            the runtime type of the produced value
 */
public class SignalOutput<T> extends AbstractOutput<T> {

    public static final String TYPE_ID = "flow:signal-value";

    private final Signal<T> signal;

    private transient boolean effectInitialRun;
    private transient @Nullable Registration effectRegistration;

    /**
     * Creates a signal-backed output.
     *
     * @param valueType
     *            runtime type of the produced value, not {@code null}
     * @param signal
     *            the source signal, not {@code null}
     */
    public SignalOutput(Class<T> valueType, Signal<T> signal) {
        super(TYPE_ID, valueType);
        this.signal = Objects.requireNonNull(signal);
    }

    /**
     * Convenience for the common case: pair a {@link ValueSignal} with an
     * output of the same value type.
     *
     * @param signal
     *            the source signal, not {@code null}
     * @param valueType
     *            runtime type, not {@code null}
     * @param <T>
     *            the value type
     * @return a new SignalOutput
     */
    public static <T> SignalOutput<T> of(ValueSignal<T> signal,
            Class<T> valueType) {
        return new SignalOutput<>(valueType, signal);
    }

    /**
     * @return the source signal
     */
    public Signal<T> getSignal() {
        return signal;
    }

    @Override
    public ObjectNode buildClientConfig(ConfigContext context) {
        Element host = context.getHost();
        if (effectRegistration == null && host.getNode().isAttached()) {
            // Install once per attach. ElementEffect re-runs the action
            // whenever any signal read inside it changes; the first run
            // happens immediately to discover dependencies, so we skip
            // it explicitly here.
            effectInitialRun = true;
            effectRegistration = ElementEffect.effect(host, () -> {
                // Read the signal to register the dependency.
                signal.get();
                if (effectInitialRun) {
                    effectInitialRun = false;
                } else {
                    context.scheduleSync();
                }
            });
        }
        ObjectNode node = JacksonUtils.createObjectNode();
        node.set("value", JacksonUtils.getMapper().valueToTree(signal.peek()));
        return node;
    }
}
