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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Verifies that {@link Isolated} is what keeps a test class off concurrent
 * threads on the JUnit Platform.
 * <p>
 * Test classes that touch process-global state used to carry jcip's
 * {@code @NotThreadSafe} instead. That annotation is only understood by
 * Surefire's JUnit 4 providers, and only when {@code parallel} is configured,
 * so it had no effect once every module started running through
 * {@code JUnitPlatformProvider}. {@code @Isolated} is the equivalent the
 * platform actually honors, and an enforcer rule keeps the jcip annotations
 * from coming back.
 * <p>
 * Parallelism is off for this build (see the surefire configuration in the root
 * pom), so the markers are dormant. This test forces parallelism on in a nested
 * launcher to show they are correct for the day it is switched on.
 */
public class IsolatedAnnotationTest {

    private static final AtomicInteger ACTIVE = new AtomicInteger();
    private static final AtomicInteger PEAK = new AtomicInteger();

    private static void recordOverlap() throws InterruptedException {
        PEAK.accumulateAndGet(ACTIVE.incrementAndGet(), Math::max);
        Thread.sleep(300);
        ACTIVE.decrementAndGet();
    }

    static class PlainCase {
        @Test
        void first() throws InterruptedException {
            recordOverlap();
        }

        @Test
        void second() throws InterruptedException {
            recordOverlap();
        }
    }

    static class OtherPlainCase {
        @Test
        void first() throws InterruptedException {
            recordOverlap();
        }

        @Test
        void second() throws InterruptedException {
            recordOverlap();
        }
    }

    @Isolated
    static class IsolatedCase {
        @Test
        void first() throws InterruptedException {
            recordOverlap();
        }

        @Test
        void second() throws InterruptedException {
            recordOverlap();
        }
    }

    @Test
    public void isolatedClass_neverRunsAlongsideAnotherTestClass() {
        assertTrue(peakConcurrency(PlainCase.class, OtherPlainCase.class) > 1,
                "Test setup problem: two unmarked test classes did not overlap, "
                        + "so the nested launcher is not running in parallel "
                        + "and the assertion below proves nothing");

        assertEquals(1,
                peakConcurrency(IsolatedCase.class, OtherPlainCase.class),
                "An @Isolated test class ran at the same time as another test "
                        + "class");
    }

    private int peakConcurrency(Class<?>... testCases) {
        ACTIVE.set(0);
        PEAK.set(0);

        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(Arrays.stream(testCases)
                        .map(testCase -> selectClass(testCase)).toList())
                .configurationParameter(
                        "junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.mode.default",
                        "same_thread")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.mode.classes.default",
                        "concurrent")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.config.strategy",
                        "fixed")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.config.fixed.parallelism",
                        "4")
                .build();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, listener);

        var summary = listener.getSummary();
        assertEquals(0, summary.getTotalFailureCount(),
                "Nested test run failed");
        assertEquals(2L * testCases.length, summary.getTestsSucceededCount(),
                "Nested test run did not execute all test methods");

        return PEAK.get();
    }
}
