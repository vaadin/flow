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
package com.vaadin.flow.server;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalEnvironment;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class VaadinServiceSignalsInitializationTest {

    @BeforeEach
    @AfterEach
    public void clearTestEnvironment() {
        CurrentInstance.clearAll();
    }

    @Test
    public void init_flowSignalEnvironmentInitialized()
            throws InterruptedException, TimeoutException {

        var service = new MockVaadinServletService();
        VaadinService.setCurrent(service);

        var phaser = new Phaser(1);
        AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                service);
        var ui = new MockUI(session);
        assertSame(ui, UI.getCurrent());
        var signal = new SharedListSignal<>(String.class);

        record EffectExecution(UI ui, String threadName) {
        }
        var invocations = new ArrayList<EffectExecution>();

        try {
            Signal.unboundEffect(() -> {
                invocations.add(new EffectExecution(UI.getCurrent(),
                        Thread.currentThread().getName()));
                signal.get();
                phaser.arrive();
            });

            phaser.awaitAdvanceInterruptibly(0, 500, TimeUnit.MILLISECONDS);

            assertEquals(1, invocations.size(),
                    "Expected effect to be executed");

            var execution = invocations.get(0);
            assertEquals(null, execution.ui,
                    "Expected UI to not be available during effect execution");
            assertTrue(
                    execution.threadName
                            .startsWith("VaadinTaskExecutor-thread-"),
                    "Expected effect to be executed in Vaadin Executor thread");
        } finally {
            session.unlock();
            UI.setCurrent(null);
        }

        signal.insertLast("update");

        phaser.awaitAdvanceInterruptibly(1, 500, TimeUnit.MILLISECONDS);

        assertEquals(2, invocations.size(),
                "Expected effect to be executed twice");

        var execution = invocations.get(1);
        assertEquals(null, execution.ui,
                "Expected UI to not be available during effect execution");
        assertTrue(
                execution.threadName.startsWith("VaadinTaskExecutor-thread-"),
                "Expected effect to be executed in Vaadin Executor thread");
    }

    @Test
    public void resultNotifier_ownerUiIsClosing_taskNotScheduled()
            throws InterruptedException {
        var service = new MockVaadinServletService();

        VaadinService.setCurrent(service);

        AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                service);
        var ui = new MockUI(session);
        assertSame(ui, UI.getCurrent());

        // Close the UI so that isClosing() returns true
        ui.close();
        assertTrue(ui.isClosing(), "UI should be closing after close()");

        // Obtain the result-notifier dispatcher while the closing UI is current
        var dispatcher = SignalEnvironment.getCurrentResultNotifier();

        AtomicBoolean effectExecuted = new AtomicBoolean(false);

        UI.setCurrent(null);
        session.unlock();

        CountDownLatch latch = new CountDownLatch(1);

        dispatcher.execute(() -> {
            effectExecuted.set(true);
            latch.countDown();
        });

        latch.await(100, TimeUnit.MILLISECONDS);

        assertFalse(effectExecuted.get(), "Expected task to not execute");
    }

}
