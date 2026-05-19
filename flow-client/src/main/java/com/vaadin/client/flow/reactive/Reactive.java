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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;

import com.vaadin.client.Command;

import elemental.events.EventRemover;

/**
 * Handles global features related to reactivity, such as keeping track of the
 * current {@link Computation}, providing a lazy flush cycle and registering
 * reactive event collectors.
 *
 * <p>
 * Under GWT the state-management surface (flush listeners, post-flush
 * listeners, current computation, event collectors) lives in the TypeScript
 * module at
 * {@code src/main/frontend/internal/client/flow/reactive/Reactive.ts}, reached
 * through {@link NativeReactive}. The Java-side helper
 * {@link #runWhenDependenciesChange(Command)} stays here because it constructs
 * a Java {@link Computation} subclass, which cannot be expressed from TS.
 *
 * <p>
 * On the JVM a parallel implementation backed by {@link ArrayList} /
 * {@link LinkedHashSet} keeps existing JUnit tests passing through the
 * migration. This is the "transitional" pattern documented in
 * {@code MIGRATION.md}; it is justified here because Reactive is at the bottom
 * of the dependency graph and so many JUnit tests reach it transitively. Both
 * halves go away in the tear-down phase together with the JUnit suite.
 *
 * @see Computation
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Reactive {

    // JVM-side state, used only when GWT.isScript() is false.
    private static List<FlushListener> jvmFlushListeners = new ArrayList<>();
    private static List<FlushListener> jvmPostFlushListeners = new ArrayList<>();
    private static Set<ReactiveValueChangeListener> jvmEventCollectors = new LinkedHashSet<>();
    private static Computation jvmCurrentComputation = null;
    private static boolean jvmFlushing = false;

    private Reactive() {
        // Only static stuff in this class
    }

    /**
     * Adds a listener that will be invoked the next time {@link #flush()} is
     * invoked.
     */
    public static void addFlushListener(FlushListener flushListener) {
        if (GWT.isScript()) {
            NativeReactive.addFlushListener(flushListener::flush);
        } else {
            jvmFlushListeners.add(flushListener);
        }
    }

    /**
     * Adds a listener that will be invoked during the next {@link #flush()},
     * after all regular flush listeners have been invoked.
     */
    public static void addPostFlushListener(FlushListener postFlushListener) {
        if (GWT.isScript()) {
            NativeReactive.addPostFlushListener(postFlushListener::flush);
        } else {
            jvmPostFlushListeners.add(postFlushListener);
        }
    }

    /**
     * Flushes all flush listeners and post flush listeners.
     */
    public static void flush() {
        if (GWT.isScript()) {
            NativeReactive.flush();
            return;
        }
        if (jvmFlushing) {
            return;
        }
        int flushIndex = 0;
        int postFlushIndex = 0;
        jvmFlushing = true;
        try {
            while (flushIndex < jvmFlushListeners.size()
                    || postFlushIndex < jvmPostFlushListeners.size()) {
                while (flushIndex < jvmFlushListeners.size()) {
                    jvmFlushListeners.get(flushIndex).flush();
                    flushIndex++;
                }
                if (postFlushIndex < jvmPostFlushListeners.size()) {
                    jvmPostFlushListeners.get(postFlushIndex).flush();
                    postFlushIndex++;
                }
            }
        } finally {
            jvmFlushing = false;
            jvmFlushListeners.subList(0, flushIndex).clear();
            jvmPostFlushListeners.subList(0, postFlushIndex).clear();
        }
    }

    /**
     * Gets the currently active computation.
     */
    public static Computation getCurrentComputation() {
        if (GWT.isScript()) {
            return (Computation) NativeReactive.getCurrentComputation();
        }
        return jvmCurrentComputation;
    }

    /**
     * Runs a task with the given computation set as
     * {@link #getCurrentComputation()}.
     */
    public static void runWithComputation(Computation computation,
            Command command) {
        if (GWT.isScript()) {
            NativeReactive.runWithComputation(computation, command::execute);
            return;
        }
        Computation old = jvmCurrentComputation;
        jvmCurrentComputation = computation;
        try {
            command.execute();
        } finally {
            jvmCurrentComputation = old;
        }
    }

    /**
     * Adds a reactive change listener that will be invoked whenever a reactive
     * change event is fired from any reactive event router.
     */
    public static EventRemover addEventCollector(
            ReactiveValueChangeListener reactiveValueChangeListener) {
        if (GWT.isScript()) {
            NativeReactive.JsRemover remover = NativeReactive
                    .addEventCollector(event -> reactiveValueChangeListener
                            .onValueChange((ReactiveValueChangeEvent) event));
            return remover::remove;
        }
        jvmEventCollectors.add(reactiveValueChangeListener);
        return () -> jvmEventCollectors.remove(reactiveValueChangeListener);
    }

    /**
     * Fires a reactive change event to all registered event collectors.
     */
    public static void notifyEventCollectors(ReactiveValueChangeEvent event) {
        if (GWT.isScript()) {
            NativeReactive.notifyEventCollectors(event);
            return;
        }
        if (jvmEventCollectors.isEmpty()) {
            return;
        }
        for (ReactiveValueChangeListener listener : new ArrayList<>(
                jvmEventCollectors)) {
            listener.onValueChange(event);
        }
    }

    /**
     * Evaluates the given command whenever there is a change in any
     * {@link ReactiveValue} used in the command.
     */
    public static Computation runWhenDependenciesChange(Command command) {
        return new Computation() {
            @Override
            protected void doRecompute() {
                command.execute();
            }
        };
    }

    /**
     * Resets Reactive to the initial state.
     *
     * <p>
     * Intended for test cases to call in setup to avoid having tests affect
     * each other as Reactive state is static and shared. Should never be called
     * from non-test code.
     */
    public static void reset() {
        if (GWT.isScript()) {
            NativeReactive.reset();
            return;
        }
        jvmFlushListeners.clear();
        jvmPostFlushListeners.clear();
        jvmEventCollectors.clear();
        jvmCurrentComputation = null;
        jvmFlushing = false;
    }
}
