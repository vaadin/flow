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

import com.vaadin.client.bootstrap.ErrorMessage;

/**
 * Adapter handed to the TypeScript {@code SystemErrorHandler} so it can reach
 * back into still-Java services (registry, application configuration,
 * heartbeat, message handling) without depending on the {@link Registry}
 * facade. Constructed in {@link DefaultRegistry} where the wiring lives.
 */
@JsType
public interface SystemErrorHandlerCallbacks {

    String getServiceUrl();

    boolean isWebComponentMode();

    boolean isProductionMode();

    ErrorMessage getSessionExpiredError();

    String[] getExportedWebComponents();

    int getHeartbeatInterval();

    void setHeartbeatInterval(int seconds);

    boolean isPushEnabled();

    void setPushEnabled(boolean enabled);

    /**
     * Disables push immediately, equivalent to the original
     * {@code messageSender.setPushEnabled(false, false)} call which stops the
     * connection synchronously rather than queuing the transition.
     */
    void disablePushImmediately();

    /**
     * Handles the resync response body in one call: parses the JSON, applies
     * any UI ID change, resets the registry, switches the UI lifecycle to
     * {@code RUNNING}, and feeds the message through
     * {@code MessageHandler.handleMessage}.
     */
    void applyResyncResponse(String responseText);
}
