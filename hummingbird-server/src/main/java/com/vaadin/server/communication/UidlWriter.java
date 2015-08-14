/*
 * Copyright 2000-2014 Vaadin Ltd.
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
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.vaadin.annotations.HTML;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.hummingbird.kernel.AbstractElementTemplate;
import com.vaadin.hummingbird.kernel.AttributeBinding;
import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ForElementTemplate;
import com.vaadin.hummingbird.kernel.ModelAttributeBinding;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.StaticChildrenElementTemplate;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChangeVisitor;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.LegacyCommunicationManager;
import com.vaadin.server.LegacyCommunicationManager.ClientCache;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.ConnectorTracker;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Serializes pending server-side changes to UI state to JSON. This includes
 * shared state, client RPC invocations, connector hierarchy changes, connector
 * type information among others.
 *
 * @author Vaadin Ltd
 * @since 7.1
 */
public class UidlWriter implements Serializable {

    private static final String DEPENDENCY_JAVASCRIPT = "scriptDependencies";
    private static final String DEPENDENCY_STYLESHEET = "stylesheetDependencies";
    private static final String DEPENDENCY_HTML = "htmlDependencies";
    private final Set<Class<? extends ClientConnector>> usedClientConnectors = new HashSet<Class<? extends ClientConnector>>();

    /**
     * Writes a JSON object containing all pending changes to the given UI.
     *
     * @param ui
     *            The {@link UI} whose changes to write
     * @param writer
     *            The writer to use
     * @param analyzeLayouts
     *            Whether detected layout problems should be logged.
     * @param async
     *            True if this message is sent by the server asynchronously,
     *            false if it is a response to a client message.
     *
     * @throws IOException
     *             If the writing fails.
     */
    public void write(UI ui, Writer writer, boolean async) throws IOException {
        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();

        // Purge pending access calls as they might produce additional changes
        // to write out
        service.runPendingAccessTasks(session);

        Set<ClientConnector> processedConnectors = new HashSet<ClientConnector>();

        LegacyCommunicationManager manager = session.getCommunicationManager();
        ClientCache clientCache = manager.getClientCache(ui);
        boolean repaintAll = clientCache.isEmpty();
        // Paints components
        ConnectorTracker uiConnectorTracker = ui.getConnectorTracker();
        getLogger().log(Level.FINE, "* Creating response to client");

        while (true) {
            ArrayList<ClientConnector> connectorsToProcess = new ArrayList<ClientConnector>();
            for (ClientConnector c : uiConnectorTracker.getDirtyConnectors()) {
                if (!processedConnectors.contains(c)
                        && LegacyCommunicationManager
                                .isConnectorVisibleToClient(c)) {
                    connectorsToProcess.add(c);
                }
            }

            if (connectorsToProcess.isEmpty()) {
                break;
            }

            for (ClientConnector connector : connectorsToProcess) {
                boolean initialized = uiConnectorTracker
                        .isClientSideInitialized(connector);
                processedConnectors.add(connector);

                try {
                    connector.beforeClientResponse(!initialized);
                } catch (RuntimeException e) {
                    manager.handleConnectorRelatedException(connector, e);
                }
            }
        }

        getLogger().log(Level.FINE, "Found " + processedConnectors.size()
                + " dirty connectors to paint");

        uiConnectorTracker.setWritingResponse(true);
        try {
            JSONObject response = new JSONObject();

            int syncId = service.getDeploymentConfiguration()
                    .isSyncIdCheckEnabled()
                            ? uiConnectorTracker.getCurrentSyncId() : -1;
            // writer.write("\"" + ApplicationConstants.SERVER_SYNC_ID + "\": "
            // + syncId + ", ");
            response.put(ApplicationConstants.SERVER_SYNC_ID, syncId);
            if (repaintAll) {
                response.put(ApplicationConstants.RESYNCHRONIZE_ID, true);
                // writer.write("\"" + ApplicationConstants.RESYNCHRONIZE_ID +
                // "\": true, ");
            }
            int nextClientToServerMessageId = ui
                    .getLastProcessedClientToServerId() + 1;
            response.put(ApplicationConstants.CLIENT_TO_SERVER_ID,
                    nextClientToServerMessageId);
            // writer.write("\"" + ApplicationConstants.CLIENT_TO_SERVER_ID +
            // "\": " + nextClientToServerMessageId);

            Collection<ClientConnector> dirtyVisibleConnectors = ui
                    .getConnectorTracker().getDirtyVisibleConnectors();
            JsonObject dependencies = Json.createObject();
            List<Class<? extends ClientConnector>> dependencyClasses = new ArrayList<>();
            for (ClientConnector c : dirtyVisibleConnectors) {
                Class<? extends ClientConnector> cls = c.getClass();
                if (!ui.getResourcesHandled().contains(cls)) {
                    dependencyClasses.add(cls);
                }
            }

            // /*
            // * Ensure super classes come before sub classes to get script
            // * dependency order right. Sub class @JavaScript might assume that
            // *
            // * @JavaScript defined by super class is already loaded.
            // */
            Collections.sort(dependencyClasses, new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> o1, Class<?> o2) {
                    // TODO optimize using Class.isAssignableFrom?
                    return hierarchyDepth(o1) - hierarchyDepth(o2);
                }

                private int hierarchyDepth(Class<?> type) {
                    if (type == Object.class) {
                        return 0;
                    } else {
                        return hierarchyDepth(type.getSuperclass()) + 1;
                    }
                }
            });
            for (Class<? extends ClientConnector> cls : dependencyClasses) {
                handleDependencies(ui, cls, response);
            }

