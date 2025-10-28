/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.lang.StackWalker.StackFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.vaadin.flow.component.internal.ComponentTracker.Location;

/**
 * Performance comparison test between different component tracking approaches.
 *
 * Run manually with: mvn test-compile exec:java
 * -Dexec.mainClass="com.vaadin.flow.ComponentTrackerPerformanceTest"
 * -Dexec.classpathScope=test
 *
 * Compares performance between:
 * 1. Original: Thread.getStackTrace() with eager processing
 * 2. StackWalker: StackWalker API with eager processing
 * 3. Lazy: Throwable with deferred processing (current implementation)
 */
public class ComponentTrackerPerformanceTest {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int COMPONENT_COUNT = 10000;
    private static final int ACCESS_COUNT = 5;
    private static final int TEST_ROUNDS = 3;

    private static final StackWalker stackWalker = StackWalker
            .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static void main(String[] args) {
        new ComponentTrackerPerformanceTest().compareRealisticScenario();
    }

    public void compareRealisticScenario() {
        System.out.println(
                "=== ComponentTracker Performance Comparison ===\n");
        System.out.println(
                "Realistic scenario: Create " + COMPONENT_COUNT
                        + " components, access location for " + ACCESS_COUNT
                        + "\n");

        // Warmup phase to let JIT compiler optimize
        System.out.println("Warming up JIT compiler...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            originalEagerApproach();
            stackWalkerEagerApproach();
            lazyThrowableApproach();
        }
        System.out.println("Warmup complete.\n");

        System.out.println("Running benchmark (" + TEST_ROUNDS + " rounds)...");
        System.out.println("---\n");

        long originalTime = 0;
        long stackWalkerTime = 0;
        long lazyTime = 0;

        for (int round = 1; round <= TEST_ROUNDS; round++) {
            // Test original approach
            System.gc();
            sleep(100);
            long start = System.nanoTime();
            originalEagerApproach();
            originalTime += System.nanoTime() - start;

            // Test StackWalker approach
            System.gc();
            sleep(100);
            start = System.nanoTime();
            stackWalkerEagerApproach();
            stackWalkerTime += System.nanoTime() - start;

            // Test lazy approach
            System.gc();
            sleep(100);
            start = System.nanoTime();
            lazyThrowableApproach();
            lazyTime += System.nanoTime() - start;

            System.out.printf("Round %d complete\n", round);
        }

        double avgOriginal = originalTime / (double) TEST_ROUNDS / 1_000_000.0;
        double avgStackWalker = stackWalkerTime / (double) TEST_ROUNDS
                / 1_000_000.0;
        double avgLazy = lazyTime / (double) TEST_ROUNDS / 1_000_000.0;

        System.out.println("\n=== Results ===");
        System.out.printf(
                "Original (Thread.getStackTrace + eager): %.2f ms average\n",
                avgOriginal);
        System.out.printf("StackWalker (eager processing):         %.2f ms average\n",
                avgStackWalker);
        System.out.printf("Lazy (Throwable + deferred):            %.2f ms average\n",
                avgLazy);

        System.out.println("\n=== Comparison ===");
        printComparison("StackWalker vs Original", originalTime,
                stackWalkerTime);
        printComparison("Lazy vs Original", originalTime, lazyTime);
        printComparison("Lazy vs StackWalker", stackWalkerTime, lazyTime);
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void printComparison(String label, long baselineTime,
            long newTime) {
        double ratio = (double) baselineTime / newTime;
        if (ratio >= 1.0) {
            // New is faster
            double percentFaster = ((baselineTime - newTime) * 100.0
                    / baselineTime);
            System.out.printf("%s: %.2fx faster (%.1f%% reduction)\n", label,
                    ratio, percentFaster);
        } else {
            // New is slower
            double slowerRatio = (double) newTime / baselineTime;
            double percentSlower = ((newTime - baselineTime) * 100.0
                    / baselineTime);
            System.out.printf("%s: %.2fx SLOWER (%.1f%% increase)\n", label,
                    slowerRatio, percentSlower);
        }
    }

    // ===== ORIGINAL IMPLEMENTATION (Thread.getStackTrace with eager
    // processing) =====

    private void originalEagerApproach() {
        // Simulates original implementation: process stack immediately on
        // trackCreate()
        List<StackInfo> components = new ArrayList<>();
        for (int i = 0; i < COMPONENT_COUNT; i++) {
            StackTraceElement[] stack = Thread.currentThread()
                    .getStackTrace();
            Location[] allLocations = Stream.of(stack)
                    .map(this::toLocationFromStackTraceElement)
                    .toArray(Location[]::new);
            // Find relevant location immediately
            Location location = findRelevant(allLocations);
            components.add(new StackInfo(location));
        }

        // Access only a few locations (simulates rare access in production)
        for (int i = 0; i < ACCESS_COUNT; i++) {
            Location loc = components.get(i * (COMPONENT_COUNT / ACCESS_COUNT)).location;
        }
    }

    // ===== STACKWALKER IMPLEMENTATION (StackWalker with eager processing)
    // =====

    private void stackWalkerEagerApproach() {
        // Simulates StackWalker implementation: process stack immediately on
        // trackCreate()
        List<StackInfo> components = new ArrayList<>();
        for (int i = 0; i < COMPONENT_COUNT; i++) {
            Location[] allLocations = stackWalker.walk(
                    frames -> frames.map(this::toLocationFromStackFrame)
                            .toArray(Location[]::new));
            // Find relevant location immediately
            Location location = findRelevant(allLocations);
            components.add(new StackInfo(location));
        }

        // Access only a few locations (simulates rare access in production)
        for (int i = 0; i < ACCESS_COUNT; i++) {
            Location loc = components.get(i * (COMPONENT_COUNT / ACCESS_COUNT)).location;
        }
    }

    // ===== LAZY IMPLEMENTATION (Throwable with deferred processing) =====

    private void lazyThrowableApproach() {
        // Simulates current lazy implementation: store Throwable only on
        // trackCreate()
        List<Throwable> components = new ArrayList<>();
        for (int i = 0; i < COMPONENT_COUNT; i++) {
            components.add(new Throwable());
        }

        // Access only a few locations - process on demand
        for (int i = 0; i < ACCESS_COUNT; i++) {
            Throwable t = components.get(i * (COMPONENT_COUNT / ACCESS_COUNT));
            StackTraceElement[] stack = t.getStackTrace();
            Location[] allLocations = Stream.of(stack)
                    .map(this::toLocationFromStackTraceElement)
                    .toArray(Location[]::new);
            Location location = findRelevant(allLocations);
        }
    }

    // ===== HELPER METHODS =====

    private Location findRelevant(Location[] locations) {
        for (Location location : locations) {
            if (location != null
                    && !location.className().startsWith("com.vaadin.flow.")
                    && !location.className().startsWith("java.")
                    && !location.className().startsWith("jdk.")) {
                return location;
            }
        }
        return null;
    }

    private Location toLocationFromStackTraceElement(
            StackTraceElement element) {
        if (element == null) {
            return null;
        }
        return new Location(element.getClassName(), element.getFileName(),
                element.getMethodName(), element.getLineNumber());
    }

    private Location toLocationFromStackFrame(StackFrame frame) {
        if (frame == null) {
            return null;
        }
        return new Location(frame.getClassName(), frame.getFileName(),
                frame.getMethodName(), frame.getLineNumber());
    }

    static class StackInfo {
        Location location;

        StackInfo(Location location) {
            this.location = location;
        }
    }
}
