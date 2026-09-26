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
package com.vaadin.flow.testutil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Buffers everything a test class or a test method writes to {@code System.out}
 * and {@code System.err} and throws it away unless the test fails. Tests that
 * intentionally trigger logging, such as the ones covering error handling, thus
 * no longer flood the build output, while everything a failing test logged is
 * printed as usual.
 * <p>
 * Output logged through {@code java.util.logging} is not captured, since its
 * handlers keep writing to the stream they were created with; configure the
 * logger levels instead when such output is too noisy.
 * <p>
 * Replacing {@code System.out} and {@code System.err} affects the whole JVM, so
 * this only works when tests run one at a time. Surefire runs the unit tests
 * sequentially; do not enable it for tests that run in parallel, such as the
 * TestBench integration tests run by Failsafe, where whichever test happens to
 * be running would capture, and on success discard, the output of the others.
 * Should tests nevertheless start on more than one thread, the capture gives up
 * for the rest of the run instead of mixing up their output.
 * <p>
 * Registered automatically via ServiceLoader in
 * {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}
 * and enabled by the {@value #ENABLED_PROPERTY} system property, which Surefire
 * sets for unit tests. Run with {@code -Dvaadin.test.quietOutput=false} to see
 * the output of passing tests again.
 */
public class QuietTestOutputListener implements TestExecutionListener {

    /**
     * Name of the system property that enables the capture.
     */
    public static final String ENABLED_PROPERTY = "vaadin.test.quietOutput";

    private final boolean enabled = Boolean.getBoolean(ENABLED_PROPERTY);

    /**
     * The stream the build output is read from, i.e. the one in use before any
     * test had a chance to replace it.
     */
    private final PrintStream buildOutput = System.err;

    private final Deque<CapturedOutput> captured = new ArrayDeque<>();

    /**
     * The thread the open captures belong to, used to notice tests running in
     * parallel.
     */
    private Thread capturingThread;

    private boolean givenUp;

    private record CapturedOutput(String uniqueId, ByteArrayOutputStream buffer,
            PrintStream previousOut, PrintStream previousErr) {
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (enabled) {
            // A java.util.logging handler keeps the System.err it was created
            // with, and it is created when java.util.logging is first used.
            // Set it up before the first capture replaces the stream, or
            // everything logged through java.util.logging for the rest of the
            // JVM, the output of failing tests included, would end up in a
            // buffer that has already been thrown away.
            Logger.getLogger("").getHandlers();
        }
    }

    // The callbacks are synchronized so that the parallel detection below is
    // not itself racy
    @Override
    public synchronized void executionStarted(TestIdentifier testIdentifier) {
        if (!enabled || givenUp || !shouldCapture(testIdentifier)) {
            return;
        }
        if (!captured.isEmpty() && capturingThread != Thread.currentThread()) {
            giveUp();
            return;
        }
        capturingThread = Thread.currentThread();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(buffer, true,
                StandardCharsets.UTF_8);
        captured.push(new CapturedOutput(testIdentifier.getUniqueId(), buffer,
                System.out, System.err));
        System.setOut(captureStream);
        System.setErr(captureStream);
    }

    @Override
    public synchronized void executionFinished(TestIdentifier testIdentifier,
            TestExecutionResult testExecutionResult) {
        CapturedOutput capture = captured.peek();
        if (capture == null
                || !capture.uniqueId().equals(testIdentifier.getUniqueId())) {
            return;
        }
        captured.pop();
        restore(capture);
        if (testExecutionResult.getStatus() == Status.FAILED) {
            print(testIdentifier, capture);
        }
    }

    @Override
    public synchronized void testPlanExecutionFinished(TestPlan testPlan) {
        // Nothing should be left, but make sure a test that ended in an
        // unexpected way does not leave the streams captured
        while (!captured.isEmpty()) {
            restore(captured.pop());
        }
    }

    /**
     * Captures test methods and test classes, the latter so that output from
     * {@code @BeforeAll} methods and from extensions setting up the class, such
     * as a CDI container, is included.
     */
    private boolean shouldCapture(TestIdentifier testIdentifier) {
        return testIdentifier.isTest() || testIdentifier.getSource()
                .filter(ClassSource.class::isInstance).isPresent();
    }

    /**
     * Stops capturing for good once tests turn out to run in parallel, since
     * replacing the streams is a JVM wide operation that cannot be shared.
     * Losing the output of a test to a buffer belonging to another one would be
     * worse than a noisy build log.
     */
    private void giveUp() {
        givenUp = true;
        while (!captured.isEmpty()) {
            restore(captured.pop());
        }
        buildOutput.println(getClass().getSimpleName()
                + ": tests are running in parallel, printing all test output");
    }

    private void restore(CapturedOutput capture) {
        System.setOut(capture.previousOut());
        System.setErr(capture.previousErr());
    }

    private void print(TestIdentifier testIdentifier, CapturedOutput capture) {
        String output = capture.buffer().toString(StandardCharsets.UTF_8);
        if (!output.isEmpty()) {
            buildOutput.println("Output of failed "
                    + testIdentifier.getDisplayName() + ":");
            buildOutput.print(output);
        }
    }
}
