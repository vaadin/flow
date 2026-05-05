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
 * tests, a native bridge in a hybrid mobile/desktop shell.
 * <p>
 * Replacement clients are installed at facade construction time by registering
 * a {@link GeolocationClientFactory} via Java's service loader (a
 * {@code META-INF/services/com.vaadin.flow.component.geolocation.GeolocationClientFactory}
 * file). Vaadin's {@link com.vaadin.flow.di.Lookup Lookup} resolves the factory
 * and {@link Geolocation} hands the resulting client every {@link #get},
 * {@link #startWatch} and {@link #subscribeAvailability} call. When no factory
 * is registered, {@code Geolocation} uses the built-in browser-backed client.
 * <p>
 * <b>Threading:</b> all callbacks on this interface (the future returned by
 * {@link #get}, the {@code onUpdate} consumer passed to {@link #startWatch},
 * and the {@code onChange} consumer passed to {@link #subscribeAvailability})
 * must be invoked on the UI thread.
 */
@NullMarked
public interface GeolocationClient extends Serializable {

    /**
     * Issues a one-shot position request. The future completes once the client
     * has an answer (a position or an error).
     *
     * @param options
     *            tuning options, or {@code null} for browser defaults
     * @return a future that completes with the outcome on the UI thread
     */
    CompletableFuture<GeolocationOutcome> get(
            @Nullable GeolocationOptions options);

    /**
     * Starts a watch session bound to {@code owner}. Position and error pushes
     * are delivered via {@code onUpdate}. The returned handle is used to stop
     * the watch and to query whether it is still active.
     *
     * @param owner
     *            the component that owns this watch; detaching the component
     *            does not auto-stop the watch — the caller is responsible
     * @param options
     *            tuning options, or {@code null} for browser defaults
     * @param onUpdate
     *            consumer invoked on the UI thread for every push
     * @return a handle for stopping the watch
     */
    WatchHandle startWatch(Component owner,
            @Nullable GeolocationOptions options,
            SerializableConsumer<GeolocationResult> onUpdate);

    /**
     * Subscribes to availability changes. The returned registration removes the
     * subscription.
     *
     * @param onChange
     *            consumer invoked on the UI thread for every availability
     *            change
     * @return a registration that removes the subscription when called
     */
    Registration subscribeAvailability(
            SerializableConsumer<GeolocationAvailability> onChange);

    /**
     * Returns the most recently observed availability. Implementations must
     * seed an initial value at construction; the result is never null.
     *
     * @return the current availability
     */
    GeolocationAvailability currentAvailability();

    /**
     * Releases any resources held by this client. Called on UI detach.
     * Idempotent: calling more than once is a no-op. After {@code close()}, the
     * behavior of {@link #get} and {@link #startWatch} is undefined and the
     * facade must not call them.
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
         *
         * @return {@code true} if the watch is still receiving updates
         */
        boolean isActive();
    }
}
