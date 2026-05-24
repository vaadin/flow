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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.internal.JacksonUtils;

/**
 * Render-time context passed to {@link Action#render} and
 * {@link Action.Input#toJs}. Exposes the surrounding {@link Trigger} so Actions
 * and Inputs can identify their host element (for return channels,
 * scope-checks, etc.) without taking it as a separate parameter.
 * <p>
 * Element references, value literals, and other captures are encoded by the
 * {@link com.vaadin.flow.dom.JsFunction} each Action/Input returns; this
 * context does not collect captures across renders.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class JsBuilder implements Serializable {

    private final Trigger trigger;

    JsBuilder(Trigger trigger) {
        this.trigger = trigger;
    }

    /**
     * The trigger this render is for. Used by handler-scoped inputs (see
     * {@link HandlerInput}) to refuse rendering into a different trigger, and
     * by promise-based actions to register return channels on the trigger
     * host's state node.
     */
    Trigger trigger() {
        return trigger;
    }

    /**
     * Encodes {@code value} as a JS literal via Jackson. Strings are
     * JSON-quoted, numbers/booleans/null become themselves, records and POJOs
     * become JS object literals.
     * <p>
     * Used by {@link Trigger} subclasses whose {@code installJs()} returns a
     * raw JS string (not a {@link com.vaadin.flow.dom.JsFunction}) and so
     * cannot let JsFunction's capture machinery do the encoding for them.
     */
    static String json(@Nullable Object value) {
        return JacksonUtils.createNode(value).toString();
    }
}
