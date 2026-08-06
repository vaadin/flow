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
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        FrontendUtils.console(FrontendUtils.GREEN, "hello");

        assertEquals(FrontendUtils.GREEN + "hello" + FrontendUtils.ANSI_RESET,
                capturedOut.toString(StandardCharsets.UTF_8));
    }

    @Test
    void console_preservesNewlinesInMessage() {
        String message = "\nline one\nline two\n";

        FrontendUtils.console(FrontendUtils.RED, message);

        assertEquals(FrontendUtils.RED + message + FrontendUtils.ANSI_RESET,
                capturedOut.toString(StandardCharsets.UTF_8));
    }

    @Test
    void console_messageWithPercentSpecifiers_printedLiterally() {
        String message = "100%s done, %c %n code, literal %";

        FrontendUtils.console(FrontendUtils.YELLOW, message);

        assertEquals(FrontendUtils.YELLOW + message + FrontendUtils.ANSI_RESET,
                capturedOut.toString(StandardCharsets.UTF_8));
    }
}
