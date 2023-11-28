/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.shared;

import java.io.Serializable;

import com.vaadin.flow.component.internal.UIInternals;

/**
 * A utility class providing static constants. Mostly for internal use.
 * <p>
 * Also available for the client side part of the framework.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ApplicationConstants implements Serializable {

    /**
     * Protocol used for referencing the application context path.
     */
    public static final String CONTEXT_PROTOCOL_PREFIX = "context://";

    /**
     * Protocol used for referencing resources relative the base URI of the
     * loaded page.
     */
    public static final String BASE_PROTOCOL_PREFIX = "base://";

    /**
     * The identifier used for the CSRF token.
     */
    public static final String UIDL_SECURITY_TOKEN_ID = "Vaadin-Security-Key";

    /**
     * The identifier used for Push messages.
     */
    public static final String UIDL_PUSH_ID = "Vaadin-Push-ID";

    /**
     * The URL which should be used to connect server-side VaadinService.
     */
    public static final String SERVICE_URL = "serviceUrl";

    /**
     * Whether the application is run in as a exported Web Component.
     */
    public static final String APP_WC_MODE = "webComponentMode";

    /**
     * Whether the application is running vaadin-router in client side.
     */
    public static final String CLIENT_ROUTING = "clientRouting";

    /**
     * Configuration parameter giving the (in some cases relative) URL to the
     * web application context root.
     */
    public static final String CONTEXT_ROOT_URL = "contextRootUrl";

    /**
     * The prefix used for all internal static files, relative to context root.
     */
    public static final String VAADIN_STATIC_FILES_PATH = "VAADIN/static/";
    /**
     * The name of the javascript containing push support.
     */
    public static final String VAADIN_PUSH_JS = VAADIN_STATIC_FILES_PATH
            + "push/vaadinPush-min.js";

    /**
     * The name of the debug version of the javascript containing push support.
     */
    public static final String VAADIN_PUSH_DEBUG_JS = VAADIN_STATIC_FILES_PATH
            + "push/vaadinPush.js";

    /**
     * Name of the parameter used to transmit the push connection identifier.
     */
    public static final String PUSH_ID_PARAMETER = "v-pushId";

    /**
     * The name of the parameter used to transmit RPC invocations.
     */
    public static final String RPC_INVOCATIONS = "rpc";

    /**
     * The name of the parameter used to transmit the CSRF token.
     */
    public static final String CSRF_TOKEN = "csrfToken";

    /**
     * The name of the parameter used to transmit the sync id. The value can be
     * set to -1 e.g. when testing with pre-recorded requests to make the
     * framework ignore the sync id.
     *
     * @see UIInternals#getServerSyncId()
     */
    public static final String SERVER_SYNC_ID = "syncId";

    /**
     * The name of the parameter used to transmit the id of the client to server
     * messages.
     *
     */
    public static final String CLIENT_TO_SERVER_ID = "clientId";

    /**
     * The name of the parameter used to transmit the id of UI used in given
     * request.
     */
    public static final String UI_ID = "uiId";

    /**
     * Default value to use in case the security protection is disabled.
     */
    public static final String CSRF_TOKEN_DEFAULT_VALUE = "init";

    /**
     * The name of the parameter used for re-synchronizing.
     */
    public static final String RESYNCHRONIZE_ID = "resynchronize";

    /**
     * Content type to use for text/html responses (should always be UTF-8).
     */
    public static final String CONTENT_TYPE_TEXT_HTML_UTF_8 = "text/html; charset=utf-8";

    /**
     * Content type to use for text/javascript responses (should always be
     * UTF-8).
     */
    public static final String CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8 = "text/javascript; charset=utf-8";

    /**
     * Name of the parameter used to transmit UI ids back and forth.
     */
    public static final String UI_ID_PARAMETER = "v-uiId";

    /**
     * Path to the Vaadin client engine folder, relative to the context root.
     */
    public static final String CLIENT_ENGINE_PATH = VAADIN_STATIC_FILES_PATH
            + "client";

    /**
     * Parameter for the initial router location when JavaScript bootstrapping.
     * It is optional when {@link ApplicationConstants#REQUEST_TYPE_PARAMETER}
     * has the {@link ApplicationConstants#REQUEST_TYPE_INIT} value
     */
    public static final String REQUEST_LOCATION_PARAMETER = "location";

    /**
     * Get parameter used in framework requests to identify the request type.
     */
    public static final String REQUEST_TYPE_PARAMETER = "v-r";

    /**
     * Request type parameter value indicating an init request.
     */
    public static final String REQUEST_TYPE_INIT = "init";

    /**
     * Request type parameter value indicating a UIDL request.
     */
    public static final String REQUEST_TYPE_UIDL = "uidl";

    /**
     * Request type parameter value indicating a heartbeat request.
     */
    public static final String REQUEST_TYPE_HEARTBEAT = "heartbeat";

    /**
     * Request type parameter value indicating a push request.
     */
    public static final String REQUEST_TYPE_PUSH = "push";

    /**
     * Request type parameter value indicating a WebComponent resynchronization
     * request.
     */
    public static final String REQUEST_TYPE_WEBCOMPONENT_RESYNC = "webcomponent-resync";

    /**
     * Attribute name for marking internal router link anchors.
     */
    public static final String ROUTER_LINK_ATTRIBUTE = "router-link";

    /**
     * Configuration parameter for the build URL of ES6 web components.
     */
    public static final String FRONTEND_URL_ES6 = "frontendUrlEs6";

    /**
     * Web socket parameter which identifies connection as live reload
     * connection.
     */
    public static final String DEBUG_WINDOW_CONNECTION = "debug_window";

    /**
     * Boolean client configuration parameter enabling the development mode
     * gizmo.
     */
    public static final String DEVMODE_GIZMO_ENABLED = "devmodeGizmoEnabled";

    /**
     * The name of the parameter used for notifying the server that user closed
     * the tab/window or navigated away.
     */
    public static final String UNLOAD_BEACON = "UNLOAD";

}
