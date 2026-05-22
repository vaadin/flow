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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * {@link GeolocationClient} implementation backed by the real browser
 * Geolocation API via {@code window.Vaadin.Flow.geolocation} and DOM events.
 * This is the default implementation used when no
 * {@link GeolocationClientFactory} is registered through
 * {@link com.vaadin.flow.di.Lookup Lookup}.
 * <p>
 * <b>Framework internal.</b> Application code does not reference this class
 * directly.
 */
@NullMarked
final class BrowserGeolocationClient implements GeolocationClient {

    private record GetResult(@Nullable GeolocationPosition position,
            @Nullable GeolocationError error,
            @Nullable String availability) implements Serializable {
    }

    private record AvailabilityDetail(
            @Nullable String availability) implements Serializable {
    }

    private final UI ui;
    private final List<SerializableConsumer<GeolocationAvailability>> availabilityListeners = new ArrayList<>();
    private final DomListenerRegistration availabilityChangeRegistration;
    private GeolocationAvailability currentAvailability;
    private boolean closed;

    BrowserGeolocationClient(UI ui, GeolocationAvailability seed) {
        this.ui = ui;
        this.currentAvailability = seed;
        availabilityChangeRegistration = ui.getElement()
                .addEventListener("vaadin-geolocation-availability-change",
                        e -> updateAvailability(
                                e.getEventDetail(AvailabilityDetail.class)
                                        .availability()))
                .addEventDetail().allowInert();
    }

    @Override
    public CompletableFuture<GeolocationOutcome> get(
            GeolocationOptions options) {
        CompletableFuture<GeolocationOutcome> future = new CompletableFuture<>();
        ui.getElement()
                .executeJs("return window.Vaadin.Flow.geolocation.get($0)",
                        options)
                .then(GetResult.class, result -> {
                    updateAvailability(result.availability());
                    if (result.position() != null) {
                        future.complete(result.position());
                    } else if (result.error() != null) {
                        future.complete(result.error());
                    } else {
                        future.completeExceptionally(new IllegalStateException(
                                "Geolocation get() returned neither position nor error"));
                    }
                }, err -> future.completeExceptionally(new RuntimeException(
                        "Client-side geolocation.get failed: " + err)));
        return future;
    }

    @Override
    public WatchHandle startWatch(Component owner, GeolocationOptions options,
            SerializableConsumer<GeolocationResult> onUpdate) {
        return new BrowserWatchHandle(owner, options, onUpdate);
    }

    @Override
    public Registration subscribeAvailability(
            SerializableConsumer<GeolocationAvailability> onChange) {
        availabilityListeners.add(onChange);
        return () -> availabilityListeners.remove(onChange);
    }

    @Override
    public GeolocationAvailability currentAvailability() {
        return currentAvailability;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        availabilityChangeRegistration.remove();
        availabilityListeners.clear();
    }

    private void updateAvailability(@Nullable String value) {
        if (value == null) {
            return;
        }
        GeolocationAvailability next;
        try {
            next = GeolocationAvailability.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        if (next == currentAvailability) {
            return;
        }
        currentAvailability = next;
        for (SerializableConsumer<GeolocationAvailability> listener : new ArrayList<>(
                availabilityListeners)) {
            listener.accept(next);
        }
    }

    private final class BrowserWatchHandle implements WatchHandle {

        private @Nullable DomListenerRegistration positionListener;
        private @Nullable DomListenerRegistration errorListener;
        private @Nullable Registration watchRegistration;
        private boolean active = true;

        BrowserWatchHandle(Component owner, GeolocationOptions options,
                SerializableConsumer<GeolocationResult> onUpdate) {
            Element el = owner.getElement();
            positionListener = el
                    .addEventListener("vaadin-geolocation-position",
                            e -> onUpdate.accept(e
                                    .getEventDetail(GeolocationPosition.class)))
                    .addEventDetail().allowInert();
            errorListener = el
                    .addEventListener("vaadin-geolocation-error",
                            e -> onUpdate.accept(
                                    e.getEventDetail(GeolocationError.class)))
                    .addEventDetail().allowInert();
            // The watch is driven by addJsInitializer: it starts on every
            // fresh client-side DOM and the returned cleanup function tears
            // the watch down when the DOM is discarded or stop() removes
            // the registration. A bridge-load failure is surfaced two ways:
            // we dispatch the same error event the position listener already
            // observes (so onUpdate sees a GeolocationError), and we
            // re-throw the original error so the framework's executeJs
            // logging records the underlying cause. A clearWatch failure is
            // captured by the framework's cleanup-callback error logging
            // (ExecuteJavaScriptElementUtils.invokeSafely).
            String watchKey = UUID.randomUUID().toString();
            int unknownErrorCode = GeolocationErrorCode.UNKNOWN.code();
            watchRegistration = el.addJsInitializer(
                    """
                            try {
                              window.Vaadin.Flow.geolocation.watch(this, $0, $1);
                            } catch (err) {
                              this.dispatchEvent(new CustomEvent('vaadin-geolocation-error', {
                                detail: { code: $2, message: 'Client-side geolocation bridge failure' }
                              }));
                              throw err;
                            }
                            const __wk = $1;
                            return () => window.Vaadin.Flow.geolocation.clearWatch(__wk);
                            """,
                    options, watchKey, unknownErrorCode);
        }

        @Override
        public void stop() {
            if (!active) {
                return;
            }
            active = false;
            if (positionListener != null) {
                positionListener.remove();
                positionListener = null;
            }
            if (errorListener != null) {
                errorListener.remove();
                errorListener = null;
            }
            if (watchRegistration != null) {
                watchRegistration.remove();
                watchRegistration = null;
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
