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

/**
 * Base class for {@link Argument} implementations. Subclasses produce the JS
 * expression that yields the argument's value when the trigger fires by
 * overriding {@link #appendExpression(JsBuilder, StringBuilder)}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value produced
 */
public abstract non-sealed class AbstractArgument<T> implements Argument<T> {

    /**
     * Appends this argument's JS expression to {@code out}. Element references
     * must go through {@link JsBuilder#reference}.
     *
     * @param builder
     *            collects element parameter references, not {@code null}
     * @param out
     *            buffer to append into, not {@code null}
     */
    protected abstract void appendExpression(JsBuilder builder,
            StringBuilder out);
}
