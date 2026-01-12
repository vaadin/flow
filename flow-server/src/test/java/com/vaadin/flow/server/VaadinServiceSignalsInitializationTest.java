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
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.experimental.DisabledFeatureException;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.Signal;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

@NotThreadSafe
public class VaadinServiceSignalsInitializationTest {

    @Before
    @After
    public void clearTestEnvironment() {
        CurrentInstance.clearAll();
    }

    @Test
    public void init_signalsFeatureFlagOff_throwsWhenSignalUsedWithCurrentService() {
        String signalsFeatureFlagKey = FeatureFlags.SYSTEM_PROPERTY_PREFIX_EXPERIMENTAL
                + FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId();
        var signalsFlag = System.getProperty(signalsFeatureFlagKey);
        try {
            System.setProperty(signalsFeatureFlagKey, "false");
            // VaadinService makes sure that Signal environment will fail if the
            // feature flag is not enabled
            MockVaadinServletService service = new MockVaadinServletService();

            // Verify that feature flag has no impact when used outside Vaadin
            Signal.effect(() -> {
            });

            VaadinService.setCurrent(service);

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
            throws InterruptedException, TimeoutException {

        String signalsFeatureFlagKey = FeatureFlags.SYSTEM_PROPERTY_PREFIX_EXPERIMENTAL
                + FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId();
        var signalsFlag = System.getProperty(signalsFeatureFlagKey);
        try {
            System.setProperty(signalsFeatureFlagKey, "true");
            var service = new MockVaadinServletService();
            VaadinService.setCurrent(service);

            var phaser = new Phaser(1);
            AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                    service);
            var ui = new MockUI(session);
            assertSame(ui, UI.getCurrent());
            var signal = new ListSignal<>(String.class);

            record EffectExecution(UI ui, String threadName) {
            }
            var invocations = new ArrayList<EffectExecution>();

            try {
                Signal.effect(() -> {
                    invocations.add(new EffectExecution(UI.getCurrent(),
                            Thread.currentThread().getName()));
                    signal.value();
                    phaser.arrive();
                });

                phaser.awaitAdvanceInterruptibly(0, 500, TimeUnit.MILLISECONDS);

                Assert.assertEquals("Expected effect to be executed", 1,
                        invocations.size());

                var execution = invocations.get(0);
                Assert.assertEquals(
                        "Expected UI to not be available during effect execution",
                        null, execution.ui);
                Assert.assertTrue(
                        "Expected effect to be executed in Vaadin Executor thread",
                        execution.threadName
                                .startsWith("VaadinTaskExecutor-thread-"));
            } finally {
                session.unlock();
                UI.setCurrent(null);
            }

            signal.insertLast("update");

            phaser.awaitAdvanceInterruptibly(1, 500, TimeUnit.MILLISECONDS);

            Assert.assertEquals("Expected effect to be executed twice", 2,
                    invocations.size());

            var execution = invocations.get(1);
            Assert.assertEquals(
                    "Expected UI to not be available during effect execution",
                    null, execution.ui);
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
