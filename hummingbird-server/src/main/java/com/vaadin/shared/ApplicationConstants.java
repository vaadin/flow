/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.shared;

import java.io.Serializable;

import com.vaadin.server.VaadinService;
import com.vaadin.ui.FrameworkData;

/**
 * A utility class providing static constants. Mostly for internal use.
 *
 * @author Vaadin
 * @since
 */
public class ApplicationConstants implements Serializable {

    /**
     * A class name automatically added to the &lt;body&gt; tag when written by
     * the framework.
     */
    public static final String GENERATED_BODY_CLASSNAME = "v-generated-body";

    /**
     * The path used for heartbeat requests.
     */
    public static final String HEARTBEAT_PATH = "HEARTBEAT";

    /**
     * The path used for push requests.
     */
    public static final String PUSH_PATH = "PUSH";

    /**
     * An internal protocol used for referencing the application context path.
     */
    public static final String SERVICE_PROTOCOL_PREFIX = "service://";
    /**
     * An internal protocol used for referencing the {@literal VAADIN} folder
     * inside the application context path.
     */
    public static final String VAADIN_PROTOCOL_PREFIX = "vaadin://";
    /**
     * An internal protocol used for identifying a font icon resource.
     */
    public static final String FONTICON_PROTOCOL_PREFIX = "fonticon://";
    /**
     * The identifier used for the CSRF token.
     */
    public static final String UIDL_SECURITY_TOKEN_ID = "Vaadin-Security-Key";

    /**
     * Configuration parameter giving the (in some cases relative) URL to the
     * {@link VaadinService}.
     * <p>
     */
    public static final String SERVICE_URL = "serviceUrl";

    /**
     * URL parameter used in UIDL requests to indicate that the full server-side
     * state should be returned to the client, i.e. without any incremental
     * changes.
     */
    public static final String URL_PARAMETER_REPAINT_ALL = "repaintAll";

    /**
     * Configuration parameter giving the (in some cases relative) URL to the
     * VAADIN folder from where static resources are loaded.
     */
    public static final String VAADIN_DIR_URL = "vaadinDir";

    /**
     * The name of the javascript containing push support. The file is located
     * in the VAADIN/push directory.
     */
    public static final String VAADIN_PUSH_JS = "vaadinPush.min.js";

    /**
     * The name of the debug version of the javascript containing push support.
     * The file is located in the VAADIN/push directory.
     *
     * @since 7.1.6
     */
    public static final String VAADIN_PUSH_DEBUG_JS = "vaadinPush.js";

    /**
     * Name of the parameter used to transmit the CSRF token.
     */
    public static final String CSRF_TOKEN_PARAMETER = "v-csrfToken";

    /**
     * The name of the parameter used to transmit RPC invocations
     *
     * @since 7.2
     */
    public static final String RPC_INVOCATIONS = "rpc";

    /**
     * The name of the parameter used to transmit the CSRF token
     *
     * @since 7.2
     */
    public static final String CSRF_TOKEN = "csrfToken";

    /**
     * The name of the parameter used to transmit the sync id. The value can be
     * set to -1 e.g. when testing with pre-recorded requests to make the
     * framework ignore the sync id.
     *
     * @see FrameworkData#getServerSyncId()
     * @since 7.2
     */
    public static final String SERVER_SYNC_ID = "syncId";

    /**
     * The name of the parameter used to transmit the id of the client to server
     * messages.
     *
     * @since 7.6
     */
    public static final String CLIENT_TO_SERVER_ID = "clientId";

    /**
     * Default value to use in case the security protection is disabled.
     */
    public static final String CSRF_TOKEN_DEFAULT_VALUE = "init";

    /**
     * The name of the parameter used for re-synchronizing.
     */
    public static final String RESYNCHRONIZE_ID = "resynchronize";

    /**
     * The name of the parameter used for sending the widget set version to the
     * server
     *
     * @since 7.6
     */
    public static final String WIDGETSET_VERSION_ID = "wsver";

    /**
     * Name of the parameter used to transmit UI ids back and forth.
     */
    public static final String UI_ID_PARAMETER = "v-uiId";

    /**
     * Relative path of the Vaadin client engine folder inside the VAADIN
     * folder.
     */
    public static final String CLIENT_ENGINE_FOLDER = "client";

    public static final String REQUEST_TYPE_PARAMETER = "v-r";
    public static final String REQUEST_TYPE_UIDL = "uidl";

}
