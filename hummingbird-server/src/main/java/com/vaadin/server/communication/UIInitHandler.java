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

package com.vaadin.server.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.server.SynchronizedRequestHandler;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.JsonConstants;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

/**
 * Handles an initial request from the client to initialize a {@link UI}.
 *
 * @author Vaadin Ltd
 * @since 7.1
 */
public abstract class UIInitHandler extends SynchronizedRequestHandler {

    public static final String BROWSER_DETAILS_PARAMETER = "v-browserDetails";

    protected abstract boolean isInitRequest(VaadinRequest request);

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return isInitRequest(request);
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        try {
            assert UI.getCurrent() == null;

            // Update browser information from the request
            session.getBrowser().updateRequestDetails(request);

            UI uI = getBrowserDetailsUI(request, session);

            JsonObject params = Json.createObject();
            params.put(ApplicationConstants.UI_ID_PARAMETER, uI.getUIId());
            String initialUIDL = getInitialUidl(request, uI);
            params.put("uidl", initialUIDL);

            return commitJsonResponse(request, response,
                    JsonUtil.stringify(params));
        } catch (JsonException e) {
            throw new IOException("Error producing initial UIDL", e);
        }
    }

    /**
     * Commit the JSON response. We can't write immediately to the output stream
     * as we want to write only a critical notification if something goes wrong
     * during the response handling.
     *
     * @param request
     *            The request that resulted in this response
     * @param response
     *            The response to write to
     * @param json
     *            The JSON to write
     * @return true if the JSON was written successfully, false otherwise
     * @throws IOException
     *             If there was an exception while writing to the output
     */
    static boolean commitJsonResponse(VaadinRequest request,
            VaadinResponse response, String json) throws IOException {
        // The response was produced without errors so write it to the client
        response.setContentType(JsonConstants.JSON_CONTENT_TYPE);

        // Ensure that the browser does not cache UIDL responses.
        // iOS 6 Safari requires this (#9732)
        response.setHeader("Cache-Control", "no-cache");

        byte[] b = json.getBytes("UTF-8");
        response.setContentLength(b.length);

        OutputStream outputStream = response.getOutputStream();
        outputStream.write(b);
        // NOTE GateIn requires the buffers to be flushed to work
        outputStream.flush();

        return true;
    }

    private UI getBrowserDetailsUI(VaadinRequest request,
            VaadinSession session) {
        List<UIProvider> uiProviders = session.getUIProviders();

        UIClassSelectionEvent classSelectionEvent = new UIClassSelectionEvent(
                request);

        UIProvider provider = null;
        Class<? extends UI> uiClass = null;
        for (UIProvider p : uiProviders) {
            uiClass = p.getUIClass(classSelectionEvent);
            if (uiClass != null) {
                provider = p;
                break;
            }
        }

        if (provider == null || uiClass == null) {
            return null;
        }

        String embedId = getEmbedId(request);
        Integer uiId = Integer.valueOf(session.getNextUIid());

        // Explicit Class.cast to detect if the UIProvider does something
        // unexpected
        UICreateEvent event = new UICreateEvent(request, uiClass, uiId);
        UI ui = uiClass.cast(provider.createInstance(event));

        // Initialize some fields for a newly created UI
        if (ui.getSession() != session) {
            // Session already set for LegacyWindow
            ui.setSession(session);
        }

        PushMode pushMode = provider.getPushMode(event);
        if (pushMode == null) {
            pushMode = session.getService().getDeploymentConfiguration()
                    .getPushMode();
        }
        ui.getPushConfiguration().setPushMode(pushMode);

        Transport transport = provider.getPushTransport(event);
        if (transport != null) {
            ui.getPushConfiguration().setTransport(transport);
        }

        // Set thread local here so it is available in init
        UI.setCurrent(ui);

        ui.doInit(request, uiId.intValue(), embedId);

        session.addUI(ui);

        return ui;
    }

    /**
     * Constructs an embed id based on information in the request.
     *
     * @since 7.2
     *
     * @param request
     *            the request to get embed information from
     * @return the embed id, or <code>null</code> if id is not available.
     *
     * @see UI#getEmbedId()
     */
    protected String getEmbedId(VaadinRequest request) {
        // Parameters sent by vaadinBootstrap.js
        String windowName = request.getParameter("v-wn");
        String appId = request.getParameter("v-appId");

        if (windowName != null && appId != null) {
            return windowName + '.' + appId;
        } else {
            return null;
        }
    }

    /**
     * Generates the initial UIDL message that can e.g. be included in a html
     * page to avoid a separate round trip just for getting the UIDL.
     *
     * @param request
     *            the request that caused the initialization
     * @param uI
     *            the UI for which the UIDL should be generated
     * @return a string with the initial UIDL message
     * @throws IOException
     */
    protected String getInitialUidl(VaadinRequest request, UI uI) {

        JsonObject response = new UidlWriter().createUidl(uI, false);

        VaadinSession session = uI.getSession();
        if (session.getConfiguration().isXsrfProtectionEnabled()) {

            String seckey = session.getCsrfToken();
            response.put(ApplicationConstants.UIDL_SECURITY_TOKEN_ID, seckey);
        }

        String initialUIDL = response.toJson();
        getLogger().log(Level.FINE, "Initial UIDL:" + initialUIDL);
        return initialUIDL;
    }

    private static final Logger getLogger() {
        return Logger.getLogger(UIInitHandler.class.getName());
    }
}
