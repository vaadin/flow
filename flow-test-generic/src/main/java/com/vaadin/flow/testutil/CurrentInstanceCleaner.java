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
package com.vaadin.flow.testutil;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/**
 * Removes any CurrentInstance thread locals before running a test.
 */
public class CurrentInstanceCleaner extends RunListener {
    @Override
    public void testStarted(Description description) throws Exception {
        // Clear current instances before each test so a previous test does not
        // affect the test
        try {
            Class<?> cls = Class
                    .forName("com.vaadin.flow.internal.CurrentInstance");
            cls.getMethod("clearAll").invoke(null);
        } catch (Exception e) { // NOSONAR
            // Not a Flow module
        }
    }
}
