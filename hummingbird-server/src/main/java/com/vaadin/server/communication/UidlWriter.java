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
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateTree;
import com.vaadin.hummingbird.change.MapPutChange;
import com.vaadin.hummingbird.namespace.TemplateNamespace;
import com.vaadin.hummingbird.shared.Namespaces;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.hummingbird.util.JsonUtil;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.JsonConstants;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIInternals;
import com.vaadin.ui.UIInternals.JavaScriptInvocation;

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

        UIInternals uiInternals = ui.getInternals();

        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();

        // Purge pending access calls as they might produce additional changes
        // to write out
        service.runPendingAccessTasks(session);

        // Paints components
        getLogger().log(Level.FINE, "* Creating response to client");

        int syncId = service.getDeploymentConfiguration().isSyncIdCheckEnabled()
                ? uiInternals.getServerSyncId() : -1;

        response.put(ApplicationConstants.SERVER_SYNC_ID, syncId);
        int nextClientToServerMessageId = uiInternals
                .getLastProcessedClientToServerId() + 1;
        response.put(ApplicationConstants.CLIENT_TO_SERVER_ID,
                nextClientToServerMessageId);

        SystemMessages messages = ui.getSession().getService()
                .getSystemMessages(ui.getLocale(), null);

        JsonObject meta = new MetadataWriter().createMetadata(ui, false, async,
                messages);
        if (meta.keys().length > 0) {
            response.put("meta", meta);
        }

        JsonArray stateChanges = Json.createArray();
        JsonObject templates = Json.createObject();

        encodeChanges(ui, stateChanges, templates);

        if (stateChanges.length() != 0) {
            response.put("changes", stateChanges);
        }
        if (templates.keys().length > 0) {
            response.put("templates", templates);
        }

        List<JavaScriptInvocation> executeJavaScriptList = uiInternals
                .dumpPendingJavaScriptInvocations();
        if (!executeJavaScriptList.isEmpty()) {
            response.put(JsonConstants.UIDL_KEY_EXECUTE,
                    encodeExecuteJavaScriptList(executeJavaScriptList));
        }

        response.put("timings", createPerformanceData(ui));
        uiInternals.incrementServerId();
        return response;
    }

    // non-private for testing purposes
    static JsonArray encodeExecuteJavaScriptList(
            List<JavaScriptInvocation> executeJavaScriptList) {
        return executeJavaScriptList.stream()
                .map(UidlWriter::encodeExecuteJavaScript)
                .collect(JsonUtil.asArray());
    }

    private static JsonArray encodeExecuteJavaScript(
            JavaScriptInvocation executeJavaScript) {
        Stream<JsonValue> parametersStream = executeJavaScript.getParameters()
                .stream().map(JsonCodec::encodeWithTypeInfo);

        // [argument1, argument2, ..., script]
        return Stream
                .concat(parametersStream,
                        Stream.of(
                                Json.create(executeJavaScript.getExpression())))
                .collect(JsonUtil.asArray());
    }

    /**
     * Encodes the state tree changes of the given UI.
     *
     * @param ui
     *            the UI
     * @param stateChanges
     *            a JSON array to put state changes into
     * @param templates
     *            a JSON object to put new template nodes into
     */
    private void encodeChanges(UI ui, JsonArray stateChanges,
            JsonObject templates) {
        StateTree stateTree = ui.getInternals().getStateTree();

        Consumer<TemplateNode> templateEncoder = new Consumer<TemplateNode>() {
            private UIInternals uiInternals = ui.getInternals();

            @Override
            public void accept(TemplateNode templateNode) {
                // Send to client if it's a new template
                if (!uiInternals.isTemplateSent(templateNode)) {
                    uiInternals.setTemplateSent(templateNode);

                    JsonObject json = templateNode.toJson(this);

                    templates.put(Integer.toString(templateNode.getId()), json);
                }
            }
        };

        stateTree.collectChanges(change -> {
            // Ensure new templates are sent to the client
            if (change instanceof MapPutChange) {
                MapPutChange put = (MapPutChange) change;
                if (put.getNamespace() == TemplateNamespace.class
                        && put.getKey().equals(Namespaces.ROOT_TEMPLATE_ID)) {
                    Integer id = (Integer) put.getValue();
                    TemplateNode templateNode = TemplateNode.get(id.intValue());
                    templateEncoder.accept(templateNode);
                }
            }

            // Encode the actual change
            stateChanges.set(stateChanges.length(), change.toJson());
        });
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
