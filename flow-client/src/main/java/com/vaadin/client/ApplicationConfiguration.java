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

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import com.vaadin.client.bootstrap.ErrorMessage;

/**
 * Application configuration data. Pure {@code @JsType(isNative=true)} binding
 * to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/ApplicationConfiguration.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ApplicationConfiguration")
public class ApplicationConfiguration {

    public ApplicationConfiguration() {
        // Defined by the TS class constructor.
    }

    @JsProperty(name = "applicationId")
    public native String getApplicationId();

    @JsProperty(name = "applicationId")
    public native void setApplicationId(String applicationId);

    @JsProperty(name = "serviceUrl")
    public native String getServiceUrl();

    @JsProperty(name = "serviceUrl")
    public native void setServiceUrl(String serviceUrl);

    @JsProperty(name = "contextRootUrl")
    public native String getContextRootUrl();

    @JsProperty(name = "contextRootUrl")
    public native void setContextRootUrl(String contextRootUrl);

    @JsProperty(name = "webComponentMode")
    public native boolean isWebComponentMode();

    @JsProperty(name = "webComponentMode")
    public native void setWebComponentMode(boolean mode);

    @JsProperty(name = "uiId")
    public native int getUIId();

    @JsProperty(name = "uiId")
    public native void setUIId(int uiId);

    @JsProperty(name = "sessionExpiredError")
    public native ErrorMessage getSessionExpiredError();

    @JsProperty(name = "sessionExpiredError")
    public native void setSessionExpiredError(ErrorMessage sessionExpiredError);

    @JsProperty(name = "heartbeatInterval")
    public native int getHeartbeatInterval();

    @JsProperty(name = "heartbeatInterval")
    public native void setHeartbeatInterval(int heartbeatInterval);

    @JsProperty(name = "maxMessageSuspendTimeout")
    public native int getMaxMessageSuspendTimeout();

    @JsProperty(name = "maxMessageSuspendTimeout")
    public native void setMaxMessageSuspendTimeout(
            int maxMessageSuspendTimeout);

    @JsProperty(name = "productionMode")
    public native boolean isProductionMode();

    @JsProperty(name = "productionMode")
    public native void setProductionMode(boolean productionMode);

    @JsProperty(name = "requestTiming")
    public native boolean isRequestTiming();

    @JsProperty(name = "requestTiming")
    public native void setRequestTiming(boolean requestTiming);

    @JsProperty(name = "servletVersion")
    public native String getServletVersion();

    @JsProperty(name = "servletVersion")
    public native void setServletVersion(String servletVersion);

    @JsProperty(name = "atmosphereVersion")
    public native String getAtmosphereVersion();

    @JsProperty(name = "atmosphereVersion")
    public native void setAtmosphereVersion(String atmosphereVersion);

    @JsProperty(name = "atmosphereJSVersion")
    public native String getAtmosphereJSVersion();

    @JsProperty(name = "atmosphereJSVersion")
    public native void setAtmosphereJSVersion(String atmosphereJSVersion);

    @JsProperty(name = "exportedWebComponents")
    public native String[] getExportedWebComponents();

    @JsProperty(name = "exportedWebComponents")
    public native void setExportedWebComponents(String[] exportedWebComponents);

    @JsProperty(name = "devToolsEnabled")
    public native boolean isDevToolsEnabled();

    @JsProperty(name = "devToolsEnabled")
    public native void setDevToolsEnabled(boolean devToolsEnabled);

    @JsProperty(name = "liveReloadUrl")
    public native String getLiveReloadUrl();

    @JsProperty(name = "liveReloadUrl")
    public native void setLiveReloadUrl(String liveReloadUrl);

    @JsProperty(name = "liveReloadBackend")
    public native String getLiveReloadBackend();

    @JsProperty(name = "liveReloadBackend")
    public native void setLiveReloadBackend(String liveReloadBackend);

    @JsProperty(name = "springBootLiveReloadPort")
    public native String getSpringBootLiveReloadPort();

    @JsProperty(name = "springBootLiveReloadPort")
    public native void setSpringBootLiveReloadPort(
            String springBootLiveReloadPort);
}
