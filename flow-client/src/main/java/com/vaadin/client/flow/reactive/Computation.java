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

import jsinterop.annotations.JsType;

import com.vaadin.client.JsRunnable;

/**
 * Automatically reruns the constructor-supplied recompute callback whenever any
 * reactive value used by it changes. The recompute method is invoked by the
 * next invocation of {@link Reactive#flush()}, unless it has been invoked
 * manually before the global flush. A computation is also scheduled for an
 * initial "recomputation" when it is created.
 *
 * <p>
 * Pure {@code @JsType(isNative=true)} binding to the TypeScript implementation
 * at {@code src/main/frontend/internal/client/flow/reactive/Computation.ts}.
 * The Java subclass pattern (override {@code doRecompute()}) is replaced by
 * passing a {@link JsRunnable} to the constructor. Dependency registration is
 * orchestrated from the Java side: {@link ReactiveEventRouter#registerRead()}
 * calls {@code addReactiveListener(...)} on the router itself and then passes
 * the resulting remover to {@link #addDependencyRemover(JsRunnable)}, so this
 * binding never has to dispatch back into Java method names by string.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.reactive", name = "Computation")
public class Computation {

    /**
     * Creates a new computation that runs the given callback when any of its
     * registered reactive-value dependencies fire a change event.
     */
    public Computation(JsRunnable recompute) {
        // Defined by the TS class constructor.
    }

    /**
     * Records a dependency-removal callback returned by Java when this
     * computation was registered as a listener on a reactive value.
     */
    public native void addDependencyRemover(JsRunnable removeListener);

    /**
     * Receives reactive change events from any registered dependency.
     * Implements {@link ReactiveValueChangeListener#onValueChange} by-name.
     */
    public native void onValueChange(ReactiveValueChangeEvent event);

    /** Stops this computation, so that it will no longer be recomputed. */
    public native void stop();

    /**
     * Checks whether this computation is invalidated. An invalidated
     * computation will eventually be recomputed (unless it has also been
     * stopped).
     */
    public native boolean isInvalidated();

    /** Recomputes this computation. */
    public native void recompute();

    /**
     * Adds an invalidate listener that will be invoked the next time this
     * computation is invalidated.
     */
    public native void onNextInvalidate(InvalidateListener listener);
}
