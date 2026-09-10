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
                        INFO c.v.b.d.DevServerOutputTracker :    ╰─[ src/main/frontend/greeting.ts?t=1756384057:1:55 ]
                        INFO c.v.b.d.DevServerOutputTracker :    |
                        INFO c.v.b.d.DevServerOutputTracker :  1 | export function greeting(): string { return 'x'
                        INFO c.v.b.d.DevServerOutputTracker :    |                                    ^
                        INFO c.v.b.d.DevServerOutputTracker :       at transformWithOxc (file:///node_modules/vite/dist/node.js:1:1)
                        """);

        List<String> errors = watch.errors();

        // One broken file is one error, not one per line of the report: the
        // excerpt, the caret diagram and the JavaScript stack are all detail.
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Internal server error"));
        // "Transform failed with 1 error" says something broke but not what.
        // The line that names the syntax problem sits past a blank one, and
        // carrying it is what makes the summary actionable on its own.
        assertTrue(errors.get(0).contains("Expected `}` but found `EOF`"),
                errors.get(0));
        // And for a message like "Unexpected token" the position is the half
        // that says which file to open, so it is carried too - without the
        // ?t= cache-buster Vite hangs off every module it has hot-updated.
        assertTrue(errors.get(0).contains("greeting.ts:1:55"), errors.get(0));
        assertFalse(errors.get(0).contains("?t="), errors.get(0));
        // The source excerpt stays out. It is the developer's own code, which
        // they have in front of them, and a caret diagram cannot line up
        // inside an indented, wrapped summary anyway - the position above is
        // what they need. Its ASCII-fallback gutter is also a literal pipe,
        // which is why the parts are not joined with one.
        assertFalse(errors.get(0).contains("export function greeting"),
                errors.get(0));
        assertFalse(errors.get(0).contains("transformWithOxc"), errors.get(0));
    }

    @Test
    void report_keepsTheDiagnosisAndDropsTheDrawing() {
        // The whole message, as the dev server hands it back when asked
        // directly rather than as lines arriving in the log. Verbatim from a
        // real run.
        List<String> lines = List.of("Transform failed with 1 error:", "",
                "[PARSE_ERROR] Expected `}` but found `EOF`",
                "   \u256d\u2500[ src/main/frontend/greeting.ts?t=1756384057:1:55 ]",
                "   \u2502",
                " 1 \u2502 export function greeting(): string { return 'x'",
                "   \u2502                                    ^",
                "    at transformWithOxc (file:///node_modules/vite/dist/node.js:1:1)");

        String report = AppLog.report(lines);

        // Three parts, each its own segment: what broke, why, and where. The
        // excerpt, the caret row and the stack are the developer's own code
        // and the log's business.
        assertEquals(
                List.of("Transform failed with 1 error:",
                        "[PARSE_ERROR] Expected `}` but found `EOF`",
                        "src/main/frontend/greeting.ts:1:55"),
                List.of(report.split(AppLog.SEGMENT)));
    }

    @Test
    void report_leavesAOneLineFailureWhole() {
        // Babel puts the file, the reason and the position on a single line,
        // and that line names a path - so nothing may be mistaken for a
        // source location worth splitting off, and nothing may be dropped.
        String report = AppLog.report(List.of(
                "[BabelError] C:\\project\\src\\main\\frontend\\views\\@index.tsx:"
                        + " Unterminated string constant. (18:20)"));

        assertEquals(
                "[BabelError] C:\\project\\src\\main\\frontend\\views\\@index.tsx:"
                        + " Unterminated string constant. (18:20)",
                report);
    }

    @Test
    void message_stripsTheLayoutBoilerplateAndNothingElse() {
        // Roughly a hundred characters of prefix, which is most of the budget a
        // one-line summary has to work with.
        assertEquals("[vite] Internal server error: Transform failed",
                AppLog.message(
                        "2026-08-28T14:32:37.265+03:00  INFO 41096 --- [v-server-output]"
                                + " c.v.b.devserver.DevServerOutputTracker   :"
                                + " 14.32.37 [vite] Internal server error: Transform failed"));
        // Plain Logback's layout too.
        assertEquals("could not send the email", AppLog.message(
                "14:32:37.265 [main] ERROR c.e.Foo - could not send the email"));
        // A line with no prefix - a bare stack-trace header - is left alone.
        assertEquals("java.lang.IllegalStateException: boom",
                AppLog.message("java.lang.IllegalStateException: boom"));
    }

    @Test
    void watch_checkerTypeError_isFoundAndKeptToOneError() throws IOException {
        Path log = log("INFO up\n");
        AppLog.Watch watch = new AppLog.Watch(log);
        watch.drain();

        // A type error the transform cannot see: oxc strips the types without
        // checking them, so the module is served with a 200 and only the
        // checker knows. Verbatim from a real run, minus the timestamps.
        append(log,
                """
                        INFO c.v.b.d.DevServerOutputTracker :  ERROR(TypeScript)  TS1382: Unexpected token. Did you mean `{'>'}` or `&gt;`?
                        INFO c.v.b.d.DevServerOutputTracker :  FILE  C:\\project\\src\\main\\frontend\\stray.tsx:4:7
                        INFO c.v.b.d.DevServerOutputTracker :
                        INFO c.v.b.d.DevServerOutputTracker :     2 |   return (
                        INFO c.v.b.d.DevServerOutputTracker :   > 4 |       >
                        INFO c.v.b.d.DevServerOutputTracker :       |       ^
                        INFO c.v.b.d.DevServerOutputTracker : [TypeScript] Found 1 error(s)
                        """);

        List<String> errors = watch.errors();

        // One error, not two: the "Found 1 error(s)" line comes after the
        // report and would double-count it - and reads "Found 0 error(s)" on a
        // clean run.
        assertEquals(1, errors.size());
        assertTrue(AppLog.checkerError(errors.get(0)), errors.get(0));
        // A transform error it is not, which is the distinction a clean fetch
        // turns on: fetching this module comes back 200.
        assertFalse(AppLog.devServerError(errors.get(0)), errors.get(0));
        // The position is carried the same way, so the reader is told which
        // file to open; the excerpt with its "> 4 |" gutter is not.
        assertTrue(errors.get(0).contains("stray.tsx:4:7"), errors.get(0));
        assertFalse(errors.get(0).contains("return ("), errors.get(0));
    }

    @Test
    void watch_checkerVerdict_isSupersededAndSurvivesAMark()
            throws IOException {
        Path log = log("INFO up\n");
        AppLog.Watch watch = new AppLog.Watch(log);
        watch.drain();
        append(log, "INFO t :  ERROR(TypeScript)  TS2322: Type 'number' is"
                + " not assignable to type 'string'.\n");

        assertTrue(watch.checkerFailure().isPresent());

        // An apply in between: the verdict is the state of the project, not an
        // event in one window, so it has to outlive the boundary - the checker
        // re-announces only when something changes.
        watch.mark();
        assertTrue(watch.checkerFailure().isPresent());

        // Put the file back and save: the checker says so, and that supersedes
        // the report still sitting in the log above it.
        append(log, "INFO t : [TypeScript] No errors\n");

        assertTrue(watch.checkerFailure().isEmpty());
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

    @Test
    void watch_mark_alsoEndsTheDetailScanTheWindowLeftOpen()
            throws IOException {
        Path log = log("");
        AppLog.Watch watch = new AppLog.Watch(log);

        // A dev-server error arms a scan for the lines under it, because the
        // one that names the syntax problem arrives a moment later. Late
        // enough, and the apply that read the error has already ended its
        // window.
        append(log, "INFO c.v.b.d.DevServerOutputTracker :"
                + " [vite] Internal server error: Transform failed with 1 error:\n");
        assertEquals(1, watch.errors().size());

        watch.mark();

        append(log, "INFO c.v.b.d.DevServerOutputTracker :"
                + " [PARSE_ERROR] Expected `}` but found `EOF`\n");

        // The error that detail belonged to is not in this window any more, so
        // there is nothing to attach it to. Attaching it anyway is what read
        // errors.get(-1) and threw, which apply reported as an internal
        // failure over a change that was fine.
        assertEquals(List.of(), watch.errors());
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
