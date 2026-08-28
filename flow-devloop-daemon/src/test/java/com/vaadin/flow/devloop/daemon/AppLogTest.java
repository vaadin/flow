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
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The app's log holds the only copy of why a launch failed - "Port 8080 was
 * already in use" is printed by the application and is not observable from the
 * daemon's side of the process boundary.
 */
class AppLogTest {

    @TempDir
    private Path dir;

    @Test
    void serving_matchesTheLineAPortClashNeverReaches() {
        assertTrue(AppLog.serving(
                "Tomcat started on port 8080 (http) with context path '/'"));
        assertTrue(AppLog
                .serving("Started Application in 4.216 seconds (process ...)"));
        assertFalse(AppLog.serving("Starting Application using Java 21"));
    }

    @Test
    void cause_prefersSpringBootsOwnDiagnosis() throws IOException {
        Path log = log("""
                ***************************
                APPLICATION FAILED TO START
                ***************************

                Description:

                Web server failed to start. Port 8080 was already in use.

                Action:

                Identify and stop the process that's listening on port 8080.
                """);

        assertEquals(
                "Web server failed to start. Port 8080 was already in use.",
                AppLog.cause(log).orElseThrow());
    }

    @Test
    void cause_picksTheDeepestCausedBy() throws IOException {
        // Spring wraps the real mistake in two bean-creation failures, and only
        // the innermost line names it.
        Path log = log(
                """
                        ERROR o.s.boot.SpringApplication - Application run failed
                        org.springframework.beans.factory.BeanCreationException: Error creating bean
                        Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: nope
                        Caused by: java.lang.IllegalStateException: No property 'findOne' found for type 'Task'
                        \tat com.example.Foo.bar(Foo.java:12)
                        """);

        assertEquals(
                "java.lang.IllegalStateException: No property 'findOne' found for type 'Task'",
                AppLog.cause(log).orElseThrow());
    }

    @Test
    void excerpt_startsAtTheFailureReportRatherThanTheLogTail()
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("INFO starting\n");
        sb.append("ERROR the reason that matters\n");
        sb.append("java.lang.IllegalStateException: boom\n");
        for (int i = 0; i < 40; i++) {
            sb.append("\tat com.example.Frame").append(i).append(".run()\n");
        }
        Path log = log(sb.toString());

        List<String> excerpt = AppLog.excerpt(log);

        assertEquals("ERROR the reason that matters", excerpt.get(0));
    }

    @Test
    void cause_emptyLog_isEmpty() throws IOException {
        assertEquals(Optional.empty(), AppLog.cause(log("")));
    }

    @Test
    void watch_recordsErrorsAndKeepsTheThrowableWithItsMessage()
            throws IOException {
        Path log = log("INFO up\n");
        AppLog.Watch watch = new AppLog.Watch(log);
        watch.drain();

        append(log, """
                ERROR c.e.Foo - There was an exception while trying to navigate
                java.lang.AbstractMethodError: com.example.Foo.bar()
                \tat com.example.Foo.bar(Foo.java:12)
                """);

        List<String> errors = watch.errors();

        assertEquals(1, errors.size());
        // One failure, reported as one error: the logged line plus the
        // throwable header that names the type.
        assertTrue(errors.get(0).contains("There was an exception"));
        assertTrue(errors.get(0).contains("AbstractMethodError"));
    }

    @Test
    void watch_linkageError_isAReloadFailure() throws IOException {
        Path log = log("");
        AppLog.Watch watch = new AppLog.Watch(log);

        append(log, """
                ERROR c.e.Foo - request failed
                java.lang.NoSuchMethodError: com.example.Foo.bar()
                """);

        assertTrue(watch.failure().isPresent());
    }

    @Test
    void watch_applicationsOwnError_isRecordedButNotAFailure()
            throws IOException {
        // An app is free to log an error of its own; escalating to a restart on
        // account of one would be a worse answer than the truth.
        Path log = log("");
        AppLog.Watch watch = new AppLog.Watch(log);

        append(log, "ERROR c.e.Foo - could not send the email\n");

        assertEquals(1, watch.errors().size());
        assertTrue(watch.failure().isEmpty());
    }

    @Test
    void watch_viteCompileError_isFoundDespiteBeingLoggedAtInfo()
            throws IOException {
        Path log = log("INFO up\n");
        AppLog.Watch watch = new AppLog.Watch(log);
        watch.drain();

        // Flow pipes Vite's output through DevServerOutputTracker at INFO, so
        // nothing about the level says this is a failure - and the detail line
        // does not help either, because [PARSE_ERROR] has no word boundary
        // before ERROR. Verbatim from a real run, minus the timestamps.
        append(log,
                """
                        INFO c.v.b.d.DevServerOutputTracker : [vite] Internal server error: Transform failed with 1 error:
                        INFO c.v.b.d.DevServerOutputTracker : [PARSE_ERROR] Expected `}` but found `EOF`
                        INFO c.v.b.d.DevServerOutputTracker :    ╰─[ src/main/frontend/greeting.ts:1:55 ]
                        INFO c.v.b.d.DevServerOutputTracker :   Plugin: vite:oxc
                        INFO c.v.b.d.DevServerOutputTracker :       at transformWithOxc (file:///node_modules/vite/dist/node.js:1:1)
                        """);

        List<String> errors = watch.errors();

        // One broken file is one error, not one per line of the report: the
        // excerpt, the caret diagram and the JavaScript stack are all detail.
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Internal server error"));
    }

    @Test
    void watch_viteProgressChatter_isNotAnError() throws IOException {
        Path log = log("INFO up\n");
        AppLog.Watch watch = new AppLog.Watch(log);
        watch.drain();

        // Every one of these is INFO and routine; a dev server that prints on
        // every keystroke must not make apply cry wolf.
        append(log,
                """
                        INFO c.v.b.d.DevServerOutputTracker :   VITE v8.2.2  ready in 452 ms
                        INFO c.v.b.d.DevServerOutputTracker : [vite] page reload src/main/frontend/greeting.ts
                        INFO c.v.b.d.DevServerOutputTracker :   ➜  Local:   http://127.0.0.1:49401/VAADIN/
                        """);

        assertEquals(List.of(), watch.errors());
    }

    @Test
    void watch_mark_dropsWhatCameBeforeTheChange() throws IOException {
        Path log = log("ERROR c.e.Foo - from a previous request\n");
        AppLog.Watch watch = new AppLog.Watch(log);

        watch.mark();

        assertTrue(watch.errors().isEmpty());
    }

    private Path log(String content) throws IOException {
        Path file = dir.resolve("app.log");
        Files.writeString(file, content);
        return file;
    }

    private void append(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardOpenOption.APPEND);
    }
}
