/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server;

import java.io.Serializable;

import com.vaadin.shared.ApplicationConstants;

/**
 * Constants used by the server side framework.
 *
 * @since 6.2
 *
 */
public final class Constants implements Serializable {

    // Keep the version number in sync with push/build.xml and other locations
    // listed in that file
    public static final String REQUIRED_ATMOSPHERE_RUNTIME_VERSION =
            "2.4.5.vaadin2";

    public static final String SERVLET_PARAMETER_PRODUCTION_MODE = "productionMode";
    // Javadocs for VaadinService should be updated if this value is changed
    public static final String SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION = "disable-xsrf-protection";
    public static final String SERVLET_PARAMETER_RESOURCE_CACHE_TIME = "resourceCacheTime";
    public static final String SERVLET_PARAMETER_HEARTBEAT_INTERVAL = "heartbeatInterval";
    public static final String SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS = "closeIdleSessions";
    public static final String SERVLET_PARAMETER_PUSH_MODE = "pushMode";
    public static final String SERVLET_PARAMETER_SYNC_ID_CHECK = "syncIdCheck";
    public static final String SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS = "sendUrlsAsParameters";
    public static final String SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING = "pushLongPollingSuspendTimeout";
    public static final String SERVLET_PARAMETER_ROUTER_CONFIGURATOR = "routerConfigurator";
    public static final String SERVLET_PARAMETER_POLYFILL_BASE = "polyfillBase";
    public static final String SERVLET_PARAMETER_USING_NEW_ROUTING = "usingNewRouting";

    /**
     * Configuration name for loading the ES5 adapter.
     */
    public static final String LOAD_ES5_ADAPTER = "load.es5.adapter";

    /**
     * Configuration name for forcing the ShadyDOM polyfill.
     */
    public static final String FORCE_SHADY_DOM = "force.shady.dom";

    /**
     * Configuration name for the build URL of ES6 web components.
     */
    public static final String FRONTEND_URL_ES6 = "frontend.url.es6";

    /**
     * Configuration name for the build URL of ES5 web components.
     */
    public static final String FRONTEND_URL_ES5 = "frontend.url.es5";

    /**
     * Default value of the configuration of the build URL of ES6 web
     * components.
     */
    public static final String FRONTEND_URL_ES6_DEFAULT_VALUE = ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
            + ApplicationConstants.VAADIN_STATIC_FILES_PATH + "frontend/es6/";

    /**
     * Default value of the configuration of the build URL of ES5 web
     * components.
     */
    public static final String FRONTEND_URL_ES5_DEFAULT_VALUE = ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
            + ApplicationConstants.VAADIN_STATIC_FILES_PATH + "frontend/es5/";

    public static final String WEB_COMPONENTS = "webComponents";

    private Constants() {
        // prevent instantiation constants class only
    }
}
