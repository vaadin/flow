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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.SessionLockEvent;
import com.vaadin.flow.server.VaadinService;

public class SessionLockMetricsBinderTest {

    private static final class RecordingHandler
            implements ObservationHandler<Observation.Context> {

        final List<String> names = new ArrayList<>();
        final List<String> contextualNames = new ArrayList<>();
        final List<Map<String, String>> tags = new ArrayList<>();

        @Override
        public void onStop(Observation.Context ctx) {
            names.add(ctx.getName());
            contextualNames.add(ctx.getContextualName());
            Map<String, String> snap = new HashMap<>();
            for (KeyValue kv : ctx.getLowCardinalityKeyValues()) {
                snap.put(kv.getKey(), kv.getValue());
            }
            tags.add(snap);
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }

    @Test
    public void acquireRecordsWaitTimer() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SessionLockMetricsBinder binder = new SessionLockMetricsBinder(
                registry);
        SessionLockEvent event = new SessionLockEvent(
                Mockito.mock(VaadinService.class));

        binder.lockRequested(event);
        binder.lockAcquired(event);

        Assert.assertEquals(1L,
                registry.find(MeterNames.SESSION_LOCK_WAIT).timer().count());
    }

    @Test
    public void releaseRecordsHoldTimer() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SessionLockMetricsBinder binder = new SessionLockMetricsBinder(
                registry);
        SessionLockEvent event = new SessionLockEvent(
                Mockito.mock(VaadinService.class));

        binder.lockRequested(event);
        binder.lockAcquired(event);
        binder.lockReleased(event);

        Assert.assertEquals(1L,
                registry.find(MeterNames.SESSION_LOCK_HOLD).timer().count());
    }

    @Test
    public void releaseWithoutAcquireDoesNotRecordHold() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SessionLockMetricsBinder binder = new SessionLockMetricsBinder(
                registry);
        SessionLockEvent event = new SessionLockEvent(
                Mockito.mock(VaadinService.class));

        binder.lockReleased(event);

        Assert.assertNull(registry.find(MeterNames.SESSION_LOCK_HOLD).timer());
    }

    @Test
    public void observationPathEmitsWaitAndHoldSpans() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        SessionLockMetricsBinder binder = new SessionLockMetricsBinder(
                new SimpleMeterRegistry(), obs, true);
        SessionLockEvent event = new SessionLockEvent(
                Mockito.mock(VaadinService.class));

        binder.lockRequested(event);
        binder.lockAcquired(event);
        binder.lockReleased(event);

        // Wait span stops on acquire, hold span stops on release.
        Assert.assertEquals(List.of(MeterNames.SESSION_LOCK_WAIT,
                MeterNames.SESSION_LOCK_HOLD), recorder.names);
        Assert.assertEquals(
                List.of(MeterNames.SESSION_LOCK_WAIT,
                        MeterNames.SESSION_LOCK_HOLD),
                recorder.contextualNames);
        // No current request in this unit test, so context is "access".
        Assert.assertEquals(MeterNames.CONTEXT_ACCESS,
                recorder.tags.get(0).get(MeterNames.TAG_CONTEXT));
        Assert.assertEquals(MeterNames.CONTEXT_ACCESS,
                recorder.tags.get(1).get(MeterNames.TAG_CONTEXT));
    }

    @Test
    public void noObservationWhenTracesDisabled() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        SessionLockMetricsBinder binder = new SessionLockMetricsBinder(
                new SimpleMeterRegistry(), obs, false);
        SessionLockEvent event = new SessionLockEvent(
                Mockito.mock(VaadinService.class));

        binder.lockRequested(event);
        binder.lockAcquired(event);
        binder.lockReleased(event);

        Assert.assertTrue("no span should fire when traces are disabled",
                recorder.names.isEmpty());
    }
}
