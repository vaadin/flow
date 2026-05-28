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
import java.util.concurrent.atomic.AtomicLong;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.signals.Signal;

/**
 * Reads the current value of a server-side {@link Signal} at the moment a
 * trigger fires.
 * <p>
 * When this input is rendered into a trigger's handler — at the
 * {@link Trigger#triggers(Action...)} call site — a component-scoped
 * {@link Signal#effect(Component, com.vaadin.flow.signals.function.EffectAction)
 * effect} is installed once. It mirrors the signal value to a uniquely-named
 * JavaScript property on the {@code owner} component's element via
 * {@link Element#executeJs}; every subsequent signal change pushes the new
 * value to the client. The trigger reads that property at fire time, so the
 * value seen reflects whatever the signal held the last time it propagated.
 * <p>
 * Constructing a {@code SignalInput} has no side effects — no effect is
 * installed and no JS is queued until the input is wired into a trigger.
 * <p>
 * The {@code owner} drives the lifecycle: while it is detached the effect is
 * suspended (and the property on the client retains the last pushed value); on
 * re-attach the effect re-emits the current value.
 *
 * <pre>{@code
 * ValueSignal<String> textSignal = new ValueSignal<>("Hello");
 * new ClickTrigger(copyButton).triggers(new WriteToClipboardAction(
 *         new SignalInput<>(this, textSignal), null));
 * textSignal.set("Goodbye");
 * // Clicking the button after the set copies "Goodbye".
 * }</pre>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value produced
 */
public class SignalInput<T> extends Action.Input<T> {

    // Generates unique property-name suffixes; a long won't realistically
    // overflow. Class reload during devmode hotdeploy resets it, but the
    // matching SignalInput instances reload with it, so no live collision
    // can result.
    private static final AtomicLong PROPERTY_INDEX = new AtomicLong();

    private final Component owner;
    private final Signal<T> signal;
    private final String propertyName;
    private boolean installed;

    /**
     * Creates a signal-backed input. No effect is installed until the input is
     * actually rendered into a trigger handler.
     *
     * @param owner
     *            the component whose lifecycle bounds the signal effect and
     *            whose element holds the mirrored value; not {@code null}
     * @param signal
     *            the signal whose value should be read at fire time, not
     *            {@code null}
     */
    public SignalInput(Component owner, Signal<T> signal) {
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.signal = Objects.requireNonNull(signal, "signal must not be null");
        // A unique property name lets multiple SignalInputs share an owner
        // without overwriting each other's mirrored value. The name is
        // internal — chosen to be unlikely to collide with anything the
        // application sets on the element.
        this.propertyName = "__vTriggerSignalInput_"
                + PROPERTY_INDEX.getAndIncrement();
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        installEffectIfNeeded();
        // Same shape as PropertyInput — both read a named property from a
        // known element.
        return JsFunction.of("return target[propertyName]")
                .withParameter("target", owner.getElement())
                .withParameter("propertyName", propertyName);
    }

    private void installEffectIfNeeded() {
        if (installed) {
            return;
        }
        installed = true;
        Element target = owner.getElement();
        Signal.effect(owner, () -> target.executeJs("this[$0]=$1", propertyName,
                signal.get()));
    }
}
