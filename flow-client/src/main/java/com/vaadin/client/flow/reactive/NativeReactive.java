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
package com.vaadin.client.flow.reactive;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

import com.vaadin.client.JsRunnable;

/**
 * JsInterop binding for the TypeScript {@code Reactive} module published at
 * {@code window.Vaadin.Flow.internal.client.flow.reactive.Reactive}. Source
 * lives in {@code src/main/frontend/internal/client/flow/reactive/Reactive.ts}.
 *
 * <p>
 * Not part of the public API: call {@link Reactive} instead. The public Java
 * class delegates here for the state-management surface and keeps
 * {@link Reactive#runWhenDependenciesChange(com.vaadin.client.Command)} in Java
 * (because that helper constructs a Java {@link Computation} subclass, which
 * cannot be expressed from TS).
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.reactive", name = "Reactive")
final class NativeReactive {

    /**
     * Callback shape for the event-collector listener — takes a single (opaque)
     * event object.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsEventConsumer {
        void accept(Object event);
    }

    /**
     * Return shape for {@link #addEventCollector(JsEventConsumer)}: a JS
     * function that removes the registration when invoked.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsRemover {
        void remove();
    }

    private NativeReactive() {
        // Native, not instantiated from Java
    }

    static native void addFlushListener(JsRunnable listener);

    static native void addPostFlushListener(JsRunnable listener);

    static native void flush();

    static native Object getCurrentComputation();

    static native void runWithComputation(Object computation,
            JsRunnable command);

    static native JsRemover addEventCollector(JsEventConsumer listener);

    static native void notifyEventCollectors(Object event);

    static native void reset();
}
