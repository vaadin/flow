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

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Pins down what {@link Isolated} buys the test classes that touch
 * process-global state: no other test class runs alongside them, and their own
 * test methods do not run alongside each other either, because the platform
 * forces same-thread execution on everything below a node holding the global
 * lock.
 * <p>
 * Tests run single threaded in this build (see the surefire configuration in
 * the root pom), so the marker is dormant and a mistake in it would not show up
 * in a normal run. Every case here therefore runs in a nested launcher with
 * parallelism forced on.
 * <p>
 * The two unmarked cases are controls, one for each overlap the launcher has to
 * be capable of producing: two methods of the same class, and methods of two
 * different classes. Without them an assertion of the form "nothing overlapped"
 * would hold just as well when the launcher runs everything sequentially, so a
 * renamed configuration key could quietly turn the real assertions into no-ops.
 */
@Isolated
public class IsolatedAnnotationTest {

    private static final long WINDOW_MS = 200;

    private static CyclicBarrier methodBarrier;
    private static volatile boolean methodsMet;

    private static CyclicBarrier classBarrier;
    private static volatile boolean classesMet;

    private static final AtomicInteger activeMarked = new AtomicInteger();
    private static final AtomicInteger peakMarked = new AtomicInteger();
    private static final AtomicInteger activeOther = new AtomicInteger();
    private static volatile boolean markedMetOther;

    /**
     * Blocks until a second test method reaches the same barrier, so a control
     * cannot pass on lucky timing the way a sleep-based check could.
     */
    private static void meetAt(CyclicBarrier barrier, Runnable onMet) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
            onMet.run();
        } catch (Exception e) { // NOSONAR
            // Times out when nothing runs in parallel, which is what the
            // controls assert on
        }
    }

    /**
     * Holds a window open long enough for a concurrently running method to be
     * seen. Can only miss overlap, never invent it, so a slow machine cannot
     * turn the assertions that expect no overlap into false failures.
     */
    private static void markedWindow() throws InterruptedException {
        peakMarked.accumulateAndGet(activeMarked.incrementAndGet(), Math::max);
        Thread.sleep(WINDOW_MS);
        if (activeOther.get() > 0) {
            markedMetOther = true;
        }
        activeMarked.decrementAndGet();
    }

    private static void otherWindow() throws InterruptedException {
        activeOther.incrementAndGet();
        Thread.sleep(WINDOW_MS);
        if (activeMarked.get() > 0) {
            markedMetOther = true;
        }
        activeOther.decrementAndGet();
    }

    /** Control for two methods of one class overlapping. */
    static class TwoMethodCase {
        @Test
        void first() {
            meetAt(methodBarrier, () -> methodsMet = true);
        }

        @Test
        void second() {
            meetAt(methodBarrier, () -> methodsMet = true);
        }
    }

    /**
     * Control for methods of two different classes overlapping, together with
     * {@link SecondSingleMethodCase}. One test method each, so the barrier can
     * only trip across the class boundary.
     */
    static class FirstSingleMethodCase {
        @Test
        void only() {
            meetAt(classBarrier, () -> classesMet = true);
        }
    }

    static class SecondSingleMethodCase {
        @Test
        void only() {
            meetAt(classBarrier, () -> classesMet = true);
        }
    }

    /** Carries the marker that the converted test classes carry. */
    @Isolated
    static class IsolatedCase {
        @Test
        void first() throws InterruptedException {
            markedWindow();
        }

        @Test
        void second() throws InterruptedException {
            markedWindow();
        }
    }

    /** Unmarked class used to check that IsolatedCase stays away from it. */
    static class PlainCase {
        @Test
        void first() throws InterruptedException {
            otherWindow();
        }

        @Test
        void second() throws InterruptedException {
            otherWindow();
        }
    }

    @Test
    public void unmarkedClass_methodsRunConcurrently() {
        run(2, TwoMethodCase.class);

        assertTrue(methodsMet,
                "Test setup problem: two methods of an unmarked class never "
                        + "met, so the nested launcher is not running methods "
                        + "in parallel and the assertion about the methods of "
                        + "an @Isolated class proves nothing");
    }

    @Test
    public void unmarkedClasses_overlapEachOther() {
        run(2, FirstSingleMethodCase.class, SecondSingleMethodCase.class);

        assertTrue(classesMet,
                "Test setup problem: methods of two unmarked test classes "
                        + "never ran at the same time, so the launcher cannot "
                        + "produce the overlap that the assertion about "
                        + "@Isolated keeping other classes away rules out");
    }

    @Test
    public void isolatedClass_ownMethodsStayOnOneThread() {
        run(2, IsolatedCase.class);

        assertEquals(1, peakMarked.get(),
                "@Isolated did not stop the test methods of one class from "
                        + "running at the same time");
    }

    @Test
    public void isolatedClass_neverRunsAlongsideAnotherClass() {
        run(4, IsolatedCase.class, PlainCase.class);

        assertFalse(markedMetOther,
                "@Isolated did not stop the test class from running at the "
                        + "same time as another test class");
    }

    private void run(int expectedTests, Class<?>... testCases) {
        methodBarrier = new CyclicBarrier(2);
        methodsMet = false;
        classBarrier = new CyclicBarrier(2);
        classesMet = false;
        activeMarked.set(0);
        peakMarked.set(0);
        activeOther.set(0);
        markedMetOther = false;

        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(Stream.of(testCases)
                        .map(testCase -> selectClass(testCase)).toList())
                .configurationParameter(
                        "junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.mode.default",
                        "concurrent")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.mode.classes.default",
                        "concurrent")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.config.strategy",
                        "fixed")
                .configurationParameter(
                        "junit.jupiter.execution.parallel.config.fixed.parallelism",
                        "8")
                .build();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, listener);

        var summary = listener.getSummary();
        assertEquals(0, summary.getTotalFailureCount(),
                "Nested test run failed");
        assertEquals(expectedTests, summary.getTestsSucceededCount(),
                "Nested test run did not execute all test methods");
    }
}
