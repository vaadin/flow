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

import jsinterop.annotations.JsType;

/**
 * Helper class for using window.console. Pure {@code @JsType(isNative=true)}
 * binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/Console.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "Console")
public final class Console {

    private Console() {
        // Native, not instantiated from Java
    }

    public static native void setProductionMode(boolean isProductionMode);

    public static native void debug(Object message);

    public static native void log(Object message);

    public static native void warn(Object message);

    public static native void error(Object message);

    public static native void reportStacktrace(Object exception);
}
