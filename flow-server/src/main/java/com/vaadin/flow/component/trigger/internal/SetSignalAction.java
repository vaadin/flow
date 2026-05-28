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

import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Named {@link CallbackAction} that calls {@code set} on a {@link ValueSignal}
 * — the common case of bridging a client-side trigger into server-side signal
 * state. For other server-side destinations (logging, custom events, queues, …)
 * use {@link CallbackAction} directly with a method reference.
 *
 * <pre>{@code
 * ValueSignal<String> valueSignal = new ValueSignal<>("");
 * DomEventTrigger input = new DomEventTrigger(textField, "input");
 * input.triggers(new SetSignalAction<>(valueSignal, String.class,
 *         new PropertyInput<>(textField, "value", String.class)));
 * }</pre>
 *
 * <p>
 * The signal's own equality checker debounces redundant updates, so emitting
 * identical values back-to-back does not retrigger downstream effects.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the type the signal holds and the JSON value is decoded to
 */
public class SetSignalAction<T> extends CallbackAction<T> {

    /**
     * Creates an action that, when the trigger fires, evaluates {@code source}
     * on the client, sends the value to the server, decodes it as
     * {@code valueType}, and assigns it to {@code signal} via
     * {@link ValueSignal#set(Object)}.
     *
     * @param signal
     *            the local value signal to update on the server, not
     *            {@code null}
     * @param valueType
     *            runtime type the JSON value is decoded to before being passed
     *            to {@link ValueSignal#set(Object)}, not {@code null}
     * @param source
     *            input that produces the value on the client when the trigger
     *            fires, not {@code null}
     */
    public SetSignalAction(ValueSignal<? super T> signal, Class<T> valueType,
            Action.Input<? extends T> source) {
        super(valueType,
                Objects.requireNonNull(signal, "signal must not be null")::set,
                source);
    }
}
