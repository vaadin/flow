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
package com.vaadin.flow.server.auth;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Captures the log output produced during an action. slf4j-simple writes log
 * output to System.err.
 */
final class LogOutputCapture {

    private LogOutputCapture() {
    }

    static String capture(Runnable action) {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(baos);
        System.setErr(captureStream);
        try {
            action.run();
        } finally {
            System.setErr(originalErr);
        }
        return baos.toString();
    }
}