            JSONArray changes = new JSONArray();
            JSONObject newTemplates = new JSONObject();

            ui.getRootNode().commit(new NodeChangeVisitor() {
                private boolean isServerOnly(StateNode node) {
                    if (node == null) {
                        return false;
                    } else if (node.containsKey(
                            AbstractElementTemplate.Keys.SERVER_ONLY)) {
                        return true;
                    } else {
                        return isServerOnly(node.getParent());
                    }
                }

                private JSONObject createChange(StateNode node, String type) {
                    assert!isServerOnly(node);

                    JSONObject change = new JSONObject();
                    change.put("type", type);
                    // abs since currently detached nodes have -id
                    change.put("id", Math.abs(node.getId()));
                    changes.put(change);
                    return change;
                }

                @Override
                public void visitRemoveChange(StateNode node,
                        RemoveChange removeChange) {
                    if (isServerOnly(node)) {
                        return;
                    }
                    if (removeChange.getValue() instanceof StateNode
                            && isServerOnly(
                                    (StateNode) removeChange.getValue())) {
                        return;
                    }
                    Object key = removeChange.getKey();
                    JSONObject change = createChange(node, "remove");
                    change.put("key", key);
                }

                @Override
                public void visitPutChange(StateNode node,
                        PutChange putChange) {
                    if (isServerOnly(node)) {
                        return;
                    }
                    JSONObject change;
                    Object key = putChange.getKey();
                    Object value = putChange.getValue();
                    if (value instanceof StateNode) {
                        StateNode childNode = (StateNode) value;
                        if (isServerOnly(childNode)) {
                            return;
                        }
                        if (key instanceof ElementTemplate) {
                            change = createChange(node, "putOverride");

                            ElementTemplate template = (ElementTemplate) key;
                            key = Integer.valueOf(template.getId());
                            ensureTemplateSent(template, ui, newTemplates);
                        } else {
                            change = createChange(node, "putNode");
                        }
                        change.put("value", childNode.getId());
                    } else {
                        change = createChange(node, "put");
                        if (value instanceof ElementTemplate) {
                            ElementTemplate template = (ElementTemplate) value;
                            value = Integer.valueOf(template.getId());
                            ensureTemplateSent(template, ui, newTemplates);
                        }
                        change.put("value", value);
                    }
                    change.put("key", key);

                    int length = changes.length();
                    if (length >= 2) {
                        JSONObject previousChange = changes
                                .getJSONObject(changes.length() - 2);
                        if ("remove".equals(previousChange.getString("type"))
                                && change.getInt("id") == previousChange
                                        .getInt("id")
                                && key.equals(
                                        previousChange.getString("key"))) {
                            changes.remove(changes.length() - 2);
                        }
                    }
                }

                private void ensureTemplateSent(ElementTemplate template, UI ui,
                        JSONObject newTemplates) {
                    Set<Integer> sentTemplates = ui.getSentTemplates();
                    if (sentTemplates == null) {
                        sentTemplates = new HashSet<>();
                    }

                    Integer templateId = Integer.valueOf(template.getId());
                    if (!sentTemplates.contains(templateId)) {
                        newTemplates.put(Integer.toString(template.getId()),
                                serializeTemplate(template, ui, newTemplates));
                        sentTemplates.add(templateId);
                    }

                    ui.setSentTemplates(sentTemplates);
                }

                private JSONObject serializeTemplate(ElementTemplate template,
                        UI ui, JSONObject newTemplates) {
                    JSONObject serialized = new JSONObject();
                    serialized.put("type", template.getClass().getSimpleName());
                    serialized.put("id", template.getId());

                    if (template.getClass() == BoundElementTemplate.class) {
                        serializeBoundElementTemplate(serialized,
                                (BoundElementTemplate) template);
                    } else if (template
                            .getClass() == StaticChildrenElementTemplate.class) {
                        serializeStaticChildrenElementEmplate(serialized,
                                (StaticChildrenElementTemplate) template, ui,
                                newTemplates);
                    } else
                        if (template.getClass() == ForElementTemplate.class) {
                        serializeForTemplate(serialized,
                                (ForElementTemplate) template, ui,
                                newTemplates);
                    } else {
                        throw new RuntimeException(template.toString());
                    }
                    return serialized;
                }

                private void serializeForTemplate(JSONObject serialized,
                        ForElementTemplate template, UI ui,
                        JSONObject newTemplates) {
                    serialized.put("modelKey", template.getModelProperty());

                    ElementTemplate childTemplate = template.getChildTemplate();
                    ensureTemplateSent(childTemplate, ui, newTemplates);
                    serialized.put("childTemplate", childTemplate.getId());

                    serializeBoundElementTemplate(serialized, template);
                }

                private void serializeStaticChildrenElementEmplate(
                        JSONObject serialized,
                        StaticChildrenElementTemplate template, UI ui,
                        JSONObject newTemplates) {
                    JSONArray children = new JSONArray();
                    serialized.put("children", children);
                    for (BoundElementTemplate childTemplate : template
                            .getChildren()) {
                        ensureTemplateSent(childTemplate, ui, newTemplates);
                        children.put(childTemplate.getId());
                    }
                    serializeBoundElementTemplate(serialized, template);
                }

                private void serializeBoundElementTemplate(
                        JSONObject serialized, BoundElementTemplate bet) {
                    JSONObject attributeBindings = new JSONObject();
                    for (AttributeBinding attributeBinding : bet
                            .getAttributeBindings().values()) {
                        if (attributeBinding instanceof ModelAttributeBinding) {
                            ModelAttributeBinding mab = (ModelAttributeBinding) attributeBinding;
                            attributeBindings.put(mab.getModelPath(),
                                    mab.getAttributeName());
                        } else {
                            // Not yet supported
                            throw new RuntimeException(
                                    attributeBinding.toString());
                        }
                    }

                    JSONObject defaultAttributes = new JSONObject();
                    bet.getDefaultAttributeValues()
                            .forEach(defaultAttributes::put);

                    serialized.put("attributeBindings", attributeBindings)
                            .put("defaultAttributes", defaultAttributes)
                            .put("tag", bet.getTag());
                }

                @Override
                public void visitParentChange(StateNode node,
                        ParentChange parentChange) {
                    // Ignore
                }

                @Override
                public void visitListReplaceChange(StateNode node,
                        ListReplaceChange listReplaceChange) {
                    if (isServerOnly(node)) {
                        return;
                    }
                    JSONObject change;
                    Object value = listReplaceChange.getNewValue();
                    if (value instanceof StateNode) {
                        change = createChange(node, "listReplaceNode");
                        change.put("value", ((StateNode) value).getId());
                    } else {
                        change = createChange(node, "listReplace");
                        change.put("value", value);
                    }
                    change.put("index", listReplaceChange.getIndex());
                    change.put("key", listReplaceChange.getKey());
                }

                @Override
                public void visitListRemoveChange(StateNode node,
                        ListRemoveChange listRemoveChange) {
                    if (isServerOnly(node)) {
                        return;
                    }
                    JSONObject change = createChange(node, "listRemove");
                    change.put("index", listRemoveChange.getIndex());
                    change.put("key", listRemoveChange.getKey());
                }

                @Override
                public void visitListInsertChange(StateNode node,
                        ListInsertChange listInsertChange) {
                    if (isServerOnly(node)) {
                        return;
                    }
                    JSONObject change;
                    Object value = listInsertChange.getValue();
                    if (value instanceof StateNode) {
                        change = createChange(node, "listInsertNode");
                        change.put("value", ((StateNode) value).getId());
                    } else {
                        change = createChange(node, "listInsert");
                        change.put("value", value);
                    }
                    change.put("index", listInsertChange.getIndex());
                    change.put("key", listInsertChange.getKey());
                }

                @Override
                public void visitIdChange(StateNode node, IdChange idChange) {
                    // Ignore
                }
            });
            response.put("elementTemplates", newTemplates);
            response.put("elementChanges", changes);
            String r = response.toString();
            writer.write(r.substring(1, r.length() - 1));
        } finally {
            uiConnectorTracker.setWritingResponse(false);
            uiConnectorTracker.cleanConnectorMap();
        }
    }

    /**
     * @since
     * @param cls
     * @param response
     */
    private void handleDependencies(UI ui, Class<? extends ClientConnector> cls,
            JSONObject response) {
        if (ui.getResourcesHandled().contains(cls)) {
            return;
        }

        ui.getResourcesHandled().add(cls);
        LegacyCommunicationManager manager = ui.getSession()
                .getCommunicationManager();

        JavaScript jsAnnotation = cls.getAnnotation(JavaScript.class);
        if (jsAnnotation != null) {
            if (!response.has(DEPENDENCY_JAVASCRIPT)) {
                response.put(DEPENDENCY_JAVASCRIPT, new JSONArray());
            }
            JSONArray scriptsJson = response
                    .getJSONArray(DEPENDENCY_JAVASCRIPT);

            for (String uri : jsAnnotation.value()) {
                scriptsJson.put(manager.registerDependency(uri, cls));
            }
        }

        StyleSheet styleAnnotation = cls.getAnnotation(StyleSheet.class);
        if (styleAnnotation != null) {
            if (!response.has(DEPENDENCY_STYLESHEET)) {
                response.put(DEPENDENCY_STYLESHEET, new JSONArray());
            }
            JSONArray stylesJson = response.getJSONArray(DEPENDENCY_STYLESHEET);

            for (String uri : styleAnnotation.value()) {
                stylesJson.put(manager.registerDependency(uri, cls));
            }
        }

        HTML htmlAnnotation = cls.getAnnotation(HTML.class);
        if (htmlAnnotation != null) {
            if (!response.has(DEPENDENCY_HTML)) {
                response.put(DEPENDENCY_HTML, new JSONArray());
            }
            JSONArray htmlJson = response.getJSONArray(DEPENDENCY_HTML);

            for (String uri : htmlAnnotation.value()) {
                htmlJson.put(manager.registerDependency(uri, cls));
            }
        }

        if (Component.class.isAssignableFrom(cls.getSuperclass())) {
            handleDependencies(ui,
                    (Class<? extends ClientConnector>) cls.getSuperclass(),
                    response);
        }
    }

    private JsonArray toJsonArray(List<String> list) {
        JsonArray result = Json.createArray();
        for (int i = 0; i < list.size(); i++) {
            result.set(i, list.get(i));
        }

        return result;
    }

    /**
     * Adds the performance timing data (used by TestBench 3) to the UIDL
     * response.
     *
     * @throws IOException
     */
    private void writePerformanceData(UI ui, Writer writer) throws IOException {
        writer.write(String.format(", \"timings\":[%d, %d]",
                ui.getSession().getCumulativeRequestDuration(),
                ui.getSession().getLastRequestDuration()));
    }

    @SuppressWarnings("unchecked")
    public String getTag(ClientConnector clientConnector,
            LegacyCommunicationManager manager) {
        Class<? extends ClientConnector> clientConnectorClass = clientConnector
                .getClass();
        while (clientConnectorClass.isAnonymousClass()) {
            clientConnectorClass = (Class<? extends ClientConnector>) clientConnectorClass
                    .getSuperclass();
        }
        Class<?> clazz = clientConnectorClass;
        while (!usedClientConnectors.contains(clazz)
                && clazz.getSuperclass() != null
                && ClientConnector.class.isAssignableFrom(clazz)) {
            usedClientConnectors.add((Class<? extends ClientConnector>) clazz);
            clazz = clazz.getSuperclass();
        }
        return manager.getTagForType(clientConnectorClass);
    }

    public Collection<Class<? extends ClientConnector>> getUsedClientConnectors() {
        return usedClientConnectors;
    }

    private static final Logger getLogger() {
        return Logger.getLogger(UidlWriter.class.getName());
    }
}
