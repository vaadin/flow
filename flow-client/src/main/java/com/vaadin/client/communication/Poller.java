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

import com.vaadin.client.UILifecycle;
import com.vaadin.client.flow.StateTree;

/**
 * Polls the server with a given interval. Pure {@code @JsType(isNative=true)}
 * binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/Poller.ts}.
 *
 * <p>
 * Construction takes the {@link StateTree} and {@link UILifecycle} directly
 * rather than the {@code Registry} so the TS class only depends on already
 * TS-migrated surfaces.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "Poller")
public class Poller {

    public Poller(StateTree tree, UILifecycle uiLifecycle) {
        // Defined by the TS class constructor.
    }

    /**
     * Sets the polling interval, in milliseconds. Changing the interval stops
     * any in-progress polling and schedules a fresh repeat timer with the new
     * interval. A negative value disables polling.
     *
     * @param interval
     *            polling interval in milliseconds, or a negative value to
     *            disable polling
     */
    public native void setInterval(int interval);

    /**
     * Polls the server for changes.
     */
    public native void poll();
}
