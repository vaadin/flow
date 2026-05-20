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

/**
 * Helper class for using window.console. Does not log anything except
 * JavaScript exception traces to console if production mode is enabled.
 * <p>
 * The TypeScript implementation lives at
 * {@code src/main/frontend/internal/client/Console.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class Console {

    private Console() {
    }

    public static void setProductionMode(boolean isProductionMode) {
        NativeConsole.setProductionMode(isProductionMode);
    }

    public static void debug(Object message) {
        NativeConsole.debug(message);
    }

    public static void log(Object message) {
        NativeConsole.log(message);
    }

    public static void warn(Object message) {
        NativeConsole.warn(message);
    }

    public static void error(Object message) {
        NativeConsole.error(message);
    }

    public static void reportStacktrace(Exception exception) {
        NativeConsole.reportStacktrace(exception);
    }
}
