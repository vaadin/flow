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

import com.vaadin.client.flow.collection.JsArray;

/**
 * JsInterop binding for the TypeScript {@code Profiler} implementation
 * published at {@code window.Vaadin.Flow.internal.client.Profiler}. Source
 * lives in {@code src/main/frontend/internal/client/Profiler.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "Profiler")
final class NativeProfiler {

    private NativeProfiler() {
        // Native, not instantiated from Java
    }

    static native void logGwtEvent(String evtGroup, String moduleName,
            String name, String type, double relativeMillis);

    static native double getPerformanceTiming(String name);

    static native JsArray<?> getGwtStatsEvents();

    static native void ensureLogger();

    static native void ensureNoLogger();

    static native JsArray<?> clearEventsList();

    static native boolean hasHighPrecisionTime();

    static native double defaultRelativeTime();

    static native double highResolutionRelativeTime();

    static native double round(double num, int exp);
}
