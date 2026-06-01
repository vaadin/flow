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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.SessionLockEvent;
import com.vaadin.flow.server.VaadinService;

public class SessionLockMetricsBinderTest {

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
}
