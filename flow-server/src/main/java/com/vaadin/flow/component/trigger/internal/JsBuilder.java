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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;

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

    private final AbstractTrigger trigger;
    private final List<Object> params = new ArrayList<>();
    private final Map<Element, String> paramByElement = new IdentityHashMap<>();

    JsBuilder(AbstractTrigger trigger) {
        this.trigger = trigger;
    }

    /**
     * The trigger this builder is collecting JS for. Used by handler-scoped
     * arguments ({@link HandlerExprArg}) to refuse being rendered into a
     * different trigger's handler.
     */
    AbstractTrigger trigger() {
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
     * Returns the element captures collected by this builder, in the order they
     * were first referenced — these become the captures of the handler
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
