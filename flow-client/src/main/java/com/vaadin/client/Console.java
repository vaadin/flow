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
package com.vaadin.client;

import com.google.gwt.core.client.GWT;

/**
 * Helper class for using window.console. Does not log anything except
 * JavaScript exception traces to console if production mode is enabled.
 * <p>
 * Under GWT the calls are forwarded to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/Console.ts}. Direct JVM usage (in
 * unit tests) falls back to {@code System.out}/{@code System.err}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class Console {

    private Console() {
    }

    public static void setProductionMode(boolean isProductionMode) {
        if (GWT.isScript()) {
            NativeConsole.setProductionMode(isProductionMode);
        }
    }

    public static void debug(Object message) {
        if (GWT.isScript()) {
            NativeConsole.debug(message);
        } else {
            System.out.println(message);
        }
    }

    public static void log(Object message) {
        if (GWT.isScript()) {
            NativeConsole.log(message);
        } else {
            System.out.println(message);
        }
    }

    public static void warn(Object message) {
        if (GWT.isScript()) {
            NativeConsole.warn(message);
        } else {
            System.err.println(message);
        }
    }

    public static void error(Object message) {
        if (GWT.isScript()) {
            NativeConsole.error(message);
        } else {
            System.err.println(message);
        }
    }

    public static void reportStacktrace(Exception exception) {
        if (GWT.isScript()) {
            NativeConsole.reportStacktrace(exception);
        } else {
            exception.printStackTrace();
        }
    }
}
