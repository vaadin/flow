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

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.hummingbird.StateTree;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Serializes pending server-side changes to UI state to JSON. This includes
 * shared state, client RPC invocations, connector hierarchy changes, connector
 * type information among others.
 *
 * @author Vaadin Ltd
 * @since 7.1
 */
public class UidlWriter implements Serializable {

    /**
     * Creates a JSON object containing all pending changes to the given UI.
     *
     * @param ui
     *            The {@link UI} whose changes to write
     * @param async
     *            True if this message is sent by the server asynchronously,
     *            false if it is a response to a client message.
     * @return JSON object containing the UIDL response
     */
    public JsonObject createUidl(UI ui, boolean async) {
        JsonObject response = Json.createObject();

        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();

        // Purge pending access calls as they might produce additional changes
        // to write out
        service.runPendingAccessTasks(session);

        // Paints components
        getLogger().log(Level.FINE, "* Creating response to client");

        int syncId = service.getDeploymentConfiguration().isSyncIdCheckEnabled()
                ? ui.getServerSyncId() : -1;

        response.put(ApplicationConstants.SERVER_SYNC_ID, syncId);
        int nextClientToServerMessageId = ui.getLastProcessedClientToServerId()
                + 1;
        response.put(ApplicationConstants.CLIENT_TO_SERVER_ID,
                nextClientToServerMessageId);

        SystemMessages messages = ui.getSession().getService()
                .getSystemMessages(ui.getLocale(), null);

        JsonObject meta = new MetadataWriter().createMetadata(ui, false, async,
                messages);
        response.put("meta", meta);

        JsonArray changes = encodeChanges(ui);
        if (changes.length() != 0) {
            response.put("changes", changes);
        }

        response.put("timings", createPerformanceData(ui));
        ui.incrementServerId();
        return response;
    }

    /**
     * Encodes the state tree changes of the given UI.
     *
     * @param ui
     *            the UI
     * @return a JSON array of changes
     */
    private JsonArray encodeChanges(UI ui) {
        JsonArray changes = Json.createArray();

        StateTree stateTree = ui.getStateTree();

        stateTree.collectChanges(
                change -> changes.set(changes.length(), change.toJson()));

        return changes;
    }

    /**
     * Adds the performance timing data (used by TestBench 3) to the UIDL
     * response.
     */
    private JsonValue createPerformanceData(UI ui) {
        JsonArray timings = Json.createArray();
        timings.set(0, ui.getSession().getCumulativeRequestDuration());
        timings.set(1, ui.getSession().getLastRequestDuration());
        return timings;
    }

    private static final Logger getLogger() {
        return Logger.getLogger(UidlWriter.class.getName());
    }
}
