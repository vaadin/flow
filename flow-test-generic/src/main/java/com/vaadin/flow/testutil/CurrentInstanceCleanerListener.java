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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/**
 * JUnit Platform equivalent of {@link CurrentInstanceCleaner}. Removes any
 * CurrentInstance thread locals before running a test, preventing state leakage
 * between tests.
 * <p>
 * Registered automatically via ServiceLoader in
 * {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}.
 */
public class CurrentInstanceCleanerListener implements TestExecutionListener {
    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            // Clear current instances before each test so a previous test does
            // not affect the test
            try {
                Class<?> cls = Class
                        .forName("com.vaadin.flow.internal.CurrentInstance");
                // A method handle rather than getMethod, which resolves every
                // method of the class and with them classes that are not on
                // the test classpath of every module, such as the servlet API
                MethodHandles.publicLookup()
                        .findStatic(cls, "clearAll",
                                MethodType.methodType(void.class))
                        .invokeExact();
            } catch (Throwable e) { // NOSONAR
                // Not a Flow module
            }
        }
    }
}
