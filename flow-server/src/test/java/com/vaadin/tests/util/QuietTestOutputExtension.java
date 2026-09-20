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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Buffers everything a test method writes to {@code System.out} and
 * {@code System.err} and throws it away when the test passes. Tests that
 * intentionally trigger logging, such as the ones exercising
 * {@code DefaultErrorHandler}, thus no longer flood the build output, while a
 * failing test still prints everything it logged.
 * <p>
 * Registered for all tests through
 * {@code META-INF/services/org.junit.jupiter.api.extension.Extension}, so no
 * test needs to opt in.
 */
public class QuietTestOutputExtension implements InvocationInterceptor {

    private final PrintStream originalOut;
    private final PrintStream originalErr;

    public QuietTestOutputExtension() {
        // JUnit instantiates the extension before any test has had a chance to
        // replace the streams
        this(System.out, System.err);
    }

    QuietTestOutputExtension(PrintStream originalOut, PrintStream originalErr) {
        this.originalOut = originalOut;
        this.originalErr = originalErr;
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        interceptQuietly(invocation);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        interceptQuietly(invocation);
    }

    private void interceptQuietly(Invocation<Void> invocation)
            throws Throwable {
        PrintStream previousOut = System.out;
        PrintStream previousErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream capturingStream = new PrintStream(captured, true,
                StandardCharsets.UTF_8);
        // Tests that install a stream of their own, for example to assert on
        // what was printed, are left alone
        boolean captureOut = previousOut == originalOut;
        boolean captureErr = previousErr == originalErr;
        if (captureOut) {
            System.setOut(capturingStream);
        }
        if (captureErr) {
            System.setErr(capturingStream);
        }
        try {
            invocation.proceed();
        } catch (Throwable testFailure) {
            capturingStream.flush();
            originalErr.print(captured.toString(StandardCharsets.UTF_8));
            throw testFailure;
        } finally {
            if (captureOut) {
                System.setOut(previousOut);
            }
            if (captureErr) {
                System.setErr(previousErr);
            }
        }
    }
}
