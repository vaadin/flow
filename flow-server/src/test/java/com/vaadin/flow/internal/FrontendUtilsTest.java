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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.FrontendUtils.AnsiColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendUtilsTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void redirectSystemOut() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(
                new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreSystemOut() {
        System.setOut(originalOut);
    }

    @Test
    void console_wrapsMessageWithColorAndReset() {
        FrontendUtils.console(AnsiColor.GREEN, "hello");

        assertEquals(AnsiColor.GREEN.wrap("hello"),
                capturedOut.toString(StandardCharsets.UTF_8));
    }

    @Test
    void console_preservesNewlinesInMessage() {
        String message = "\nline one\nline two\n";

        FrontendUtils.console(AnsiColor.RED, message);

        assertEquals(AnsiColor.RED.wrap(message),
                capturedOut.toString(StandardCharsets.UTF_8));
    }

    @Test
    void console_messageWithPercentSpecifiers_printedLiterally() {
        String message = "100%s done, %c %n code, literal %";

        FrontendUtils.console(AnsiColor.YELLOW, message);

        assertEquals(AnsiColor.YELLOW.wrap(message),
                capturedOut.toString(StandardCharsets.UTF_8));
    }

    @Test
    void console_appendsResetExactlyOnce() {
        FrontendUtils.console(AnsiColor.BRIGHT_BLUE, "hi");

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        int firstIndex = output.indexOf("[0m");
        int lastIndex = output.lastIndexOf("[0m");

        assertEquals(firstIndex, lastIndex);
        assertEquals(output.length() - "[0m".length(), firstIndex);
    }

    @Test
    void getFrontendFolder_noLegacyFolder_frontendDirNotProbed(
            @TempDir File projectRoot) {
        // Regression test for the Gradle configuration cache: probing
        // src/main/frontend while configuring records a file-system-entry
        // input on a path the build itself creates, which discards the entry on
        // the next build. With no legacy frontend folder the answer is
        // frontendDir either way, so the probe must not happen at all.
        AtomicInteger probes = new AtomicInteger();
        File frontendDir = probeCountingFile(
                new File(projectRoot, "src/main/frontend").getPath(), probes);

        assertEquals(frontendDir,
                FrontendUtils.getFrontendFolder(projectRoot, frontendDir));
        assertEquals(0, probes.get(),
                "getFrontendFolder must not probe the build-created frontend folder when the project has no legacy frontend folder");
    }

    @Test
    void getFrontendFolder_legacyFolderAndNoFrontendDir_legacyReturned(
            @TempDir File projectRoot) {
        File legacy = new File(projectRoot, FrontendUtils.LEGACY_FRONTEND_DIR);
        assertTrue(legacy.mkdirs());
        File frontendDir = new File(projectRoot, "src/main/frontend");

        assertEquals(legacy,
                FrontendUtils.getFrontendFolder(projectRoot, frontendDir));
    }

    @Test
    void getFrontendFolder_legacyFolderAndExistingFrontendDir_frontendDirReturned(
            @TempDir File projectRoot) {
        assertTrue(new File(projectRoot, FrontendUtils.LEGACY_FRONTEND_DIR)
                .mkdirs());
        File frontendDir = new File(projectRoot, "src/main/frontend");
        assertTrue(frontendDir.mkdirs());

        assertEquals(frontendDir,
                FrontendUtils.getFrontendFolder(projectRoot, frontendDir));
    }

    @Test
    void getFrontendFolder_legacyFolderAndCustomFrontendDir_customDirReturned(
            @TempDir File projectRoot) {
        assertTrue(new File(projectRoot, FrontendUtils.LEGACY_FRONTEND_DIR)
                .mkdirs());
        File customFrontendDir = new File(projectRoot, "my-frontend");

        assertEquals(customFrontendDir, FrontendUtils
                .getFrontendFolder(projectRoot, customFrontendDir));
    }

    private static File probeCountingFile(String path, AtomicInteger probes) {
        return new File(path) {
            @Override
            public boolean exists() {
                probes.incrementAndGet();
                return super.exists();
            }
        };
    }
}
