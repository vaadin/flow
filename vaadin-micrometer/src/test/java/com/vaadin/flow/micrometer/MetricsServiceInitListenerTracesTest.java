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
package com.vaadin.flow.micrometer;

import java.util.Optional;
import java.util.concurrent.Executor;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.micrometer.trace.TracingExecutor;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;

public class MetricsServiceInitListenerTracesTest {

    @After
    public void tearDown() {
        VaadinMicrometer.uninstall();
    }

    @Test
    public void executorIsWrappedWhenTracesEnabled() {
        ObservationRegistry obs = ObservationRegistry.create();
        MetricsServiceInitListener listener = new MetricsServiceInitListener(
                new SimpleMeterRegistry(), obs, VaadinMetricsConfig.defaults());

        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = new ServiceInitEvent(service);
        Executor original = Runnable::run;
        event.setExecutor(original);

        listener.serviceInit(event);

        Optional<Executor> after = event.getExecutor();
        Assert.assertTrue(after.isPresent());
        Assert.assertTrue("executor should be wrapped in TracingExecutor",
                after.get() instanceof TracingExecutor);
    }

    @Test
    public void executorIsNotWrappedWhenTracesDisabled() {
        ObservationRegistry obs = ObservationRegistry.create();
        MetricsServiceInitListener listener = new MetricsServiceInitListener(
                new SimpleMeterRegistry(), obs,
                VaadinMetricsConfig.builder().traces(false).build());

        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = new ServiceInitEvent(service);
        Executor original = Runnable::run;
        event.setExecutor(original);

        listener.serviceInit(event);

        Assert.assertSame("executor should remain unwrapped", original,
                event.getExecutor().orElse(null));
    }

    @Test
    public void executorIsNotWrappedWhenObservationRegistryAbsent() {
        MetricsServiceInitListener listener = new MetricsServiceInitListener(
                new SimpleMeterRegistry(), null,
                VaadinMetricsConfig.defaults());

        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = new ServiceInitEvent(service);
        Executor original = Runnable::run;
        event.setExecutor(original);

        listener.serviceInit(event);

        Assert.assertSame(original, event.getExecutor().orElse(null));
    }

    @Test
    public void noOpInstallHookCanBeOverriddenBySubclass() {
        final boolean[] called = { false };
        ObservationRegistry obs = ObservationRegistry.create();
        new MetricsServiceInitListener(new SimpleMeterRegistry(), obs,
                VaadinMetricsConfig.defaults()) {
            @Override
            protected void installDefaultObservationHandlers(
                    ObservationRegistry r,
                    io.micrometer.core.instrument.MeterRegistry mr) {
                called[0] = true;
            }
        };
        Assert.assertTrue(
                "subclass override should be dispatched from base ctor",
                called[0]);
    }
}
