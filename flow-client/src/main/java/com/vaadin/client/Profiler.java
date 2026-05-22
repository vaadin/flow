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
 * Lightweight profiling tool. Pure {@code @JsType(isNative=true)} binding to
 * the TypeScript implementation at
 * {@code src/main/frontend/internal/client/Profiler.ts}.
 *
 * <p>
 * Profiling collection is unconditionally disabled in the migrated runtime (the
 * original GWT {@code vaadin.profiler} compile flag and the
 * {@code __gwtStatsEvent} aggregation stream are gone). {@link #isEnabled()}
 * always returns {@code false}; the {@code enter}/{@code leave}/{@code reset}
 * entry points are no-ops, kept so existing call sites continue to link. The
 * time helpers ({@link #getRelativeTimeMillis()} and
 * {@link #getRelativeTimeString(double)}) wrap {@code performance.now()} and
 * remain useful for ad-hoc latency logs.
 *
 * @author Vaadin Ltd
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "Profiler")
public final class Profiler {

    private Profiler() {
        // Native, not instantiated from Java
    }

    public static native boolean isEnabled();

    public static native void enter(String name);

    public static native void leave(String name);

    public static native double getRelativeTimeMillis();

    public static native String getRelativeTimeString(double reference);

    public static native void reset();

    public static native void initialize();

    public static native void logTimings();

    public static native void logBootstrapTimings();
}
