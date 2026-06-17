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

import com.vaadin.flow.dom.JsFunction;

/**
 * Input that reads a property from a trigger handler's {@code event} argument
 * (e.g. {@code event.screenX} for a {@link DomEventTrigger}).
 * <p>
 * Carries the trigger class the expression is valid for so that
 * {@link Trigger#triggers(Action...)} refuses to render an input meant for one
 * trigger family into the handler of an unrelated trigger (where the referenced
 * variable would not be in scope). Class-based scoping lets a single input
 * instance be exposed as a {@code public static final} field (see
 * {@link MouseEventTrigger.EventData}) and reused across every instance of the
 * owning class and its subclasses. Custom trigger families expose their event
 * state the same way: declare a {@code HandlerInput} per property instead of
 * subclassing {@link Action.Input} by hand.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value produced
 */
public final class HandlerInput<T> extends Action.Input<T> {

    private final String propertyName;
    private final Class<? extends Trigger> ownerClass;

    /**
     * Creates an input that yields {@code event[propertyName]} at fire time,
     * valid in the handler of any trigger that is an instance of
     * {@code ownerClass}.
     *
     * @param propertyName
     *            the event property name, not {@code null}
     * @param ownerClass
     *            the trigger class the expression is valid for, not
     *            {@code null}
     */
    public HandlerInput(String propertyName,
            Class<? extends Trigger> ownerClass) {
        this.propertyName = Objects.requireNonNull(propertyName);
        this.ownerClass = Objects.requireNonNull(ownerClass);
    }

    @Override
    public JsFunction toJs(Trigger trigger) {
        if (!ownerClass.isInstance(trigger)) {
            throw new IllegalArgumentException("Input is scoped to "
                    + ownerClass.getSimpleName() + " and cannot be used in a "
                    + trigger.getClass().getSimpleName() + " handler");
        }
        // $0 = property name (string capture, Jackson-quoted on the client),
        // event = the handler argument the framework passes in.
        return JsFunction.of("return event[$0]", propertyName)
                .withArguments("event");
    }
}
