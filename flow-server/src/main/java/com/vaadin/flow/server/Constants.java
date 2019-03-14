/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Constants used by the server side framework.
 *
 *
 */
public final class Constants implements Serializable {

    // Keep the version number in sync with flow-push/pom.xml
    public static final String REQUIRED_ATMOSPHERE_RUNTIME_VERSION = "2.4.30.vaadin1";

    public static final String SERVLET_PARAMETER_PRODUCTION_MODE = "productionMode";

    /** enable it if your project is a Polymer 2.0 one, should be removed in V15 */
    public static final String SERVLET_PARAMETER_BOWER_MODE = "bowerMode";

    public static final String SERVLET_PARAMETER_REQUEST_TIMING = "requestTiming";
    // Javadocs for VaadinService should be updated if this value is changed
    public static final String SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION = "disable-xsrf-protection";
    public static final String SERVLET_PARAMETER_HEARTBEAT_INTERVAL = "heartbeatInterval";
    public static final String SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT = "webComponentDisconnect";
    public static final String SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS = "closeIdleSessions";
    public static final String SERVLET_PARAMETER_PUSH_MODE = "pushMode";
    public static final String SERVLET_PARAMETER_PUSH_URL = "pushURL";
    public static final String SERVLET_PARAMETER_SYNC_ID_CHECK = "syncIdCheck";
    public static final String SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS = "sendUrlsAsParameters";
    public static final String SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING = "pushLongPollingSuspendTimeout";

    public static final String SERVLET_PARAMETER_JSBUNDLE = "module.bundle";
    public static final String JSBUNDLE_DEFAULT_VALUE = "build/index.js";
    public static final String SERVLET_PARAMETER_POLYFILLS = "module.polyfills";
    public static final String POLYFILLS_DEFAULT_VALUE = "build/webcomponentsjs/webcomponents-loader.js";

    /**
     * Configuration name for the parameter that determines whether Brotli
     * compression should be used for static resources in cases when a
     * precompressed file is available.
     */
    public static final String SERVLET_PARAMETER_BROTLI = "brotli";

    /**
     * Configuration name for loading the ES5 adapters.
     */
    public static final String LOAD_ES5_ADAPTERS = "load.es5.adapters";

    /**
     * Configuration name for the frontend URL prefix for ES6.
     */
    public static final String FRONTEND_URL_ES6 = "frontend.url.es6";

    /**
     * Configuration name for the frontend URL prefix for ES5.
     */
    public static final String FRONTEND_URL_ES5 = "frontend.url.es5";

    /**
     * Default frontend URL prefix for ES6.
     */
    public static final String FRONTEND_URL_ES6_DEFAULT_VALUE = ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
            + "frontend-es6/";

    /**
     * Default frontend URL prefix for ES.
     */
    public static final String FRONTEND_URL_ES5_DEFAULT_VALUE = ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
            + "frontend-es5/";

    /**
     * Default frontend URL prefix for development.
     */
    public static final String FRONTEND_URL_DEV_DEFAULT = ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
            + "frontend/";

    /**
     * Configuration name for the parameter that determines if Flow should use
     * webJars or not.
     */
    public static final String DISABLE_WEBJARS = "disable.webjars";

    /**
     * Configuration name for the parameter that determines if Flow should use
     * bundled fragments or not.
     */
    public static final String USE_ORIGINAL_FRONTEND_RESOURCES = "original.frontend.resources";

    /**
     * I18N provider property.
     */
    public static final String I18N_PROVIDER = "i18n.provider";

    /**
     * Configuration name for the parameter that determines if Flow should automatically
     * register servlets needed for the application to work.
     */
    public static final String DISABLE_AUTOMATIC_SERVLET_REGISTRATION = "disable.automatic.servlet.registration";

    /**
     * The file path for WebPack profile statistics json file to use to
     * determine template contents.
     * <p>
     * File needs to be available either for the ClassLoader as a resource, or
     * as a static web resource. By default it returns the value in
     * {@link Constants#STATISTICS_JSON_DEFAULT}
     */
    public static final String STATISTICS_JSON = "statistics.file.path";

    /**
     * Default path for the WebPack profile statistics json file. It can be
     * modifyied by setting the system property "statistics.file.path".
     */
    public static final String STATISTICS_JSON_DEFAULT = "/target/build/stats.json";

    private Constants() {
        // prevent instantiation constants class only
    }
}
