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

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

public class RequestMetricsBinderTest {

    @Test
    public void successfulRequestRecordsDurationWithSuccessOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RequestMetricsBinder binder = new RequestMetricsBinder(registry,
                VaadinMetricsConfig.defaults());
        VaadinRequest req = Mockito.mock(VaadinRequest.class);
        VaadinResponse res = Mockito.mock(VaadinResponse.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        binder.requestStart(req, res);
        binder.requestEnd(req, res, session);

        Timer timer = registry.find(MeterNames.REQUEST_DURATION)
                .tag(MeterNames.TAG_OUTCOME, MeterNames.OUTCOME_SUCCESS)
                .timer();
        Assert.assertNotNull(timer);
        Assert.assertEquals(1L, timer.count());
    }

    @Test
    public void exceptionRecordsErrorOutcomeAndExceptionCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RequestMetricsBinder binder = new RequestMetricsBinder(registry,
                VaadinMetricsConfig.defaults());
        VaadinRequest req = Mockito.mock(VaadinRequest.class);
        VaadinResponse res = Mockito.mock(VaadinResponse.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        binder.requestStart(req, res);
        binder.handleException(req, res, session,
                new IllegalStateException("boom"));
        binder.requestEnd(req, res, session);

        Timer timer = registry.find(MeterNames.REQUEST_DURATION)
                .tag(MeterNames.TAG_OUTCOME, MeterNames.OUTCOME_ERROR).timer();
        Assert.assertNotNull(timer);
        Assert.assertEquals(1L, timer.count());

        Assert.assertEquals(1.0,
                registry.find(MeterNames.ERRORS)
                        .tag(MeterNames.TAG_EXCEPTION, "IllegalStateException")
                        .counter().count(),
                0.0);
    }
}
