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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.micrometer.client.ClientMetricsBinder;
import com.vaadin.flow.micrometer.client.MetricsCollectorElement;
import com.vaadin.flow.micrometer.trace.VaadinObservationNames;
import com.vaadin.flow.server.communication.RpcInvocationEvent;
import com.vaadin.tests.util.MockUI;

public class RpcMetricsBinderTest {

    private static final class RecordingHandler
            implements ObservationHandler<Observation.Context> {

        final List<String> names = new ArrayList<>();
        final List<String> contextualNames = new ArrayList<>();
        final List<Map<String, String>> lowCard = new ArrayList<>();
        final List<Map<String, String>> highCard = new ArrayList<>();

        @Override
        public void onStop(Observation.Context ctx) {
            names.add(ctx.getName());
            contextualNames.add(ctx.getContextualName());
            lowCard.add(snapshot(ctx.getLowCardinalityKeyValues()));
            highCard.add(snapshot(ctx.getHighCardinalityKeyValues()));
        }

        private static Map<String, String> snapshot(Iterable<KeyValue> kvs) {
            Map<String, String> snap = new HashMap<>();
            for (KeyValue kv : kvs) {
                snap.put(kv.getKey(), kv.getValue());
            }
            return snap;
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }

    private static RpcInvocationEvent event(String type, int nodeId,
            String name) {
        return new RpcInvocationEvent(Mockito.mock(UI.class), type, nodeId,
                name);
    }

    @Test
    public void clientCallableProducesNamedSpanWithDetailAttributes() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        RpcMetricsBinder binder = new RpcMetricsBinder(obs);
        RpcInvocationEvent event = event("publishedEventHandler", 42, "save");

        binder.invocationStarted(event);
        binder.invocationEnded(event);

        Assert.assertEquals(MeterNames.RPC_DURATION, recorder.names.get(0));
        Assert.assertEquals(
                VaadinObservationNames.RPC + ".publishedEventHandler",
                recorder.contextualNames.get(0));
        Assert.assertEquals("publishedEventHandler", recorder.lowCard.get(0)
                .get(VaadinObservationNames.KEY_RPC_TYPE));
        Assert.assertEquals(VaadinObservationNames.OUTCOME_SUCCESS,
                recorder.lowCard.get(0)
                        .get(VaadinObservationNames.KEY_OUTCOME));
        // Detail and node id are high-cardinality span attributes, not Timer
        // tags.
        Assert.assertEquals("save", recorder.highCard.get(0)
                .get(VaadinObservationNames.KEY_RPC_NAME));
        Assert.assertEquals("42", recorder.highCard.get(0)
                .get(VaadinObservationNames.KEY_NODE_ID));
    }

    @Test
    public void failedInvocationCarriesErrorOutcome() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        RpcMetricsBinder binder = new RpcMetricsBinder(obs);
        RpcInvocationEvent event = event("event", 7, "click");

        binder.invocationStarted(event);
        binder.invocationFailed(event, new IllegalStateException("boom"));
        binder.invocationEnded(event);

        Assert.assertEquals(VaadinObservationNames.OUTCOME_ERROR,
                recorder.lowCard.get(0)
                        .get(VaadinObservationNames.KEY_OUTCOME));
    }

    @Test
    public void missingDetailOmitsNameAttribute() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        RpcMetricsBinder binder = new RpcMetricsBinder(obs);
        // Node-less, detail-less invocation (e.g. an attach handler).
        RpcInvocationEvent event = event("attachExistingElement", -1, null);

        binder.invocationStarted(event);
        binder.invocationEnded(event);

        Assert.assertFalse(recorder.highCard.get(0)
                .containsKey(VaadinObservationNames.KEY_RPC_NAME));
        Assert.assertFalse(recorder.highCard.get(0)
                .containsKey(VaadinObservationNames.KEY_NODE_ID));
    }

    @Test
    public void clientSampleCallbackIsNotTraced() {
        ObservationRegistry obs = ObservationRegistry.create();
        RecordingHandler recorder = new RecordingHandler();
        obs.observationConfig().observationHandler(recorder);

        MockUI ui = new MockUI();
        MetricsCollectorElement collector = new MetricsCollectorElement(
                new ClientMetricsBinder(new SimpleMeterRegistry(),
                        VaadinMetricsConfig.defaults()),
                VaadinMetricsConfig.defaults());
        ui.add(collector);
        int nodeId = collector.getElement().getNode().getId();

        RpcMetricsBinder binder = new RpcMetricsBinder(obs);
        // The recordSamples @ClientCallable on the hidden collector element.
        RpcInvocationEvent event = new RpcInvocationEvent(ui,
                "publishedEventHandler", nodeId, "recordSamples");

        binder.invocationStarted(event);
        binder.invocationEnded(event);

        Assert.assertTrue(
                "the metrics module's own client-sample callback must not "
                        + "produce a span",
                recorder.names.isEmpty());
    }
}
