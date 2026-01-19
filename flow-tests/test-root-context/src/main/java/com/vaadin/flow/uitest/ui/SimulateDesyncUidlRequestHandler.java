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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.ServerRpcHandler;
import com.vaadin.flow.server.communication.UidlRequestHandler;

/**
 * Custom UidlRequestHandler that can simulate a desync by throwing
 * MessageIdSyncException when enabled.
 */
public class SimulateDesyncUidlRequestHandler extends UidlRequestHandler {

    /**
     * Session attribute key for the desync simulation flag.
     */
    public static final String SIMULATE_DESYNC_ATTR = "simulateDesync";

    /**
     * Enables desync simulation for the given session. The next RPC request in
     * this session will throw a MessageIdSyncException.
     */
    public static void enableDesync(VaadinSession session) {
        session.setAttribute(SIMULATE_DESYNC_ATTR, Boolean.TRUE);
    }

    /**
     * Disables desync simulation for the given session.
     */
    public static void disableDesync(VaadinSession session) {
        session.setAttribute(SIMULATE_DESYNC_ATTR, null);
    }

    /**
     * Checks if desync simulation is enabled for the given session.
     */
    public static boolean isDesyncEnabled(VaadinSession session) {
        return Boolean.TRUE.equals(session.getAttribute(SIMULATE_DESYNC_ATTR));
    }

    @Override
    protected ServerRpcHandler createRpcHandler() {
        return new ServerRpcHandler() {
            @Override
            public void handleRpc(UI ui, String message, VaadinRequest request)
                    throws InvalidUIDLSecurityKeyException {
                VaadinSession session = ui.getSession();
                if (isDesyncEnabled(session)) {
                    // Reset the flag so subsequent requests work normally
                    // This simulates a one-time sync error (like after pod
                    // crash)
                    disableDesync(session);

                    // Simulate the client being ahead of the server
                    // by throwing a MessageIdSyncException
                    int expectedId = ui.getInternals()
                            .getLastProcessedClientToServerId() + 1;
                    int receivedId = expectedId + 100;
                    throw new MessageIdSyncException(expectedId, receivedId);
                }
                super.handleRpc(ui, message, request);
            }
        };
    }
}
