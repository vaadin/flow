/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.component.UI;

/**
 * Constants for all servlet init parameters. Keeping them in a separate class
 * allows using reflection to expose the parameters in the Spring add-on.
 *
 * <p>
 * Note: do not add other constants than String constants representing init
 * parameters here.
 *
 * @author Vaadin Ltd
 * @since
 */
public class InitParameters implements Serializable {

    /**
     * The name of the parameter that is by default used in e.g. web.xml to
     * define the name of the default {@link UI} class.
     */
    // javadoc in UI should be updated if this value is changed
    public static final String UI_PARAMETER = "UI";

    public static final String SERVLET_PARAMETER_PRODUCTION_MODE = "productionMode";

    /**
     * @deprecated Bower WebJars are no longer supported.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_COMPATIBILITY_MODE = "compatibilityMode";

    /**
     * @deprecated Bower WebJars are no longer supported.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_BOWER_MODE = "bowerMode";

    public static final String SERVLET_PARAMETER_ENABLE_DEV_SERVER = "enableDevServer";
    public static final String SERVLET_PARAMETER_REUSE_DEV_SERVER = "reuseDevServer";
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
    public static final String SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT = "maxMessageSuspendTimeout";
    public static final String SERVLET_PARAMETER_JSBUNDLE = "module.bundle";
    public static final String SERVLET_PARAMETER_POLYFILLS = "module.polyfills";
    public static final String NODE_VERSION = "node.version";
    public static final String NODE_DOWNLOAD_ROOT = "node.download.root";

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
     * Configuration name for the WebPack profile statistics json file to use to
     * determine template contents.
     * <p>
     * File needs to be available either for the ClassLoader as a resource, or
     * as a static web resource. By default it returns the value in
     * {@link Constants#STATISTICS_JSON_DEFAULT}
     */
    public static final String SERVLET_PARAMETER_STATISTICS_JSON = "statistics.file.path";

    /**
     * Configuration name for the time waiting for webpack output success or
     * error pattern defined in
     * {@link Constants#SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN} and
     * {@link Constants#SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN}
     * parameters.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT = "devmode.webpack.output.pattern.timeout";

    /**
     * Configuration name for the pattern used to inspect the webpack output to
     * assure it is up and running. Default value is defined in
     * {@link DevModeHandler} as the <code>: Compiled</code> expression.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN = "devmode.webpack.output.success.pattern";

    /**
     * Configuration name for the pattern used to inspect the webpack output to
     * detecting when compilation failed. Default value is defined in
     * {@link DevModeHandler} as the <code>: Failed</code> expression.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN = "devmode.webpack.output.error.pattern";

    /**
     * Configuration name for adding extra options to the webpack-dev-server.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS = "devmode.webpack.options";

    /**
     * Boolean parameter for enabling/disabling bytecode scanning in dev mode.
     * If enabled, entry points are scanned for reachable frontend resources. If
     * disabled, all classes on the classpath are scanned.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE = "devmode.optimizeBundle";

    /**
     * Configuration parameter name for enabling pnpm.
     *
     * @since 2.2
     */
    public static final String SERVLET_PARAMETER_ENABLE_PNPM = "pnpm.enable";

    /**
     * Configuration parameter name for enabling live reload.
     *
     * @since
     */
    public static final String SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD = "devmode.liveReload.enabled";

    /**
     * I18N provider property.
     */
    public static final String I18N_PROVIDER = "i18n.provider";

    /**
     * Configuration name for the parameter that determines if Flow should
     * automatically register servlets needed for the application to work.
     */
    public static final String DISABLE_AUTOMATIC_SERVLET_REGISTRATION = "disable.automatic.servlet.registration";

    /**
     * Configuration parameter name for requiring node executable installed in
     * home directory.
     *
     * @since
     */
    public static final String REQUIRE_HOME_NODE_EXECUTABLE = "require.home.node";

    /**
     * Configuration name for the parameter that sets the compiled web
     * components path. The path should be the same as
     * {@code webComponentOutputDirectoryName} in the maven plugin that
     * transpiles ES6 code. This path is only used for generated web components
     * (server side web components) module in case they are transpiled: web
     * component UI imports them as dependencies.
     */
    public static final String COMPILED_WEB_COMPONENTS_PATH = "compiled.web.components.path";

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
     * Boolean parameter for enabling/disabling transpilation for IE11 with the
     * BabelMultiTargetPlugin in dev mode.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_TRANSPILE = "devmode.transpile";

    /**
     * Property boolean for marking stats.json to be fetched from external
     * location.
     */
    public static final String EXTERNAL_STATS_FILE = "external.stats.file";
    /**
     * Property String for external stats.json location url.
     */
    public static final String EXTERNAL_STATS_URL = "external.stats.url";

    /**
     * Configuration parameter name for enabling old JavaScript license checker
     * and disable server-side and offline license checker features.
     * <p>
     * Compatibility/Bower mode always uses old license checking.
     *
     * @since 2.8
     */
    public static final String SERVLET_PARAMETER_ENABLE_OLD_LICENSE_CHECKER = "oldLicenseChecker";

    /**
     * Configuration name for enabling ci build for npm/pnpm.
     */
    public static final String CI_BUILD = "ci.build";

    /**
     * A property that enforces full experience validation for Flow components.
     * <p>
     * The full experience validation integrates web component's own validation,
     * server-side component's constraints and Binder validation into a seamless
     * chain. By default, it's disabled, which means that components aren't
     * validated on blur, for example.
     * <p>
     * For more detailed information, please refer to:
     * https://github.com/vaadin/platform/issues/3066#issuecomment-1598771284
     */
    public static final String ENFORCE_FIELD_VALIDATION = "enforceFieldValidation";
}
