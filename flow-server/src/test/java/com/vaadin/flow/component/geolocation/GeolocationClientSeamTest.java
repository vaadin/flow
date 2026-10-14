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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link GeolocationClient} seam — both the public
 * {@link GeolocationClientFactory} extension point that external test drivers
 * and native bridges register through {@link Lookup}, and the
 * {@link com.vaadin.flow.component.internal.UIInternals#setGeolocationClient(GeolocationClient)}
 * direct-install seam used by flow-server's own tests and by the
 * {@link GeolocationWatcher#handle()} accessor.
 */
class GeolocationClientSeamTest {

    private MockUI ui;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
    }

    @Tag("div")
    private static class TestComponent extends Component {
    }

    @Test
    void lookupFactory_resolvedOnFirstUse_clientReceivesGetCalls() {
        FakeClient fake = new FakeClient();
        VaadinService service = VaadinService.getCurrent();
        Lookup lookup = service.getContext().getAttribute(Lookup.class);
        Mockito.when(lookup.lookup(GeolocationClientFactory.class))
                .thenReturn(unused -> fake);

        MockUI freshUi = new MockUI();
        Geolocation.getPosition(pos -> {
        }, err -> {
        }, freshUi);

        assertEquals(1, fake.getCalls.size(),
                "factory-produced client should receive getPosition() calls");
    }

    @Test
    void setGeolocationClient_routesGetThroughInstalledClient() {
        FakeClient fake = new FakeClient();
        ui.getInternals().setGeolocationClient(fake);

        Geolocation.getPosition(pos -> {
        }, err -> {
        }, ui);

        assertEquals(1, fake.getCalls.size(),
                "getPosition() should route through the installed client");
    }

    @Test
    void setGeolocationClient_closesPreviousClient() {
        FakeClient first = new FakeClient();
        FakeClient second = new FakeClient();
        ui.getInternals().setGeolocationClient(first);
        ui.getInternals().setGeolocationClient(second);

        assertTrue(first.closed,
                "previous client should be closed when setGeolocationClient replaces it");
    }

    @Test
    void watchPosition_handleComesFromCurrentClient() {
        FakeClient fake = new FakeClient();
        ui.getInternals().setGeolocationClient(fake);

        TestComponent owner = new TestComponent();
        ui.add(owner);
        GeolocationWatcher watcher = Geolocation.watchPosition(owner);

        GeolocationClient.WatchHandle handle = watcher.handle();
        assertNotNull(handle, "watcher should expose its watch handle");
        assertSame(fake.lastWatchHandle, handle,
                "handle should be the one returned by client.startWatch");
    }

    @Test
    void watchPosition_handleIsNullAfterStop() {
        FakeClient fake = new FakeClient();
        ui.getInternals().setGeolocationClient(fake);

        TestComponent owner = new TestComponent();
        ui.add(owner);
        GeolocationWatcher watcher = Geolocation.watchPosition(owner);
        watcher.stop();

        assertNull(watcher.handle(),
                "handle() should return null after stop()");
    }

    @Test
    void getPosition_onErrorReceivesUnknownErrorWhenClientFutureFailsExceptionally() {
        FakeClient fake = new FakeClient();
        fake.nextGetResult = CompletableFuture
                .failedFuture(new RuntimeException(
                        "Client-side geolocation.get failed: boom"));
        ui.getInternals().setGeolocationClient(fake);

        AtomicReference<@Nullable GeolocationPosition> position = new AtomicReference<>();
        AtomicReference<@Nullable GeolocationError> received = new AtomicReference<>();
        Geolocation.getPosition(position::set, received::set, ui);

        GeolocationError err = received.get();
        assertNotNull(err, "onError must fire even when the JS bridge fails");
        assertNull(position.get(),
                "onSuccess must stay silent when the bridge fails");
        assertEquals(GeolocationErrorCode.UNKNOWN, err.errorCode(),
                "error code should be UNKNOWN for client-bridge failures");
        assertFalse(err.debugInfo().contains("boom"),
                "synthesized message must not leak the wrapped exception text;"
                        + " the cause is logged at DEBUG instead");
    }

    /**
     * Minimal in-test fake. Records get() calls and serves a sentinel
     * WatchHandle from startWatch.
     */
    private static class FakeClient implements GeolocationClient {
        final List<GeolocationOptions> getCalls = new ArrayList<>();
        boolean closed;
        GeolocationClient.@Nullable WatchHandle lastWatchHandle;
        @Nullable
        CompletableFuture<GeolocationOutcome> nextGetResult;

        @Override
        public CompletableFuture<GeolocationOutcome> get(
                GeolocationOptions options) {
            getCalls.add(options);
            CompletableFuture<GeolocationOutcome> result = nextGetResult;
            return result != null ? result : new CompletableFuture<>();
        }

        @Override
        public WatchHandle startWatch(Component owner,
                GeolocationOptions options,
                SerializableConsumer<GeolocationResult> onUpdate) {
            lastWatchHandle = new FakeWatchHandle();
            return lastWatchHandle;
        }

        @Override
        public Registration subscribeAvailability(
                SerializableConsumer<GeolocationAvailability> onChange) {
            return () -> {
            };
        }

        @Override
        public GeolocationAvailability currentAvailability() {
            return GeolocationAvailability.UNKNOWN;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class FakeWatchHandle
            implements GeolocationClient.WatchHandle {
        boolean active = true;

        @Override
        public void stop() {
            active = false;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
