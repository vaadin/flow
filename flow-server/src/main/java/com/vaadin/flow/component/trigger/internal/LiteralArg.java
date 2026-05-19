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

import org.jspecify.annotations.Nullable;

/**
 * Argument backed by a server-side literal that is JSON-encoded into the JS at
 * build time. Lets actions take {@code Argument<? extends T>} uniformly while
 * still accepting plain constants from callers.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value
 */
final class LiteralArg<T> extends AbstractArgument<T> {

    private final @Nullable T value;

    LiteralArg(@Nullable T value) {
        this.value = value;
    }

    @Override
    protected void appendExpression(JsBuilder builder, StringBuilder out) {
        out.append(JsBuilder.json(value));
    }
}
