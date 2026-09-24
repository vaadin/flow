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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    /**
     * WildFly sorts module options apart from the rest before it builds the
     * server's command line, and the two-token form comes apart in the sorting:
     * measured, every {@code --add-opens} arrived ahead of every value and the
     * JVM refused to start with "--add-opens requires modules to be specified".
     */
    @Test
    void singleToken_foldsAModuleOptionOntoItsValue() {
        assertEquals(
                List.of("-javaagent:a.jar",
                        "--add-opens=java.base/java.net=ALL-UNNAMED",
                        "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED",
                        "-Dport=1"),
                MavenGoalRuntime
                        .singleToken(List.of("-javaagent:a.jar", "--add-opens",
                                "java.base/java.net=ALL-UNNAMED", "--add-opens",
                                "java.base/jdk.internal.loader=ALL-UNNAMED",
                                "-Dport=1")));
    }

    /** One already in the {@code =} form is left exactly as it is. */
    @Test
    void singleToken_leavesAnOptionThatAlreadyCarriesItsValue() {
        List<String> flags = List
                .of("--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", "-Xmx2g");

        assertEquals(flags, MavenGoalRuntime.singleToken(flags));
    }

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

    /**
     * Payara Server's channel is a {@code List<String>}, which Maven splits on
     * commas before the plugin sees it - and the plugin then drops every piece
     * with no {@code =} in it, so a comma does not divide a flag, it deletes
     * most of it. The comma-bearing flags go to an argument file, which the JVM
     * expands itself, and the rest stay inline where the launch line shows
     * them.
     */
    @Test
    void withCommasInArgFile_onlyTheCommaBearingFlagsAreMoved(@TempDir Path dir)
            throws IOException {
        Path file = dir.resolve("payara-args.txt");

        List<String> passed = MavenGoalRuntime
                .withCommasInArgFile(List.of("-javaagent:/ha.jar",
                        "-DdisabledPlugins=Vaadin,Spring,SpringBoot,Jetty",
                        "-XX:+AllowEnhancedClassRedefinition"), file);

        assertEquals(
                List.of("-javaagent:/ha.jar",
                        "-XX:+AllowEnhancedClassRedefinition", "@" + file),
                passed);
        assertEquals("\"-DdisabledPlugins=Vaadin,Spring,SpringBoot,Jetty\"\n",
                Files.readString(file));
    }

    /**
     * Liberty's channel carries one flag per property, each becoming one line
     * of the server's generated jvm.options - so a line holding all of them
     * would reach the JVM as a single argument. The key is the flag's position,
     * which only has to make the names distinct.
     */
    @Test
    void flagProperties_oneSettingPerFlag() {
        assertEquals(List.of("-Dliberty.jvm.devloop0=-javaagent:/ha.jar",
                "-Dliberty.jvm.devloop1="
                        + "-XX:+AllowEnhancedClassRedefinition",
                "-Dliberty.jvm.devloop2=-DdisabledPlugins=Vaadin,Spring"),
                MavenGoalRuntime.flagProperties("liberty.jvm.devloop",
                        List.of("-javaagent:/ha.jar",
                                "-XX:+AllowEnhancedClassRedefinition",
                                "-DdisabledPlugins=Vaadin,Spring")));
    }

    /**
     * And nothing is written when nothing needs it, so a channel of this shape
     * costs a launch with no comma in it neither a file nor a token.
     */
    @Test
    void withCommasInArgFile_noCommaLeavesTheFlagsAndTheDiskAlone(
            @TempDir Path dir) throws IOException {
        Path file = dir.resolve("payara-args.txt");

        assertEquals(NEEDED,
                MavenGoalRuntime.withCommasInArgFile(NEEDED, file));
        assertFalse(Files.exists(file));
    }
}
