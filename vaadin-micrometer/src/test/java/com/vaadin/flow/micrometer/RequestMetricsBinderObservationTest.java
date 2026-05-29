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
import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.micrometer.trace.VaadinObservationNames;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

public class RequestMetricsBinderObservationTest {

    private static final class RecordingHandler
            implements ObservationHandler<Observation.Context> {

        final List<String> names = new ArrayList<>();
        final List<String> contextualNames = new ArrayList<>();
        final List<Map<String, String>> tags = new ArrayList<>();
        final AtomicBoolean errored = new AtomicBoolean();

        @Override
        public void onStop(Observation.Context ctx) {
            names.add(ctx.getName());
            contextualNames.add(ctx.getContextualName());
            Map<String, String> snap = new HashMap<>();
            for (KeyValue kv : ctx.getLowCardinalityKeyValues()) {
                snap.put(kv.getKey(), kv.getValue());
            }
            tags.add(snap);
            if (ctx.getError() != null) {
                errored.set(true);
            }
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }

    @Test
    public void observationProducesExpectedNameAndTagsOnSuccess() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        RequestMetricsBinder binder = new RequestMetricsBinder(
                new SimpleMeterRegistry(), obs, VaadinMetricsConfig.defaults());

        VaadinRequest req = Mockito.mock(VaadinRequest.class);
        Mockito.when(req.getParameter("v-r")).thenReturn("uidl");
        VaadinResponse resp = Mockito.mock(VaadinResponse.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        binder.requestStart(req, resp);
        binder.requestEnd(req, resp, session);

        Assert.assertEquals(1, recorder.names.size());
        Assert.assertEquals(MeterNames.REQUEST_DURATION, recorder.names.get(0));
        // No poll/navigation marked, so a plain UIDL request is labelled as a
        // generic "rpc" interaction rather than the opaque "uidl".
        Assert.assertEquals(
                VaadinObservationNames.REQUEST + "."
                        + VaadinObservationNames.INTERACTION_RPC,
                recorder.contextualNames.get(0));
        Assert.assertEquals("uidl", recorder.tags.get(0)
                .get(VaadinObservationNames.KEY_REQUEST_TYPE));
        Assert.assertEquals(VaadinObservationNames.INTERACTION_RPC,
                recorder.tags.get(0)
                        .get(VaadinObservationNames.KEY_INTERACTION));
        Assert.assertEquals(VaadinObservationNames.OUTCOME_SUCCESS,
                recorder.tags.get(0).get(VaadinObservationNames.KEY_OUTCOME));
        Assert.assertFalse(recorder.errored.get());
    }

    @Test
    public void pollMarkerLabelsRequestAsPoll() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        RequestMetricsBinder binder = new RequestMetricsBinder(
                new SimpleMeterRegistry(), obs, VaadinMetricsConfig.defaults());

        VaadinRequest req = Mockito.mock(VaadinRequest.class);
        Mockito.when(req.getParameter("v-r")).thenReturn("uidl");
        VaadinResponse resp = Mockito.mock(VaadinResponse.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        binder.requestStart(req, resp);
        // Simulate a poll listener firing during request handling.
        RequestInteraction.mark(VaadinObservationNames.INTERACTION_POLL);
        binder.requestEnd(req, resp, session);

        Assert.assertEquals(
                VaadinObservationNames.REQUEST + "."
                        + VaadinObservationNames.INTERACTION_POLL,
                recorder.contextualNames.get(0));
        Assert.assertEquals(VaadinObservationNames.INTERACTION_POLL,
                recorder.tags.get(0)
                        .get(VaadinObservationNames.KEY_INTERACTION));
    }

    @Test
    public void staleMarkerIsClearedAtRequestStart() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        RequestMetricsBinder binder = new RequestMetricsBinder(
                new SimpleMeterRegistry(), obs, VaadinMetricsConfig.defaults());

        VaadinRequest req = Mockito.mock(VaadinRequest.class);
        Mockito.when(req.getParameter("v-r")).thenReturn("uidl");
        VaadinResponse resp = Mockito.mock(VaadinResponse.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        // Leftover marker from a prior request on this thread.
        RequestInteraction.mark(VaadinObservationNames.INTERACTION_POLL);
        binder.requestStart(req, resp);
        binder.requestEnd(req, resp, session);

        Assert.assertEquals(VaadinObservationNames.INTERACTION_RPC,
                recorder.tags.get(0)
                        .get(VaadinObservationNames.KEY_INTERACTION));
    }

    @Test
    public void observationCarriesErrorAndOutcomeOnException() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        RequestMetricsBinder binder = new RequestMetricsBinder(
                new SimpleMeterRegistry(), obs, VaadinMetricsConfig.defaults());

        VaadinRequest req = Mockito.mock(VaadinRequest.class);
        VaadinResponse resp = Mockito.mock(VaadinResponse.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        binder.requestStart(req, resp);
        binder.handleException(req, resp, session,
                new IllegalStateException("boom"));
        binder.requestEnd(req, resp, session);

        Assert.assertEquals(VaadinObservationNames.OUTCOME_ERROR,
                recorder.tags.get(0).get(VaadinObservationNames.KEY_OUTCOME));
        Assert.assertTrue(recorder.errored.get());
    }

    @Test
    public void noObservationWhenTracesDisabled() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        RequestMetricsBinder binder = new RequestMetricsBinder(
                new SimpleMeterRegistry(), obs,
                VaadinMetricsConfig.builder().traces(false).build());

        VaadinRequest req = Mockito.mock(VaadinRequest.class);
        VaadinResponse resp = Mockito.mock(VaadinResponse.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);

        binder.requestStart(req, resp);
        binder.requestEnd(req, resp, session);

        Assert.assertTrue("no observation should fire when traces are disabled",
                recorder.names.isEmpty());
    }
}
