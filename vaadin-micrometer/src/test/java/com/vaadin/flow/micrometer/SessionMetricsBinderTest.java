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

import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

public class SessionMetricsBinderTest {

    @Test
    public void initIncrementsCounterAndActive() throws ServiceException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SessionMetricsBinder binder = new SessionMetricsBinder(registry);
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        binder.sessionInit(new SessionInitEvent(service, session, null));

        Assert.assertEquals(1.0,
                registry.counter(MeterNames.SESSIONS_CREATED).count(), 0.0);
        Assert.assertEquals(1.0,
                registry.find(MeterNames.SESSIONS_ACTIVE).gauge().value(), 0.0);
    }

    @Test
    public void destroyDecrementsActiveAndRecordsDuration()
            throws ServiceException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SessionMetricsBinder binder = new SessionMetricsBinder(registry);
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        binder.sessionInit(new SessionInitEvent(service, session, null));
        binder.sessionDestroy(new SessionDestroyEvent(service, session));

        Assert.assertEquals(0.0,
                registry.find(MeterNames.SESSIONS_ACTIVE).gauge().value(), 0.0);
        Assert.assertEquals(1L,
                registry.find(MeterNames.SESSIONS_DURATION).timer().count());
    }

    @Test
    public void destroyWithoutInitOnlyDecrementsAndDoesNotRecordDuration() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SessionMetricsBinder binder = new SessionMetricsBinder(registry);
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        binder.sessionDestroy(new SessionDestroyEvent(service, session));

        Assert.assertEquals(-1.0,
                registry.find(MeterNames.SESSIONS_ACTIVE).gauge().value(), 0.0);
        Assert.assertEquals(0L,
                registry.find(MeterNames.SESSIONS_DURATION).timer().count());
    }
}
