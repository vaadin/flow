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
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.annotations.Bower;
import com.vaadin.annotations.HTML;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.hummingbird.kernel.AbstractElementTemplate;
import com.vaadin.hummingbird.kernel.AttributeBinding;
import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.DynamicTextTemplate;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ForElementTemplate;
import com.vaadin.hummingbird.kernel.JsonConverter;
import com.vaadin.hummingbird.kernel.ModelAttributeBinding;
import com.vaadin.hummingbird.kernel.RootNode.PendingRpc;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.StaticTextTemplate;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChangeVisitor;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
import com.vaadin.hummingbird.parser.EventBinding;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.Constants;
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
            JsonObject response = Json.createObject();

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

            JsonArray changes = Json.createArray();
            JsonObject newTemplates = Json.createObject();

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

                private boolean isServerOnlyKey(Object key) {
                    if (key == null) {
                        return false;
                    } else if (key instanceof Class) {
                        return true;
                    } else {
                        return false;
                    }
                }

                private JsonObject createChange(StateNode node, String type) {
                    assert!isServerOnly(node);

                    JsonObject change = Json.createObject();
                    change.put("type", type);
                    // abs since currently detached nodes have -id
                    change.put("id", Math.abs(node.getId()));
                    changes.set(changes.length(), change);
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
                    if (isServerOnlyKey(key)) {
                        return;
                    }

                    JsonObject change = createChange(node, "remove");
                    assert key instanceof String || key instanceof Enum;
                    change.put("key", String.valueOf(key));
                }

                @Override
                public void visitPutChange(StateNode node,
                        PutChange putChange) {
                    if (isServerOnly(node)) {
                        return;
                    }
                    JsonObject change;
                    Object key = putChange.getKey();
                    if (isServerOnlyKey(key)) {
                        return;
                    }

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
                        change.put("value", JsonConverter.toJson(value));
                    }
                    assert key instanceof String || key instanceof Enum;
                    change.put("key", String.valueOf(key));

                    int length = changes.length();
                    if (length >= 2) {
                        JsonObject previousChange = changes
                                .getObject(changes.length() - 2);
                        if ("remove".equals(previousChange.getString("type"))
                                && change.getNumber("id") == previousChange
                                        .getNumber("id")
                                && key.equals(
                                        previousChange.getString("key"))) {
                            changes.remove(changes.length() - 2);
                        }
                    }
                }

                private void ensureTemplateSent(ElementTemplate template, UI ui,
                        JsonObject newTemplates) {

                    if (!ui.knowsTemplate(template)) {
                        newTemplates.put(Integer.toString(template.getId()),
                                serializeTemplate(template, ui, newTemplates));
                        ui.registerTemplate(template);
                    }
                }

                private JsonObject serializeTemplate(ElementTemplate template,
                        UI ui, JsonObject newTemplates) {
                    JsonObject serialized = Json.createObject();
                    serialized.put("type", template.getClass().getSimpleName());
                    serialized.put("id", template.getId());

                    if (template.getClass() == BoundElementTemplate.class) {
                        serializeBoundElementTemplate(serialized,
                                (BoundElementTemplate) template);
                    } else
                        if (template.getClass() == ForElementTemplate.class) {
                        serializeForTemplate(serialized,
                                (ForElementTemplate) template, ui,
                                newTemplates);
                    } else if (template
                            .getClass() == DynamicTextTemplate.class) {
                        serializeDynamicTextTemplate(serialized,
                                (DynamicTextTemplate) template, ui,
                                newTemplates);
                    } else
                        if (template.getClass() == StaticTextTemplate.class) {
                        serializeStaticTextTemplate(serialized,
                                (StaticTextTemplate) template, ui,
                                newTemplates);
                    } else {
                        throw new RuntimeException(template.toString());
                    }
                    return serialized;
                }

                private void serializeStaticTextTemplate(JsonObject serialized,
                        StaticTextTemplate template, UI ui,
                        JsonObject newTemplates) {
                    serialized.put("content", template.getContent());
                }

                private void serializeDynamicTextTemplate(JsonObject serialized,
                        DynamicTextTemplate template, UI ui,
                        JsonObject newTemplates) {
                    AttributeBinding binding = template.getBinding();
                    if (binding instanceof ModelAttributeBinding) {
                        ModelAttributeBinding mab = (ModelAttributeBinding) binding;
                        serialized.put("binding", mab.getPath().getFullPath());
                    } else {
                        throw new RuntimeException(binding.toString());
                    }
                }

                private void serializeForTemplate(JsonObject serialized,
                        ForElementTemplate template, UI ui,
                        JsonObject newTemplates) {
                    serialized.put("modelKey",
                            template.getModelProperty().getFullPath());
                    serialized.put("innerScope", template.getInnerScope());

                    serializeBoundElementTemplate(serialized, template);
                }

                private void serializeBoundElementTemplate(
                        JsonObject serialized, BoundElementTemplate bet) {
                    JsonObject attributeBindings = Json.createObject();
                    for (AttributeBinding attributeBinding : bet
                            .getAttributeBindings().values()) {
                        if (attributeBinding instanceof ModelAttributeBinding) {
                            ModelAttributeBinding mab = (ModelAttributeBinding) attributeBinding;
                            attributeBindings.put(mab.getPath().getFullPath(),
                                    mab.getAttributeName());
                        } else {
                            // Not yet supported
                            throw new RuntimeException(
                                    attributeBinding.toString());
                        }
                    }

                    List<BoundElementTemplate> childTemplates = bet
                            .getChildTemplates();
                    if (childTemplates != null) {
                        JsonArray children = Json.createArray();
                        serialized.put("children", children);
                        for (BoundElementTemplate childTemplate : childTemplates) {
                            ensureTemplateSent(childTemplate, ui, newTemplates);
                            children.set(children.length(),
                                    childTemplate.getId());
                        }
                    }

                    JsonObject defaultAttributes = Json.createObject();
                    bet.getDefaultAttributeValues()
                            .forEach(defaultAttributes::put);

                    JsonObject classPartBindings = Json.createObject();
                    bet.getClassPartBindings().forEach((key, binding) -> {
                        if (binding instanceof ModelAttributeBinding) {
                            ModelAttributeBinding mab = (ModelAttributeBinding) binding;
                            classPartBindings.put(mab.getPath().getFullPath(),
                                    key);
                        } else {
                            // Not yet supported
                            throw new RuntimeException(binding.toString());
                        }
                    });

                    if (classPartBindings.keys().length != 0) {
                        serialized.put("classPartBindings", classPartBindings);
                    }

                    Map<String, List<EventBinding>> events = bet.getEvents();
                    if (events != null && !events.isEmpty()) {
                        JsonObject eventsJson = Json.createObject();
                        events.forEach((type, list) -> {
                            JsonArray params = Json.createArray();
                            list.stream().map(EventBinding::getParams)
                                    .flatMap(Collection::stream)
                                    .filter(p -> !"element".equals(p))
                                    .distinct().forEach(p -> params
                                            .set(params.length(), p));

                            eventsJson.put(type, params);
                        });
                        serialized.put("events", eventsJson);
                    }

                    serialized.put("attributeBindings", attributeBindings);
                    serialized.put("defaultAttributes", defaultAttributes);
                    serialized.put("tag", bet.getTag());
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
                    Object key = listReplaceChange.getKey();
                    if (isServerOnlyKey(key)) {
                        return;
                    }

                    JsonObject change;
                    Object value = listReplaceChange.getNewValue();
                    if (value instanceof StateNode) {
                        change = createChange(node, "listReplaceNode");
                        change.put("value", ((StateNode) value).getId());
                    } else {
                        change = createChange(node, "listReplace");
                        change.put("value", JsonConverter.toJson(value));
                    }
                    change.put("index", listReplaceChange.getIndex());
                    assert key instanceof String || key instanceof Enum;
                    change.put("key", String.valueOf(key));
                }

                @Override
                public void visitListRemoveChange(StateNode node,
                        ListRemoveChange listRemoveChange) {
                    if (isServerOnly(node)) {
                        return;
                    }
                    Object key = listRemoveChange.getKey();
                    if (isServerOnlyKey(key)) {
                        return;
                    }

                    JsonObject change = createChange(node, "listRemove");
                    change.put("index", listRemoveChange.getIndex());
                    assert key instanceof String || key instanceof Enum;
                    change.put("key", String.valueOf(key));
                }

                @Override
                public void visitListInsertChange(StateNode node,
                        ListInsertChange listInsertChange) {
                    if (isServerOnly(node)) {
                        return;
                    }
                    Object key = listInsertChange.getKey();
                    if (isServerOnlyKey(key)) {
                        return;
                    }

                    JsonObject change;
                    Object value = listInsertChange.getValue();
                    if (value instanceof StateNode) {
                        change = createChange(node, "listInsertNode");
                        change.put("value", ((StateNode) value).getId());
                    } else {
                        change = createChange(node, "listInsert");
                        change.put("value", JsonConverter.toJson(value));
                    }
                    change.put("index", listInsertChange.getIndex());
                    assert key instanceof String || key instanceof Enum;
                    change.put("key", String.valueOf(key));
                }

                @Override
                public void visitIdChange(StateNode node, IdChange idChange) {
                    // Ignore
                }
            });
            response.put("elementTemplates", newTemplates);
            response.put("elementChanges", changes);

            List<PendingRpc> rpcQueue = ui.getRootNode().flushRpcQueue();
            if (!rpcQueue.isEmpty()) {
                response.put("rpc", encodeRpcQueue(rpcQueue));
            }

            String r = response.toString();
            writer.write(r.substring(1, r.length() - 1));
        } finally {
            uiConnectorTracker.setWritingResponse(false);
            uiConnectorTracker.cleanConnectorMap();
        }
    }

    private JsonArray encodeRpcQueue(List<PendingRpc> rpcQueue) {
        JsonArray array = Json.createArray();

        for (PendingRpc pendingRpc : rpcQueue) {
            JsonArray rpc = Json.createArray();
            rpc.set(0, pendingRpc.getJavascript());

            Object[] params = pendingRpc.getParams();
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];

                rpc.set(i + 1, serializeRpcParam(param));
            }

            array.set(array.length(), rpc);
        }

        return array;
    }

    private static JsonValue serializeRpcParam(Object param) {
        if (param instanceof Element) {
            Element element = (Element) param;

            JsonObject object = Json.createObject();
            object.put("node", element.getNode().getId());
            object.put("template", element.getTemplate().getId());

            return object;
        } else {
            return JsonConverter.toJson(param);
        }
    }

    /**
     * @since
     * @param cls
     * @param response
     */
    private void handleDependencies(UI ui, Class<? extends ClientConnector> cls,
            JsonObject response) {
        if (ui.getResourcesHandled().contains(cls)) {
            return;
        }

        ui.getResourcesHandled().add(cls);
        LegacyCommunicationManager manager = ui.getSession()
                .getCommunicationManager();

        JavaScript jsAnnotation = cls.getAnnotation(JavaScript.class);
        if (jsAnnotation != null) {
            if (!response.hasKey(DEPENDENCY_JAVASCRIPT)) {
                response.put(DEPENDENCY_JAVASCRIPT, Json.createArray());
            }
            JsonArray scriptsJson = response.getArray(DEPENDENCY_JAVASCRIPT);

            for (String uri : jsAnnotation.value()) {
                scriptsJson.set(scriptsJson.length(),
                        manager.registerDependency(uri, cls));
            }
        }

        StyleSheet styleAnnotation = cls.getAnnotation(StyleSheet.class);
        if (styleAnnotation != null) {
            if (!response.hasKey(DEPENDENCY_STYLESHEET)) {
                response.put(DEPENDENCY_STYLESHEET, Json.createArray());
            }
            JsonArray stylesJson = response.getArray(DEPENDENCY_STYLESHEET);

            for (String uri : styleAnnotation.value()) {
                stylesJson.set(stylesJson.length(),
                        manager.registerDependency(uri, cls));
            }
        }

        List<String> htmlResources = getHtmlResources(cls);
        if (!htmlResources.isEmpty()) {
            if (!response.hasKey(DEPENDENCY_HTML)) {
                response.put(DEPENDENCY_HTML, Json.createArray());
            }
            JsonArray htmlJson = response.getArray(DEPENDENCY_HTML);

            for (String uri : htmlResources) {
                htmlJson.set(htmlJson.length(),
                        manager.registerDependency(uri, cls));
            }
        }

        if (Component.class.isAssignableFrom(cls.getSuperclass())) {
            handleDependencies(ui,
                    (Class<? extends ClientConnector>) cls.getSuperclass(),
                    response);
        }
    }

    public static List<String> getHtmlResources(
            Class<? extends ClientConnector> cls) {

        List<String> resources = new ArrayList<>();

        HTML htmlAnnotation = cls.getAnnotation(HTML.class);
        Bower bowerAnnotation = cls.getAnnotation(Bower.class);

        if (htmlAnnotation != null) {
            for (String uri : htmlAnnotation.value()) {
                resources.add(uri);
            }
        }
        if (bowerAnnotation != null) {
            for (String bowerComponent : bowerAnnotation.value()) {
                String uri = Constants.BOWER_RESOURCE.replace("{0}",
                        bowerComponent);
                resources.add(uri);
            }
        }
        return resources;
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
