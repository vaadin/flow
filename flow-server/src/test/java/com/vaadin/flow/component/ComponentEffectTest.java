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
package com.vaadin.flow.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.experimental.DisabledFeatureException;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.NumberSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalEnvironment;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

public class ComponentEffectTest {
    private static final Executor executor = Runnable::run;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void effect_componentAttachedAndDetached_effectEnabledAndDisabled() {
        runWithSignalEnvironmentMocks(() -> {
            TestComponent component = new TestComponent();
            ValueSignal<String> signal = new ValueSignal<>("initial");
            AtomicInteger count = new AtomicInteger();
            Registration registration = ComponentEffect.effect(component,
                    () -> {
                        signal.value();
                        count.incrementAndGet();
                    });

            assertEquals("Effect should not be run until component is attached",
                    0, count.get());

            signal.value("test");
            assertEquals(
                    "Effect should not be run until component is attached even after signal value change",
                    0, count.get());

            MockUI ui = new MockUI();
            ui.add(component);

            assertEquals("Effect should be run once component is attached", 1,
                    count.get());

            signal.value("test2");
            assertEquals("Effect should be run when signal value is chaged", 2,
                    count.get());

            ui.remove(component);

            signal.value("test3");
            assertEquals("Effect should not be run after detach", 2,
                    count.get());

            ui.add(component);
            assertEquals("Effect should be run after attach", 3, count.get());

            registration.remove();
            signal.value("test4");
            assertEquals("Effect should not be run after remove", 3,
                    count.get());
        });
    }

    @Test
    public void bind_signalValueChanges_componentUpdated() {
        runWithSignalEnvironmentMocks(() -> {
            TestComponent component = new TestComponent();
            ValueSignal<String> signal = new ValueSignal<>("initial");

            MockUI ui = new MockUI();
            ui.add(component);

            Registration registration = ComponentEffect.bind(component, signal,
                    TestComponent::setValue);

            assertEquals("Initial value should be set", "initial",
                    component.getValue());

            // Change signal value
            signal.value("new value");

            assertEquals("Component should be updated with new value",
                    "new value", component.getValue());

            // Change signal value again
            signal.value("another value");

            assertEquals("Component should be updated with another value",
                    "another value", component.getValue());

            registration.remove();

            // Change signal value after registration is removed
            signal.value("final value");

            assertEquals(
                    "Component should not be updated after registration is removed",
                    "another value", component.getValue());
        });
    }

    @Test
    public void format_customLocale_signalValuesChange_formattedStringUpdated() {
        runWithSignalEnvironmentMocks(() -> {
            TestComponent component = new TestComponent();

            MockUI ui = new MockUI();
            ui.add(component);

            ValueSignal<String> stringSignal = new ValueSignal<>("test");
            NumberSignal numberSignal = new NumberSignal(42.23456);

            Registration registration = ComponentEffect.format(component,
                    TestComponent::setValue, Locale.ENGLISH,
                    "The price of %s is %.2f", stringSignal, numberSignal);

            assertEquals("Initial formatted value should be set",
                    "The price of test is 42.23", component.getValue());

            // Change int signal value
            numberSignal.value(20.12345);

            assertEquals(
                    "Formatted value should be updated with new numeric value",
                    "The price of test is 20.12", component.getValue());

            // Change string signal value
            stringSignal.value("updated");

            assertEquals(
                    "Formatted value should be updated with new string value",
                    "The price of updated is 20.12", component.getValue());

            registration.remove();

            numberSignal.value(30.3456);
            stringSignal.value("final");

            assertEquals(
                    "Formatted value should not be updated after registration is removed",
                    "The price of updated is 20.12", component.getValue());
        });
    }

    @Test
    public void format_defaultLocale_signalValuesChange_formattedStringUpdated() {
        runWithSignalEnvironmentMocks(() -> {
            TestComponent component = new TestComponent();

            MockUI ui = new MockUI();
            ui.add(component);

            ValueSignal<String> stringSignal = new ValueSignal<>("test");
            ValueSignal<Integer> numberSignal = new ValueSignal<>(42);

            ComponentEffect.format(component, TestComponent::setValue,
                    "The price of %s is %d", stringSignal, numberSignal);

            assertEquals("Initial formatted value should be set",
                    "The price of test is 42", component.getValue());
        });
    }

    /**
     * Other tests may already have initialized the environment with the feature
     * flag off and executors that would throw an exception, so it's too late
     * now to mock the feature flags. Thus we need to "reinitialize" the
     * environment.
     */
    private static void runWithSignalEnvironmentMocks(Runnable test) {
        try (var environment = mockStatic(SignalEnvironment.class);
                var featureFlagStaticMock = mockStatic(FeatureFlags.class)) {
            FeatureFlags flags = mock(FeatureFlags.class);
            when(flags.isEnabled(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()))
                    .thenReturn(true);
            featureFlagStaticMock.when(() -> FeatureFlags.get(any()))
                    .thenReturn(flags);
            environment.when(() -> SignalEnvironment.initialized())
                    .thenReturn(true);
            environment.when(() -> SignalEnvironment.defaultDispatcher())
                    .thenReturn(executor);
            environment.when(() -> SignalEnvironment.synchronousDispatcher())
                    .thenReturn(executor);
            environment.when(() -> SignalEnvironment.asynchronousDispatcher())
                    .thenReturn(executor);
            environment.when(() -> SignalEnvironment.objectMapper())
                    .thenReturn(objectMapper);
            test.run();
        }
    }

    @Tag("div")
    private static class TestComponent extends Component {
        String value;

        public TestComponent() {
            super(new Element("div"));
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
