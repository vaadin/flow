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
package com.vaadin.client.flow.binding;

import java.util.function.Consumer;

import jsinterop.annotations.JsType;

import com.vaadin.client.JsRunnable;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsSet;

import elemental.dom.Node;

/**
 * Manages debouncing of events. Pure {@code @JsType(isNative=true)} binding to
 * the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/binding/Debouncer.ts}.
 *
 * <p>
 * Use {@link #getOrCreate(Node, String, double)} to either create a new
 * instance or get an existing one tracking a sequence of similar events.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.binding", name = "Debouncer")
public class Debouncer {

    /**
     * Informs this debouncer that an event has occurred.
     *
     * @return {@code true} if the event should be processed as-is without
     *         delaying
     */
    public native boolean trigger(JsSet<String> phases,
            Consumer<String> command, JsMap<String, JsRunnable> commands);

    /**
     * Gets an existing debouncer or creates a new one associated with the given
     * DOM node, identifier and debounce timeout.
     */
    public static native Debouncer getOrCreate(Node element, String identifier,
            double debounce);

    /**
     * Flushes all pending changes. Returns the send-commands actually executed
     * during the flush so callers can avoid double-executing.
     */
    public static native JsArray<Consumer<String>> flushAll();
}
