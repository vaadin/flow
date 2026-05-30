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
 * Provides a way to query information about the web browser. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/BrowserInfo.ts}. The TS module
 * inlines a small UA-string parser, replacing the previous dependency on
 * {@code BrowserDetails} from {@code flow-shared}.
 *
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "BrowserInfo")
public final class BrowserInfo {

    private BrowserInfo() {
        // Native, not instantiated from Java
    }

    /** Singleton accessor. */
    public static native BrowserInfo get();

    public native boolean isIE();

    public native boolean isEdge();

    public native boolean isFirefox();

    public native boolean isSafari();

    public native boolean isSafariOrIOS();

    public native boolean isChrome();

    public native boolean isGecko();

    public native boolean isWebkit();

    public native boolean isOpera();

    public native boolean isAndroid();

    public native boolean isTouchDevice();

    public native boolean isAndroidWithBrokenScrollTop();

    public native int getBrowserMajorVersion();

    public native int getBrowserMinorVersion();

    public native float getGeckoVersion();

    public native float getWebkitVersion();
}
