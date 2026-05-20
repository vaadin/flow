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

/**
 * Input whose value is produced by a JS expression scoped to a specific
 * trigger's handler — typically {@code event[...]} for a
 * {@link DomEventTrigger}.
 * <p>
 * Carries the owning trigger so that {@link Trigger#triggers(Action...)}
 * refuses to render an input created by trigger A into the handler of trigger B
 * (where the referenced variable would not be in scope).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value produced
 */
final class HandlerInput<T> extends Input<T> {

    private final String jsExpression;
    private final Trigger owner;

    HandlerInput(String jsExpression, Trigger owner) {
        this.jsExpression = Objects.requireNonNull(jsExpression);
        this.owner = Objects.requireNonNull(owner);
    }

    @Override
    protected void appendExpression(JsBuilder builder, StringBuilder out) {
        if (builder.trigger() != owner) {
            throw new IllegalArgumentException(
                    "Input is scoped to a different trigger and cannot be"
                            + " used here");
        }
        out.append(jsExpression);
    }
}
