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

    private record CapturedOutput(String uniqueId, ByteArrayOutputStream buffer,
            PrintStream previousOut, PrintStream previousErr) {
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (!enabled || !shouldCapture(testIdentifier)) {
            return;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(buffer, true,
                StandardCharsets.UTF_8);
        captured.push(new CapturedOutput(testIdentifier.getUniqueId(), buffer,
                System.out, System.err));
        System.setOut(captureStream);
        System.setErr(captureStream);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier,
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
    public void testPlanExecutionFinished(TestPlan testPlan) {
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
