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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestIdentifier;

class QuietTestOutputListenerTest {

    private final PrintStream systemOut = System.out;
    private final PrintStream systemErr = System.err;
    private final ByteArrayOutputStream buildOutput = new ByteArrayOutputStream();

    private QuietTestOutputListener listener;

    @BeforeEach
    void setUp() {
        System.setProperty(QuietTestOutputListener.ENABLED_PROPERTY, "true");
        // The listener remembers the stream in use when it is created
        System.setErr(
                new PrintStream(buildOutput, true, StandardCharsets.UTF_8));
        listener = new QuietTestOutputListener();
    }

    @AfterEach
    void tearDown() {
        System.setOut(systemOut);
        System.setErr(systemErr);
    }

    @Test
    void passingTest_outputDiscarded() {
        TestIdentifier test = testIdentifier();

        listener.executionStarted(test);
        PrintStream streamInstalledByListener = System.out;
        System.err.println("Unexpected error: null");
        listener.executionFinished(test, TestExecutionResult.successful());

        Assertions.assertEquals("", buildOutput(),
                "output of a passing test should be discarded");
        Assertions.assertNotSame(streamInstalledByListener, System.out,
                "System.out should be restored");
    }

    @Test
    void failingTest_outputPrinted() {
        TestIdentifier test = testIdentifier();

        listener.executionStarted(test);
        System.err.println("Unexpected error: null");
        listener.executionFinished(test,
                TestExecutionResult.failed(new AssertionError("failed")));

        Assertions.assertTrue(buildOutput().contains("Unexpected error: null"),
                "output of a failing test should be printed, but was: "
                        + buildOutput());
    }

    @Test
    void passingTestInsideTestClass_classLevelOutputDiscarded() {
        TestIdentifier testClass = classIdentifier();
        TestIdentifier test = testIdentifier();

        listener.executionStarted(testClass);
        System.out.println("container starting");
        listener.executionStarted(test);
        System.out.println("test running");
        listener.executionFinished(test, TestExecutionResult.successful());
        listener.executionFinished(testClass, TestExecutionResult.successful());

        Assertions.assertEquals("", buildOutput(),
                "output of a passing test class should be discarded");
    }

    @Test
    void failingTestInsidePassingTestClass_testOutputPrinted() {
        TestIdentifier testClass = classIdentifier();
        TestIdentifier test = testIdentifier();

        listener.executionStarted(testClass);
        listener.executionStarted(test);
        System.err.println("test output");
        listener.executionFinished(test,
                TestExecutionResult.failed(new AssertionError("failed")));
        listener.executionFinished(testClass, TestExecutionResult.successful());

        Assertions.assertTrue(buildOutput().contains("test output"),
                "the failing test output should not be swallowed by the test class, but was: "
                        + buildOutput());
    }

    @Test
    void notEnabled_outputLeftAlone() {
        System.clearProperty(QuietTestOutputListener.ENABLED_PROPERTY);
        QuietTestOutputListener disabled = new QuietTestOutputListener();
        TestIdentifier test = testIdentifier();
        PrintStream streamBefore = System.err;

        disabled.executionStarted(test);
        System.err.println("printed by a test");
        disabled.executionFinished(test, TestExecutionResult.successful());

        Assertions.assertSame(streamBefore, System.err,
                "the streams should be left alone when the capture is off");
        Assertions.assertTrue(buildOutput().contains("printed by a test"),
                "output should go to the build output when the capture is off");
    }

    @Test
    void testsStartingOnAnotherThread_captureGivenUp() throws Exception {
        TestIdentifier first = testIdentifier();
        TestIdentifier second = testIdentifier();
        PrintStream streamBeforeCapture = System.err;
        listener.executionStarted(first);

        Thread parallelTest = new Thread(
                () -> listener.executionStarted(second));
        parallelTest.start();
        parallelTest.join();

        Assertions.assertSame(streamBeforeCapture, System.err,
                "the streams should be restored when tests run in parallel");
        System.err.println("printed by a test");
        listener.executionFinished(first, TestExecutionResult.successful());
        Assertions.assertTrue(buildOutput().contains("printed by a test"),
                "output should go to the build output once the capture gave up, but was: "
                        + buildOutput());

        // and stays given up for the rest of the run
        listener.executionStarted(testIdentifier());

        Assertions.assertSame(streamBeforeCapture, System.err,
                "the capture should not start again once it gave up");
    }

    @Test
    void unfinishedTest_streamsRestoredWhenTestPlanEnds() {
        listener.executionStarted(testIdentifier());
        PrintStream streamInstalledByListener = System.out;

        listener.testPlanExecutionFinished(null);

        Assertions.assertNotSame(streamInstalledByListener, System.out,
                "System.out should be restored when the test plan ends");
    }

    private String buildOutput() {
        return buildOutput.toString(StandardCharsets.UTF_8);
    }

    private static int counter = 0;

    private static TestIdentifier testIdentifier() {
        return identifier(TestDescriptor.Type.TEST, null);
    }

    private static TestIdentifier classIdentifier() {
        return identifier(TestDescriptor.Type.CONTAINER,
                ClassSource.from(QuietTestOutputListenerTest.class));
    }

    private static TestIdentifier identifier(TestDescriptor.Type type,
            ClassSource source) {
        UniqueId uniqueId = UniqueId.forEngine("test")
                .append(type.name().toLowerCase(), "id" + counter++);
        return TestIdentifier.from(new AbstractTestDescriptor(uniqueId,
                uniqueId.getLastSegment().getValue(), source) {
            @Override
            public Type getType() {
                return type;
            }
        });
    }
}
