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
 * Input backed by a server-side literal that is captured into the rendered
 * {@link JsFunction} and Jackson-encoded into a JS value on the client. Lets
 * actions take {@code Action.Input<? extends T>} uniformly while still
 * accepting plain constants from callers — e.g. copying a fixed string to the
 * clipboard:
 *
 * <pre>{@code
 * new ClickTrigger(button).triggers(new WriteToClipboardAction(
 *         new LiteralInput<>("hello"), null, copied -> {
 *         }, err -> {
 *         }));
 * }</pre>
 *
 * <p>
 * The value is required to be non-null: {@code null} as a literal payload
 * almost never matches a sensible browser API call (e.g.
 * {@code writeText(null)} writes the string {@code "null"} to the clipboard).
 * Actions that need to emit a literal {@code null} should do so through their
 * own mechanism — see {@link SetPropertyAction}'s null-clearing convenience
 * constructor.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value
 */
public final class LiteralInput<T> extends Action.Input<T> {

    private final T value;

    /**
     * Creates a literal input wrapping the given value.
     *
     * @param value
     *            the value to encode, not {@code null}
     */
    public LiteralInput(T value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        // The value is captured (not stringified into the body), so Jackson
        // handles all encoding — quoting, escaping, nested objects, etc.
        return JsFunction.of("return $0", value);
    }
}
