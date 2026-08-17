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
package com.vaadin.flow;

import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Verifies what actually keeps a test class off concurrent threads on the JUnit
 * Platform.
 * <p>
 * Roughly 60 test classes in this repository are annotated with jcip's
 * {@link NotThreadSafe}. That annotation is only understood by Surefire's JUnit
 * 4 providers ({@code surefire-junit47}) and only when {@code parallel} is
 * configured. All those classes now run through {@code JUnitPlatformProvider},
 * which never looks at jcip annotations, so the marker has no effect on how the
 * tests are scheduled.
 * <p>
 * Each case below is executed by a nested JUnit Platform launcher with Jupiter
 * parallelism forced on, so the scheduling effect of the annotation is directly
 * observable instead of inferred.
 */
public class NotThreadSafeAnnotationTest {

    /**
     * Records whether more than one test method was in flight at the same time.
     */
    private static class Concurrency {
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger peak = new AtomicInteger();

        void run() throws InterruptedException {
            peak.accumulateAndGet(active.incrementAndGet(), Math::max);
            Thread.sleep(300);
            active.decrementAndGet();
        }

        boolean overlapped() {
            return peak.get() > 1;
        }
    }

    /** No marker at all: proves the nested launcher really runs in parallel. */
    static class UnmarkedCase {
        static final Concurrency CONCURRENCY = new Concurrency();

        @Test
        void first() throws InterruptedException {
            CONCURRENCY.run();
        }

        @Test
        void second() throws InterruptedException {
            CONCURRENCY.run();
        }
    }

    /** What the repository uses today. */
    @NotThreadSafe
    static class JcipMarkedCase {
        static final Concurrency CONCURRENCY = new Concurrency();

        @Test
        void first() throws InterruptedException {
            CONCURRENCY.run();
        }

        @Test
        void second() throws InterruptedException {
            CONCURRENCY.run();
        }
    }

    /** The Jupiter equivalent of the intent behind {@code @NotThreadSafe}. */
    @Execution(ExecutionMode.SAME_THREAD)
    static class SameThreadMarkedCase {
        static final Concurrency CONCURRENCY = new Concurrency();

        @Test
        void first() throws InterruptedException {
            CONCURRENCY.run();
        }

        @Test
        void second() throws InterruptedException {
            CONCURRENCY.run();
        }
    }

    @Test
    public void jcipAnnotation_isHonoredByTheJUnitPlatform() {
        assertTrue(runInParallel(UnmarkedCase.class, UnmarkedCase.CONCURRENCY),
                "Test setup problem: the nested launcher did not run the "
                        + "unmarked test class in parallel, so the rest of "
                        + "this test proves nothing");
        assertFalse(
                runInParallel(SameThreadMarkedCase.class,
                        SameThreadMarkedCase.CONCURRENCY),
                "@Execution(SAME_THREAD) should keep the test methods on one "
                        + "thread");

        assertFalse(
                runInParallel(JcipMarkedCase.class, JcipMarkedCase.CONCURRENCY),
                "net.jcip.annotations.NotThreadSafe did not keep the test "
                        + "methods off concurrent threads. It is only honored "
                        + "by Surefire's JUnit 4 providers, so on the JUnit "
                        + "Platform the ~60 classes annotated with it are not "
                        + "protected from parallel execution");
    }

    private boolean runInParallel(Class<?> testCase, Concurrency concurrency) {
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testCase))
                .configurationParameter(
                        "junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.mode.default",
                        "concurrent")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.config.strategy",
                        "fixed")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.config.fixed.parallelism",
                        "2")
                .build();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, listener);

        var summary = listener.getSummary();
        assertEquals(0, summary.getTotalFailureCount(),
                () -> "Nested run of " + testCase.getSimpleName() + " failed");
        assertEquals(2, summary.getTestsSucceededCount(),
                () -> "Expected 2 test methods to run in "
                        + testCase.getSimpleName());

        return concurrency.overlapped();
    }
}
