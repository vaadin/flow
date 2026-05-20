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
package com.vaadin.client.communication;

import jsinterop.annotations.JsType;

import com.vaadin.client.JsRunnable;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsMap;

/**
 * Provides the push configuration stored in the root node with an easier to use
 * API. Pure {@code @JsType(isNative=true)} binding to the TypeScript
 * implementation at
 * {@code src/main/frontend/internal/client/communication/PushConfiguration.ts}.
 *
 * <p>
 * Construction takes the {@link StateTree} and a pair of enable-push /
 * disable-push callbacks (each dispatches into
 * {@link MessageSender#setPushEnabled(boolean)}) so the TS class does not have
 * to dispatch back through the Java {@code Registry} facade.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "PushConfiguration")
public class PushConfiguration {

    public PushConfiguration(StateTree tree, JsRunnable enablePush,
            JsRunnable disablePush) {
        // Defined by the TS class constructor.
    }

    /**
     * Gets the push servlet mapping configured or determined on the server.
     */
    public native String getPushServletMapping();

    /**
     * Checks if XHR should be used for client-&gt;server messages even when we
     * are using a bidirectional push transport such as websockets.
     */
    public native boolean isAlwaysXhrToServer();

    /**
     * Gets all configured push parameters as a map.
     */
    public native JsMap<String, String> getParameters();

    /**
     * Checks if push is enabled.
     */
    public native boolean isPushEnabled();
}
