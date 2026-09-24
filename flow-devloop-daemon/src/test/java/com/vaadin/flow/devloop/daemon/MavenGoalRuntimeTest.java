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
package com.vaadin.flow.devloop.daemon;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Under a build-plugin runtime the application JVM is Maven's own, so
 * {@code MAVEN_OPTS} is the only way in - and it is also where the developer's
 * project already keeps the flags its build needs. What goes in it is therefore
 * worth pinning down: the launch itself needs a real project and is covered by
 * {@code flow-tests/test-devloop}.
 */
class MavenGoalRuntimeTest {

    private static final List<String> NEEDED = List.of("-javaagent:/ha.jar",
            "-XX:+AllowEnhancedClassRedefinition");

    @Test
    void mavenOpts_inheritedValueIsKept() {
        String opts = MavenGoalRuntime.mavenOpts("-Xmx2g -Dhttps.proxyPort=80",
                NEEDED);

        assertTrue(opts.contains("-Xmx2g"), opts);
        assertTrue(opts.contains("-Dhttps.proxyPort=80"), opts);
        assertTrue(opts.contains("-javaagent:/ha.jar"), opts);
    }

    /**
     * The JVM lets the later of two conflicting flags win, so the loop's own
     * have to come last: a project may set a heap size, but it must not be able
     * to turn off the redefinition the loop depends on.
     */
    @Test
    void mavenOpts_theLoopsFlagsComeLast() {
        String opts = MavenGoalRuntime.mavenOpts("-Xmx2g", NEEDED);

        assertEquals("-Xmx2g -javaagent:/ha.jar "
                + "-XX:+AllowEnhancedClassRedefinition", opts);
    }

    @Test
    void mavenOpts_nothingInherited_isJustTheLoopsFlags() {
        String expected = "-javaagent:/ha.jar "
                + "-XX:+AllowEnhancedClassRedefinition";

        assertEquals(expected, MavenGoalRuntime.mavenOpts(null, NEEDED));
        assertEquals(expected, MavenGoalRuntime.mavenOpts("   ", NEEDED));
    }

    /**
     * Maven expands {@code $MAVEN_OPTS} unquoted, so a flag with a space in it
     * reaches the JVM as two broken arguments. Nothing can fix that, so it is
     * named at launch rather than left to fail as a JVM that will not start.
     */
    @Test
    void unsplittable_namesTheFlagWithASpaceAndNoOther() {
        List<String> warnings = MavenGoalRuntime.unsplittable(List.of(
                "-javaagent:/opt/ha.jar", "-javaagent:/Program Files/ha.jar"));

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("/Program Files/ha.jar"),
                warnings.get(0));
        assertTrue(warnings.get(0).contains("MAVEN_OPTS"), warnings.get(0));
    }

    @Test
    void unsplittable_saysNothingWhenEveryFlagSurvives() {
        assertEquals(List.of(), MavenGoalRuntime.unsplittable(NEEDED));
    }
}
