/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.server.communication;

import java.io.IOException;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.SessionExpiredHandler;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Handles heartbeat requests. Heartbeat requests are periodically sent by the
 * client-side to inform the server that the UI sending the heartbeat is still
 * alive (the browser window is open, the connection is up) even when there are
 * no UIDL requests for a prolonged period of time. UIs that do not receive
 * either heartbeat or UIDL requests are eventually removed from the session and
 * garbage collected.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class HeartbeatHandler extends SynchronizedRequestHandler
        implements SessionExpiredHandler {

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return HandlerHelper.isRequestType(request, RequestType.HEARTBEAT);
    }

    /**
     * Handles a heartbeat request for the given session. Reads the GET
     * parameter named {@link ApplicationConstants#UI_ID_PARAMETER} to identify
     * the UI. If the UI is found in the session, sets it
     * {@link UIInternals#getLastHeartbeatTimestamp() heartbeat timestamp} to
     * the current time. Otherwise, writes a HTTP Not Found error to the
     * response.
     */
    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        UI ui = session.getService().findUI(request);
        if (ui != null) {
            ui.getInternals()
                    .setLastHeartbeatTimestamp(System.currentTimeMillis());
            // Ensure that the browser does not cache heartbeat responses.
            // iOS 6 Safari requires this
            // (https://github.com/vaadin/framework/issues/3226)
            response.setHeader("Cache-Control", "no-cache");
            // If Content-Type is not set, browsers assume text/html and may
            // complain about the empty response body
            // (https://github.com/vaadin/framework/issues/4167)
            response.setHeader("Content-Type", "text/plain");
        } else {
            response.sendError(HttpStatusCode.NOT_FOUND.getCode(),
                    "UI not found");
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.vaadin.server.SessionExpiredHandler#handleSessionExpired(com.vaadin
     * .server.VaadinRequest, com.vaadin.server.VaadinResponse)
     */
    @Override
    public boolean handleSessionExpired(VaadinRequest request,
            VaadinResponse response) throws IOException {
        if (!HandlerHelper.isRequestType(request, RequestType.HEARTBEAT)) {
            return false;
        }

        response.sendError(HttpStatusCode.FORBIDDEN.getCode(),
                "Session expired");
        return true;
    }
}
