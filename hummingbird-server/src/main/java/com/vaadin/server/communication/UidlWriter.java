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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.annotations.Bower;
import com.vaadin.annotations.HTML;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.NotYetImplemented;
import com.vaadin.annotations.PolymerStyle;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.JsonConverter;
import com.vaadin.hummingbird.kernel.RootNode.PendingRpc;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.change.NodeChange;
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
    private static final String DEPENDENCY_POLYMER_STYLE = "polymerStyleDependencies";
    private static final String DEPENDENCY_HTML = "htmlDependencies";
    private final Set<Class<? extends ClientConnector>> usedClientConnectors = new HashSet<Class<? extends ClientConnector>>();

    @FunctionalInterface
    public interface Processor<T> {
        public T process(T input, UI ui);

        public static <T> T processChain(T value,
                Collection<Processor<T>> processors, UI ui) {
            for (Processor<T> processor : processors) {
                value = processor.process(value, ui);
            }
            return value;
        }
    }

    private static final List<Processor<LinkedHashMap<StateNode, List<NodeChange>>>> changeProcessors = Arrays
            .asList(TransactionLogOptimizer::optimizeChanges,
                    TransactionLogPruner::removeChangesFromClient,
                    TransactionLogPruner::removeClientComputed);
    private static final List<Processor<Set<ElementTemplate>>> templateProcessors = Arrays
            .asList(TransactionLogOptimizer::optimizeTemplates);

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

            if (async) {
                JsonObject meta = Json.createObject();
                meta.put("async", true);
                response.put("meta", meta);
            }

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

            Set<Class<? extends ClientConnector>> dependencyClasses = new HashSet<>();
            for (ClientConnector c : dirtyVisibleConnectors) {
                dependencyClasses.add(c.getClass());
            }

            List<Dependency> deps = collectDependencies(ui, dependencyClasses);
            writeDependencies(ui, deps, response);
            encodeChanges(ui, response);

            encodeRpc(ui, response);

            String r = response.toString();
            if (getLogger().isLoggable(Level.FINE)) {
                getLogger().fine("Sending UIDL " + r);
            }
            writer.write(r.substring(1, r.length() - 1));
        } finally {
            uiConnectorTracker.setWritingResponse(false);
            uiConnectorTracker.cleanConnectorMap();
        }
    }

    private void writeDependencies(UI ui, List<Dependency> deps,
            JsonObject response) {
        LegacyCommunicationManager manager = ui.getSession()
                .getCommunicationManager();

        for (Dependency d : deps) {
            JsonArray json;

            if (d.getType() == Dependency.Type.SCRIPT) {
                if (!response.hasKey(DEPENDENCY_JAVASCRIPT)) {
                    response.put(DEPENDENCY_JAVASCRIPT, Json.createArray());
                }
                json = response.getArray(DEPENDENCY_JAVASCRIPT);
            } else if (d.getType() == Dependency.Type.HTML) {
                if (!response.hasKey(DEPENDENCY_HTML)) {
                    response.put(DEPENDENCY_HTML, Json.createArray());
                }
                json = response.getArray(DEPENDENCY_HTML);
            } else if (d.getType() == Dependency.Type.STYLESHEET) {
                if (!response.hasKey(DEPENDENCY_STYLESHEET)) {
                    response.put(DEPENDENCY_STYLESHEET, Json.createArray());
                }
                json = response.getArray(DEPENDENCY_STYLESHEET);
            } else if (d.getType() == Dependency.Type.POLYMER_STYLE) {
                if (!response.hasKey(DEPENDENCY_POLYMER_STYLE)) {
                    response.put(DEPENDENCY_POLYMER_STYLE, Json.createArray());
                }
                json = response.getArray(DEPENDENCY_POLYMER_STYLE);
            } else {
                throw new IllegalStateException("Unknown type: " + d.getType());
            }

            json.set(json.length(), d.getUrl());
        }
    }

    public static List<Dependency> collectDependencies(UI ui,
            Set<Class<? extends ClientConnector>> classes) {
        List<Class<? extends ClientConnector>> unhandledClasses = new ArrayList<>();
        for (Class<? extends ClientConnector> cls : classes) {
            if (!ui.getResourcesHandled().contains(cls)) {
                unhandledClasses.add(cls);

                NotYetImplemented annotation = cls
                        .getAnnotation(NotYetImplemented.class);
                if (annotation != null) {
                    String msg = "Using " + cls.getName()
                            + " which has not yet been implemented properly";
                    if (annotation.value().length() > 0) {
                        msg += ": " + annotation.value();
                    }
                    getLogger().warning(msg);
                }
            }
        }

        // get added dynamic dependencies from UI
        List<Dependency> dynamicDependencies = ui.getDynamicDependencies();

        if (unhandledClasses.isEmpty()) {
            return dynamicDependencies == null ? Collections.emptyList()
                    : dynamicDependencies;
        }

        /*
         * Ensure super classes come before sub classes to get script dependency
         * order right. Sub class @JavaScript might assume that
         *
         * @JavaScript defined by super class is already loaded.
         */
        Collections.sort(unhandledClasses, (o1, o2) -> {
            if (o1.isAssignableFrom(o2)) {
                return -1;
            } else if (o2.isAssignableFrom(o1)) {
                return 1;
            }
            if (UI.class.isAssignableFrom(o1)) {
                return -1;
            } else if (UI.class.isAssignableFrom(o2)) {
                return 1;
            }

            return 0;
        });

        List<Dependency> collectedDependencies = new ArrayList<>();
        LegacyCommunicationManager manager = ui.getSession()
                .getCommunicationManager();
        for (Class<? extends ClientConnector> cls : unhandledClasses) {
            collectDependencies(ui, cls, collectedDependencies, manager);
        }
        if (dynamicDependencies != null) {
            collectedDependencies.addAll(dynamicDependencies);
        }
        return collectedDependencies;
    }

    private static void collectDependencies(UI ui,
            Class<? extends ClientConnector> cls, List<Dependency> dependencies,
            LegacyCommunicationManager manager) {

        if (ui.getResourcesHandled().contains(cls)) {
            return;
        }

        getLogger().fine("Collecting dependencies for " + cls.getName());
        ui.getResourcesHandled().add(cls);

        JavaScript jsAnnotation = cls.getAnnotation(JavaScript.class);
        if (jsAnnotation != null) {
            for (String uri : jsAnnotation.value()) {
                Dependency dependency = new Dependency(Dependency.Type.SCRIPT,
                        manager.registerDependency(uri, cls));
                dependencies.add(dependency);
                getLogger().fine("Dependency found: " + dependency);
            }
        }

        StyleSheet styleAnnotation = cls.getAnnotation(StyleSheet.class);
        if (styleAnnotation != null) {
            for (String uri : styleAnnotation.value()) {
                Dependency dependency = new Dependency(
                        Dependency.Type.STYLESHEET,
                        manager.registerDependency(uri, cls));
                dependencies.add(dependency);
                getLogger().fine("Dependency found: " + dependency);
            }
        }

        PolymerStyle polymerStyleAnnotation = cls
                .getAnnotation(PolymerStyle.class);
        if (polymerStyleAnnotation != null) {
            for (String moduleId : polymerStyleAnnotation.value()) {
                Dependency dependency = new Dependency(
                        Dependency.Type.POLYMER_STYLE, moduleId);
                dependencies.add(dependency);
                getLogger().fine("Dependency found: " + dependency);
            }
        }

        List<String> htmlResources = getHtmlResources(cls);
        if (!htmlResources.isEmpty()) {

            for (String uri : htmlResources) {
                Dependency dependency = new Dependency(Dependency.Type.HTML,
                        manager.registerDependency(uri, cls));
                dependencies.add(dependency);
                getLogger().fine("Dependency found: " + dependency);
            }
        }

        if (Component.class.isAssignableFrom(cls.getSuperclass())) {
            collectDependencies(ui,
                    (Class<? extends ClientConnector>) cls.getSuperclass(),
                    dependencies, manager);
        }
    }

    public static class Dependency {
        public enum Type {
            SCRIPT, HTML, STYLESHEET, POLYMER_STYLE
        };

        private Type type;
        private String url;

        public Dependency(Type type, String url) {
            this.type = type;
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return "Dependency [type=" + type + ", url=" + url + "]";
        }

    }

    public static void encodeRpc(UI ui, JsonObject response) {
        List<PendingRpc> rpcQueue = ui.getRootNode().flushRpcQueue();
        if (!rpcQueue.isEmpty()) {
            response.put("rpc", encodeRpcQueue(rpcQueue));
        }
    }

    public static void encodeChanges(UI ui, JsonObject response) {
        TransactionLogBuilder logBuilder = new TransactionLogBuilder();
        ui.getRootNode().commit(logBuilder.getVisitor());

        LinkedHashMap<StateNode, List<NodeChange>> changes = Processor
                .processChain(logBuilder.getChanges(), changeProcessors, ui);

        Set<ElementTemplate> templates = Processor.processChain(
                logBuilder.getTemplates(), templateProcessors, ui);

        TransactionLogJsonProducer jsonProducer = new TransactionLogJsonProducer(
                ui, changes, templates);

        response.put("elementTemplates", jsonProducer.getTemplatesJson());
        response.put("elementChanges", jsonProducer.getChangesJson());
    }

    private static JsonArray encodeRpcQueue(List<PendingRpc> rpcQueue) {
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
        } else if (param instanceof StateNode) {
            StateNode node = (StateNode) param;
            return Json.create(node.getId());
        } else {
            return JsonConverter.toJson(param);
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
