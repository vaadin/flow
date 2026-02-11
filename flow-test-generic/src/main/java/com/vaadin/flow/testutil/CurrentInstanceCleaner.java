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

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/**
 * Removes any CurrentInstance thread locals before running a test.
 */
public class CurrentInstanceCleaner implements TestExecutionListener {
    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        // Clear current instances before each test so a previous test does not
        // affect the test
        if (testIdentifier.isTest()) {
            try {
                Class<?> cls = Class
                        .forName("com.vaadin.flow.internal.CurrentInstance");
                cls.getMethod("clearAll").invoke(null);
            } catch (Exception e) { // NOSONAR
                // Not a Flow module
            }
        }
    }
}
