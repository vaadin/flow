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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.micrometer.client.MetricsCollectorElement;
import com.vaadin.flow.micrometer.trace.VaadinObservationNames;
import com.vaadin.flow.server.communication.RpcInvocationEvent;
import com.vaadin.flow.server.communication.RpcInvocationListener;

/**
 * Emits one tracing span per client-to-server RPC invocation (DOM event,
 * {@code @ClientCallable}/template method call, server-side navigation, return
 * channel message) so a long {@code vaadin.session.lock.hold} span can be
 * broken down into the individual invocations that ran inside it.
 * <p>
 * The Observation is started on {@code invocationStarted} while the lock-hold
 * Observation scope is open, so each {@code vaadin.rpc.<type>} span nests under
 * the hold span and the trace timeline shows which event or method consumed the
 * time. The Observation name matches a Timer name
 * ({@link MeterNames#RPC_DURATION}) so a {@code DefaultMeterObservationHandler}
 * also produces a {@code vaadin.rpc.duration} Timer dimensioned by RPC type and
 * outcome. The invocation detail (event name, method, location) and node id are
 * recorded as high-cardinality attributes that surface on the span but do not
 * inflate Timer tag cardinality.
 * <p>
 * Registered only when {@code traces} is on and an {@link ObservationRegistry}
 * is available; otherwise the Flow RPC machinery has no listener and skips
 * firing events entirely.
 */
final class RpcMetricsBinder implements RpcInvocationListener {

    private final transient ObservationRegistry observationRegistry;
    private final transient ThreadLocal<Observation> current = new ThreadLocal<>();
    private final transient ThreadLocal<Boolean> errored = ThreadLocal
            .withInitial(() -> Boolean.FALSE);

    RpcMetricsBinder(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void invocationStarted(RpcInvocationEvent event) {
        if (observationRegistry == null || isMetricsCollectorTarget(event)) {
            return;
        }
        Observation obs = Observation
                .createNotStarted(MeterNames.RPC_DURATION, observationRegistry)
                .contextualName(
                        VaadinObservationNames.RPC + "." + event.getType())
                .lowCardinalityKeyValue(VaadinObservationNames.KEY_RPC_TYPE,
                        event.getType());
        if (event.getName() != null) {
            obs.highCardinalityKeyValue(VaadinObservationNames.KEY_RPC_NAME,
                    event.getName());
        }
        if (event.getNodeId() >= 0) {
            obs.highCardinalityKeyValue(VaadinObservationNames.KEY_NODE_ID,
                    Integer.toString(event.getNodeId()));
        }
        obs.start();
        current.set(obs);
    }

    @Override
    public void invocationFailed(RpcInvocationEvent event, Throwable error) {
        errored.set(Boolean.TRUE);
        Observation obs = current.get();
        if (obs != null && error != null) {
            obs.error(error);
        }
    }

    @Override
    public void invocationEnded(RpcInvocationEvent event) {
        boolean wasError = errored.get();
        errored.remove();
        Observation obs = current.get();
        current.remove();
        if (obs != null) {
            obs.lowCardinalityKeyValue(VaadinObservationNames.KEY_OUTCOME,
                    wasError ? VaadinObservationNames.OUTCOME_ERROR
                            : VaadinObservationNames.OUTCOME_SUCCESS);
            obs.stop();
        }
    }

    /**
     * Suppresses spans for the metrics module's own client-sample callback (the
     * {@code recordSamples} {@link com.vaadin.flow.component.ClientCallable} on
     * the hidden {@link MetricsCollectorElement}). Matching on the target
     * component rather than the method name avoids colliding with an
     * application method that happens to share the name. Best-effort: any
     * lookup failure falls through to emitting the span.
     */
    private static boolean isMetricsCollectorTarget(RpcInvocationEvent event) {
        if (event.getNodeId() < 0) {
            return false;
        }
        UI ui = event.getUI();
        if (ui == null || ui.getInternals() == null
                || ui.getInternals().getStateTree() == null) {
            return false;
        }
        StateNode node = ui.getInternals().getStateTree()
                .getNodeById(event.getNodeId());
        if (node == null) {
            return false;
        }
        return ElementUtil.from(node).flatMap(element -> element.getComponent())
                .filter(MetricsCollectorElement.class::isInstance).isPresent();
    }
}
