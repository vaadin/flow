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
package com.vaadin.flow.micrometer.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.Assert;
import org.junit.Test;

public class TracingExecutorObservationTest {

    private static final class NameRecorder
            implements ObservationHandler<Observation.Context> {

        final List<String> names = new ArrayList<>();
        final List<String> contextualNames = new ArrayList<>();
        final AtomicBoolean errored = new AtomicBoolean();

        @Override
        public void onStop(Observation.Context ctx) {
            names.add(ctx.getName());
            contextualNames.add(ctx.getContextualName());
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
    public void everyTaskEmitsUiAccessObservation() {
        ObservationRegistry obs = ObservationRegistry.create();
        NameRecorder recorder = new NameRecorder();
        obs.observationConfig().observationHandler(recorder);

        Executor inline = Runnable::run;
        TracingExecutor te = new TracingExecutor(inline, obs);

        te.execute(() -> {
        });
        te.execute(() -> {
        });

        Assert.assertEquals(2, recorder.names.size());
        Assert.assertEquals(VaadinObservationNames.UI_ACCESS,
                recorder.names.get(0));
        Assert.assertEquals(VaadinObservationNames.UI_ACCESS,
                recorder.contextualNames.get(0));
    }

    @Test
    public void exceptionsArePropagatedAndRecordedOnObservation() {
        ObservationRegistry obs = ObservationRegistry.create();
        NameRecorder recorder = new NameRecorder();
        obs.observationConfig().observationHandler(recorder);

        TracingExecutor te = new TracingExecutor(Runnable::run, obs);

        try {
            te.execute(() -> {
                throw new IllegalStateException("boom");
            });
            Assert.fail("expected exception");
        } catch (IllegalStateException expected) {
            // expected
        }
        Assert.assertTrue(recorder.errored.get());
    }

    @Test
    public void noObservationWhenRegistryAbsent() {
        ObservationRegistry obs = ObservationRegistry.create();
        NameRecorder recorder = new NameRecorder();
        obs.observationConfig().observationHandler(recorder);

        TracingExecutor te = new TracingExecutor(Runnable::run);
        AtomicBoolean ran = new AtomicBoolean();

        te.execute(() -> ran.set(true));

        Assert.assertTrue(ran.get());
        Assert.assertTrue(
                "no observation should be emitted when registry " + "is null",
                recorder.names.isEmpty());
    }
}
