/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.server;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.vaadin.experimental.DisabledFeatureException;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalEnvironment;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@NotThreadSafe
public class VaadinServiceSignalsInitializationTest {

    @Before
    @After
    public void clearTestEnvironment() {
        CurrentInstance.clearAll();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void init_signalsFeatureFlagOff_throwsWhenSignalUsed___staticMocks() {
        try (var featureFlagStaticMock = mockStatic(FeatureFlags.class);
                var signalsEnvStaticMock = mockStatic(
                        SignalEnvironment.class)) {
            FeatureFlags flags = mock(FeatureFlags.class);
            when(flags.isEnabled(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()))
                    .thenReturn(false);
            featureFlagStaticMock.when(() -> FeatureFlags.get(any()))
                    .thenReturn(flags);
            ArgumentCaptor<Executor> executorCaptor = ArgumentCaptor
                    .forClass(Executor.class);
            ArgumentCaptor<Supplier<Executor>> dispatcherCaptor = ArgumentCaptor
                    .forClass(Supplier.class);

            signalsEnvStaticMock.when(() -> SignalEnvironment
                    .tryInitialize(any(), executorCaptor.capture()))
                    .thenReturn(true);
            signalsEnvStaticMock
                    .when(() -> SignalEnvironment
                            .addDispatcherOverride(dispatcherCaptor.capture()))
                    .thenReturn(null);

            new MockVaadinServletService();
            signalsEnvStaticMock.verify(
                    () -> SignalEnvironment.tryInitialize(any(), any()));
            signalsEnvStaticMock.verify(
                    () -> SignalEnvironment.addDispatcherOverride(any()));

            // Expecting Vaadin signals executor to always throw exception
            var executor = executorCaptor.getValue();
            var error = assertThrows(DisabledFeatureException.class,
                    () -> executor.execute(() -> {
                    }));
            Assert.assertTrue(error.getMessage()
                    .contains(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()));

            // Expecting Vaadin dispatcher to always throw exception
            error = assertThrows(DisabledFeatureException.class,
                    () -> dispatcherCaptor.getValue().get());
            Assert.assertTrue(error.getMessage()
                    .contains(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void init_signalsFeatureFlagOn_flowSignalEnvironmentInitialized___staticMocks()
            throws InterruptedException {
        try (var featureFlagStaticMock = mockStatic(FeatureFlags.class);
                var signalsEnvStaticMock = mockStatic(
                        SignalEnvironment.class)) {
            FeatureFlags flags = mock(FeatureFlags.class);
            when(flags.isEnabled(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()))
                    .thenReturn(true);
            featureFlagStaticMock.when(() -> FeatureFlags.get(any()))
                    .thenReturn(flags);
            ArgumentCaptor<Executor> executorCaptor = ArgumentCaptor
                    .forClass(Executor.class);
            ArgumentCaptor<Supplier<Executor>> dispatcherCaptor = ArgumentCaptor
                    .forClass(Supplier.class);

            signalsEnvStaticMock.when(() -> SignalEnvironment
                    .tryInitialize(any(), executorCaptor.capture()))
                    .thenReturn(true);
            signalsEnvStaticMock
                    .when(() -> SignalEnvironment
                            .addDispatcherOverride(dispatcherCaptor.capture()))
                    .thenReturn(null);
            signalsEnvStaticMock.when(SignalEnvironment::defaultDispatcher)
                    .then((i -> executorCaptor.getValue()));

            MockVaadinServletService service = new MockVaadinServletService();
            AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                    service);

            signalsEnvStaticMock.verify(
                    () -> SignalEnvironment.tryInitialize(any(), any()));
            signalsEnvStaticMock.verify(
                    () -> SignalEnvironment.addDispatcherOverride(any()));

            // Expecting Vaadin executor to be used
            Executor executor = executorCaptor.getValue();
            CountDownLatch latch1 = new CountDownLatch(1);
            AtomicReference<String> threadName = new AtomicReference<>();
            executor.execute(() -> {
                threadName.set(Thread.currentThread().getName());
                latch1.countDown();
            });

            if (!latch1.await(500, TimeUnit.MILLISECONDS)) {
                Assert.fail("Expected async task to be executed");
            }
            Assert.assertTrue(
                    "Expected async task to be executed by Vaadin Signals executor",
                    threadName.get().startsWith("VaadinTaskExecutor-thread-"));

            // Vaadin signals dispatcher should execute synchronously if UI is
            // available.
            threadName.set(null);
            UI ui = new MockUI(session);
            AtomicReference<UI> uiRef = new AtomicReference<>();
            try {
                CountDownLatch latch2 = new CountDownLatch(1);
                executor = dispatcherCaptor.getValue().get();
                executor.execute(() -> {
                    uiRef.set(UI.getCurrent());
                    threadName.set(Thread.currentThread().getName());
                    latch2.countDown();
                });
                if (!latch2.await(500, TimeUnit.MILLISECONDS)) {
                    Assert.fail("Expected task to be executed");
                }
                Assert.assertEquals(
                        "Expected UI to be available during sync effect execution",
                        ui, uiRef.get());
                Assert.assertFalse(
                        "Expected effect to be executed in main thread",
                        threadName.get()
                                .startsWith("VaadinTaskExecutor-thread-"));
            } finally {
                session.unlock();
                UI.setCurrent(null);
                uiRef.set(null);
                threadName.set(null);
            }

            // Vaadin signals dispatcher should execute asynchronously if a
            // different
            // UI is not available.
            CountDownLatch latch3 = new CountDownLatch(1);
            executor.execute(() -> {
                uiRef.set(UI.getCurrent());
                threadName.set(Thread.currentThread().getName());
                latch3.countDown();
            });
            if (!latch3.await(500, TimeUnit.MILLISECONDS)) {
                Assert.fail("Expected task to be executed");
            }
            Assert.assertEquals(
                    "Expected UI to be available during sync effect execution",
                    ui, uiRef.get());
            Assert.assertTrue(
                    "Expected effect to be executed in Vaadin executor thread",
                    threadName.get().startsWith("VaadinTaskExecutor-thread-"));
        }
    }

    @Test
    public void init_signalsFeatureFlagOff_throwsWhenSignalUsed() {
        String signalsFeatureFlagKey = FeatureFlags.SYSTEM_PROPERTY_PREFIX_EXPERIMENTAL
                + FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId();
        var signalsFlag = System.getProperty(signalsFeatureFlagKey);
        try {
            System.setProperty(signalsFeatureFlagKey, "false");
            // VaadinService makes sure that Signal environment will fail if the
            // feature flag is not enabled
            new MockVaadinServletService();
            var error = assertThrows(DisabledFeatureException.class,
                    () -> Signal.effect(() -> {
                    }));
            Assert.assertTrue(error.getMessage()
                    .contains(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()));
        } finally {
            if (signalsFlag != null) {
                System.setProperty(signalsFeatureFlagKey, signalsFlag);
            } else {
                System.clearProperty(signalsFeatureFlagKey);
            }
        }
    }

    @Test
    public void init_signalsFeatureFlagOn_flowSignalEnvironmentInitialized()
            throws InterruptedException {

        String signalsFeatureFlagKey = FeatureFlags.SYSTEM_PROPERTY_PREFIX_EXPERIMENTAL
                + FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId();
        var signalsFlag = System.getProperty(signalsFeatureFlagKey);
        try {
            System.setProperty(signalsFeatureFlagKey, "true");
            var service = new MockVaadinServletService();

            var latch = new CountDownLatch(2);
            AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                    service);
            var ui = new MockUI(session);
            var signal = new ListSignal<>(String.class);

            record EffectExecution(UI ui, String threadName) {
            }
            var invocations = new ArrayList<EffectExecution>();

            try {
                Signal.effect(() -> {
                    // Should run in Flow defined dispatcher, so UI should be
                    // available
                    invocations.add(new EffectExecution(UI.getCurrent(),
                            Thread.currentThread().getName()));
                    signal.value();
                    latch.countDown();
                });
                Assert.assertEquals("Expected effect to be executed", 1,
                        invocations.size());
                // First execution in main thread
                var execution = invocations.get(0);
                Assert.assertEquals(
                        "Expected UI to be available during sync effect execution",
                        ui, execution.ui);
                Assert.assertFalse(
                        "Expected effect to be executed in main thread",
                        execution.threadName
                                .startsWith("VaadinTaskExecutor-thread-"));
            } finally {
                session.unlock();
                UI.setCurrent(null);
            }

            signal.insertLast("update");

            if (!latch.await(500, TimeUnit.MILLISECONDS)) {
                Assert.fail(
                        "Expected signal effect to be computed asynchronously");
            }

            Assert.assertEquals("Expected effect to be executed twice", 2,
                    invocations.size());
            // Second execution in Vaadin executor thread
            var execution = invocations.get(1);
            Assert.assertEquals(
                    "Expected UI to be available during async effect execution",
                    ui, execution.ui);
            Assert.assertTrue(
                    "Expected effect to be executed in Vaadin Executor thread",
                    execution.threadName
                            .startsWith("VaadinTaskExecutor-thread-"));
        } finally {
            if (signalsFlag != null) {
                System.setProperty(signalsFeatureFlagKey, signalsFlag);
            } else {
                System.clearProperty(signalsFeatureFlagKey);
            }
        }
    }

}
