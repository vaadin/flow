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
package com.vaadin.flow.internal;

/**
 * Helpers for finding the application code in a stack trace.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class StackTraceUtil {

    private static final String[] NON_APPLICATION_PREFIXES = {
            "com.vaadin.flow.component.", "com.vaadin.flow.di.",
            "com.vaadin.flow.dom.", "com.vaadin.flow.internal.",
            "com.vaadin.flow.spring.", "com.vaadin.cdi.", "com.vaadin.quarkus.",
            "java.", "jdk.", "org.springframework.beans.", "org.jboss.weld.",
            "io.quarkus.arc.", };

    private StackTraceUtil() {
        // Only static helpers
    }

    /**
     * Checks whether the given class is application code rather than one of the
     * framework, JDK or dependency injection container classes that sit between
     * application code and the API it calls. A stack trace points at the code
     * that made a call through its first frame of an application class.
     * <p>
     * Only the framework packages that application code calls directly are
     * excluded, so framework code that runs on its own, such as the router,
     * counts as application code.
     *
     * @param className
     *            the fully qualified name of the class, not <code>null</code>
     * @return <code>true</code> if the class is application code,
     *         <code>false</code> otherwise
     */
    public static boolean isApplicationClass(String className) {
        for (String prefix : NON_APPLICATION_PREFIXES) {
            if (className.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }
}
