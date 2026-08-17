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

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Covers the marker that test classes touching process-global state carry.
 * {@link Isolated} is the JUnit Platform equivalent of jcip's
 * {@code @NotThreadSafe}, which only Surefire's JUnit 4 providers understand
 * and which therefore did nothing for these classes. It gives both halves of
 * what jcip gave: no other test class runs alongside the annotated one, and the
 * annotated class's own test methods do not run alongside each other either,
 * because the platform forces same-thread execution on everything below a node
 * holding the global lock.
 * <p>
 * Parallelism is off for the unit tests (see the surefire configuration in the
 * root pom), so the marker is dormant and a mistake in it would otherwise be
 * invisible. Each case below therefore runs in a nested launcher with
 * parallelism forced on. Dropping {@code @Isolated} from {@link IsolatedCase}
 * makes both scheduling assertions fail.
 */
@Isolated
public class IsolatedAnnotationTest {

    private static final long WINDOW_MS = 200;

    private static CyclicBarrier barrier;
    private static volatile boolean rendezvousReached;

    private static final AtomicInteger activeMarked = new AtomicInteger();
    private static final AtomicInteger peakMarked = new AtomicInteger();
    private static final AtomicInteger activeOther = new AtomicInteger();
    private static volatile boolean markedMetOther;

    /**
     * Blocks until another test method reaches the same point, which only
     * happens if the launcher really is running methods in parallel. Unlike a
     * timing window this cannot report a false negative.
     */
    private static void rendezvous() {
        try {
            barrier.await(30, TimeUnit.SECONDS);
            rendezvousReached = true;
        } catch (Exception e) { // NOSONAR
            // leaves rendezvousReached false, which the caller asserts on
        }
    }

    /**
     * Holds a window open long enough for a concurrently running method to be
     * observed. Can only under-report overlap, never invent it, so a slow
     * machine cannot turn the assertions below into false failures.
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

    /** Unmarked, so its two methods are free to run at the same time. */
    static class RendezvousCase {
        @Test
        void first() {
            rendezvous();
        }

        @Test
        void second() {
            rendezvous();
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
        run(RendezvousCase.class);

        assertTrue(rendezvousReached,
                "Test setup problem: two methods of an unmarked class never "
                        + "met, so the nested launcher is not running in "
                        + "parallel and the other cases here prove nothing");
    }

    @Test
    public void isolatedClass_ownMethodsStayOnOneThread() {
        run(IsolatedCase.class);

        assertEquals(1, peakMarked.get(),
                "@Isolated did not stop the test methods of one class from "
                        + "running at the same time");
    }

    @Test
    public void isolatedClass_neverRunsAlongsideAnotherClass() {
        run(IsolatedCase.class, PlainCase.class);

        assertFalse(markedMetOther,
                "@Isolated did not stop the test class from running at the "
                        + "same time as another test class");
    }

    /**
     * {@code @Isolated} is only honored on a class the engine treats as a test
     * class, so a marker that ends up on a helper type is silently useless.
     * Covers this module's test classes; the other modules carrying the marker
     * have no equivalent check.
     */
    @Test
    public void isolatedClasses_areTestClasses()
            throws IOException, URISyntaxException {
        Path root = Path.of(getClass().getProtectionDomain().getCodeSource()
                .getLocation().toURI());
        List<String> isolated = new ArrayList<>();
        List<String> withoutTests = new ArrayList<>();

        try (Stream<Path> classFiles = Files.walk(root)) {
            classFiles.filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> {
                        Class<?> cls = load(root, path);
                        if (cls != null
                                && cls.isAnnotationPresent(Isolated.class)) {
                            isolated.add(cls.getName());
                            if (!yieldsTests(cls)) {
                                withoutTests.add(cls.getName());
                            }
                        }
                    });
        }

        // Without this the walk, the name reconstruction or the class loading
        // could all quietly fail and leave an empty scan looking like a clean
        // one. IsolatedCase is right here in this file, so it has to turn up.
        assertTrue(isolated.contains(IsolatedCase.class.getName()),
                () -> "Scan is not inspecting anything: it walked " + root
                        + " without finding " + IsolatedCase.class.getName()
                        + ", only " + isolated);

        assertEquals(List.of(), withoutTests,
                "@Isolated has no effect on a class the test engine does not "
                        + "pick up; the engine finds no test in these classes");
    }

    /**
     * Asks the platform itself whether the class is one it would run, rather
     * than looking for test annotations by hand. That keeps classes whose tests
     * live in {@code @Nested} types, or behind a composed annotation, from
     * being reported as untested. Abstract classes are left alone: they cannot
     * be test classes themselves, and {@code @Isolated} is {@code @Inherited},
     * so a marker on a base class is there for the subclasses.
     */
    private boolean yieldsTests(Class<?> cls) {
        if (Modifier.isAbstract(cls.getModifiers())) {
            return true;
        }
        try {
            return LauncherFactory.create()
                    .discover(LauncherDiscoveryRequestBuilder.request()
                            .selectors(selectClass(cls)).build())
                    .countTestIdentifiers(TestIdentifier::isTest) > 0;
        } catch (Throwable e) { // NOSONAR
            // Undiscoverable here, do not fail the build over it
            return true;
        }
    }

    private Class<?> load(Path root, Path classFile) {
        String name = root.relativize(classFile).toString()
                .replace(classFile.getFileSystem().getSeparator(), ".")
                .replaceAll("\\.class$", "");
        try {
            return Class.forName(name, false, getClass().getClassLoader());
        } catch (Throwable e) { // NOSONAR
            // Not loadable on this classpath, nothing to check
            return null;
        }
    }

    private void run(Class<?>... testCases) {
        barrier = new CyclicBarrier(2);
        rendezvousReached = false;
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
        assertEquals(2L * testCases.length, summary.getTestsSucceededCount(),
                "Nested test run did not execute all test methods");
    }
}
