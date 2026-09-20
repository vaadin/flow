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
package com.vaadin.tests.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.slf4j.LoggerFactory;

class QuietTestOutputExtensionTest {

    private final PrintStream systemErr = System.err;
    private final ByteArrayOutputStream buildOutput = new ByteArrayOutputStream();
    private final PrintStream buildOutputStream = new PrintStream(buildOutput,
            true, StandardCharsets.UTF_8);
    private final QuietTestOutputExtension extension = new QuietTestOutputExtension(
            System.out, buildOutputStream);

    @AfterEach
    void restoreSystemErr() {
        System.setErr(systemErr);
    }

    @Test
    void passingTest_loggedErrorNotInBuildOutput() throws Throwable {
        System.setErr(buildOutputStream);

        intercept(QuietTestOutputExtensionTest::logExpectedError);

        Assertions.assertEquals("", buildOutput(),
                "a passing test should not log anything to the build output");
    }

    @Test
    void failingTest_loggedErrorInBuildOutput() {
        System.setErr(buildOutputStream);
        AssertionError failure = new AssertionError("test failed");

        AssertionError thrown = Assertions.assertThrows(AssertionError.class,
                () -> intercept(() -> {
                    logExpectedError();
                    throw failure;
                }));

        Assertions.assertSame(failure, thrown,
                "the original failure should be rethrown");
        Assertions.assertTrue(buildOutput().contains("Unexpected error"),
                "a failing test should log to the build output, but output was: "
                        + buildOutput());
    }

    @Test
    void testCapturingSystemErrItself_captureNotStolen() throws Throwable {
        ByteArrayOutputStream capturedByTest = new ByteArrayOutputStream();
        System.setErr(
                new PrintStream(capturedByTest, true, StandardCharsets.UTF_8));

        intercept(QuietTestOutputExtensionTest::logExpectedError);

        Assertions.assertTrue(
                capturedByTest.toString(StandardCharsets.UTF_8)
                        .contains("Unexpected error"),
                "a test that installs its own stream should still receive the output");
        Assertions.assertEquals("", buildOutput(),
                "nothing should leak to the build output");
    }

    private String buildOutput() {
        buildOutputStream.flush();
        return buildOutput.toString(StandardCharsets.UTF_8);
    }

    private void intercept(ThrowingRunnable testBody) throws Throwable {
        extension.interceptTestMethod(invocationOf(testBody), null, null);
    }

    private static void logExpectedError() {
        LoggerFactory.getLogger(QuietTestOutputExtensionTest.class).error(
                "Unexpected error: {}", (Object) null,
                new IllegalStateException());
    }

    private static Invocation<Void> invocationOf(ThrowingRunnable testBody) {
        return () -> {
            testBody.run();
            return null;
        };
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
