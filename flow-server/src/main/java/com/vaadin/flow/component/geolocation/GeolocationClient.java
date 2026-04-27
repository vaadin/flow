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
package com.vaadin.flow.component.geolocation;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * Port between the {@link Geolocation} facade and whatever delivers actual
 * position data — the browser in production, an in-memory test driver in unit
 * tests.
 * <p>
 * <b>Threading:</b> all callbacks on this interface (the future returned by
 * {@link #get}, the {@code onUpdate} consumer passed to {@link #startWatch},
 * and the {@code onChange} consumer passed to {@link #subscribeAvailability})
 * must be invoked on the UI thread.
 * <p>
 * <b>Framework internal.</b> Application code never references this interface
 * directly; external browserless test drivers install a replacement client via
 * {@link Geolocation#setClient(GeolocationClient)}.
 */
@NullMarked
public interface GeolocationClient extends Serializable {

    /**
     * Issues a one-shot position request. The future completes once the client
     * has an answer (a position or an error).
     */
    CompletableFuture<GeolocationOutcome> get(
            @Nullable GeolocationOptions options);

    /**
     * Starts a watch session bound to {@code owner}. Position and error pushes
     * are delivered via {@code onUpdate}. The returned handle is used to stop
     * the watch and to query whether it is still active.
     */
    WatchHandle startWatch(Component owner,
            @Nullable GeolocationOptions options,
            SerializableConsumer<GeolocationResult> onUpdate);

    /**
     * Subscribes to availability changes. The returned registration removes the
     * subscription.
     */
    Registration subscribeAvailability(
            SerializableConsumer<GeolocationAvailability> onChange);

    /**
     * Returns the most recently observed availability. Implementations must
     * seed an initial value at construction; the result is never null.
     */
    GeolocationAvailability currentAvailability();

    /**
     * Releases any resources held by this client. Called when the facade is
     * replacing one client with another (e.g. when the test controller is
     * installed) and on UI detach. Idempotent: calling more than once is a
     * no-op. After {@code close()}, the behavior of {@link #get} and
     * {@link #startWatch} is undefined and the facade must not call them.
     */
    void close();

    /**
     * Handle to a tracker watch session. The handle is alive while the
     * underlying watch is active; calling {@link #stop()} idempotently tears it
     * down.
     */
    interface WatchHandle extends Serializable {
        /**
         * Stops the underlying watch. Idempotent: calling more than once is a
         * no-op.
         */
        void stop();

        /**
         * Returns whether the watch is currently active (has not yet been
         * stopped or auto-cancelled).
         */
        boolean isActive();
    }
}
