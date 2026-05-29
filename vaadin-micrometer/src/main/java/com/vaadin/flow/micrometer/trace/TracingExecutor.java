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

import java.util.Objects;
import java.util.concurrent.Executor;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * Wraps a Vaadin service {@link Executor} so that
 * <ol>
 * <li>the trace context (and any other
 * {@link io.micrometer.context.ThreadLocalAccessor}-backed state) active when a
 * task is <em>submitted</em> is restored when the task <em>runs</em>; and</li>
 * <li>each task gets its own {@code vaadin.ui.access} span when an
 * {@link ObservationRegistry} is supplied. The span is parented to the
 * propagated trace, so a click that schedules a {@code UI.access(...)} ends up
 * with a continuous trace tree across thread hops.</li>
 * </ol>
 */
public final class TracingExecutor implements Executor {

    private final Executor delegate;
    private final ObservationRegistry observationRegistry;
    private final ContextSnapshotFactory snapshotFactory;

    public TracingExecutor(Executor delegate) {
        this(delegate, null);
    }

    public TracingExecutor(Executor delegate,
            ObservationRegistry observationRegistry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.observationRegistry = observationRegistry;
        this.snapshotFactory = ContextSnapshotFactory.builder().build();
    }

    @Override
    public void execute(Runnable command) {
        ContextSnapshot snapshot = snapshotFactory.captureAll();
        delegate.execute(snapshot.wrap(() -> {
            if (observationRegistry == null) {
                command.run();
                return;
            }
            Observation observation = Observation
                    .createNotStarted(VaadinObservationNames.UI_ACCESS,
                            observationRegistry)
                    .contextualName(VaadinObservationNames.UI_ACCESS).start();
            try (Observation.Scope ignored = observation.openScope()) {
                command.run();
            } catch (Throwable t) {
                observation.error(t);
                throw t;
            } finally {
                observation.stop();
            }
        }));
    }

    /** Exposed for tests. */
    Executor delegate() {
        return delegate;
    }
}
