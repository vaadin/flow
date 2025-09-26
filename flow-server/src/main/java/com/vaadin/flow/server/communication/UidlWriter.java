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
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.change.NodeAttachChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Serializes pending server-side changes to UI state to JSON. This includes
 * shared state, client RPC invocations, connector hierarchy changes, connector
 * type information among others.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UidlWriter implements Serializable {
    private static final String COULD_NOT_READ_URL_CONTENTS_ERROR_MESSAGE = "Could not read url %s contents";

    /**
     * Provides context information for the resolve operations.
     */
    public static class ResolveContext implements Serializable {
        private VaadinService service;

        /**
         * Creates a new context.
         *
         * @param service
         *            the service which is resolving
         */
        public ResolveContext(VaadinService service) {
            this.service = Objects.requireNonNull(service);
        }

        /**
         * Gets the related Vaadin service.
         *
         * @return the service
         */
        public VaadinService getService() {
            return service;
        }

    }

    /**
     * Creates a JSON object containing all pending changes to the given UI.
     *
     * @param ui
     *            The {@link UI} whose changes to write
     * @param async
     *            True if this message is sent by the server asynchronously,
     *            false if it is a response to a client message
     * @param resync
     *            True iff the client should be asked to resynchronize
     * @return JSON object containing the UIDL response
     */
    public ObjectNode createUidl(UI ui, boolean async, boolean resync) {
        ObjectNode response = JacksonUtils.createObjectNode();

        UIInternals uiInternals = ui.getInternals();

        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();

        // Purge pending access calls as they might produce additional changes
        // to write out
        service.runPendingAccessTasks(session);

        // Paints components
        getLogger().debug("* Creating response to client");

        if (resync) {
            response.put(ApplicationConstants.RESYNCHRONIZE_ID, true);
        }
        int nextClientToServerMessageId = uiInternals
                .getLastProcessedClientToServerId() + 1;
        response.put(ApplicationConstants.CLIENT_TO_SERVER_ID,
                nextClientToServerMessageId);

        SystemMessages messages = service.getSystemMessages(ui.getLocale(),
                null);

        ObjectNode meta = new MetadataWriter().createMetadata(ui, false, async,
                messages);
        if (!JacksonUtils.getKeys(meta).isEmpty()) {
            response.set("meta", meta);
        }

        ArrayNode stateChanges = JacksonUtils.createArrayNode();

        encodeChanges(ui, stateChanges);

        populateDependencies(response, uiInternals.getDependencyList(),
                new ResolveContext(service));

        if (uiInternals.getConstantPool().hasNewConstants()) {
            response.set("constants",
                    uiInternals.getConstantPool().dumpConstants());
        }
        if (!stateChanges.isEmpty()) {
            response.set("changes", stateChanges);
        }

        List<PendingJavaScriptInvocation> executeJavaScriptList = uiInternals
                .dumpPendingJavaScriptInvocations();
        if (!executeJavaScriptList.isEmpty()) {
            response.set(JsonConstants.UIDL_KEY_EXECUTE,
                    encodeExecuteJavaScriptList(executeJavaScriptList));
        }
        if (service.getDeploymentConfiguration().isRequestTiming()) {
            response.set("timings", createPerformanceData(ui));
        }

        // Get serverSyncId after all changes has been computed, as push may
        // have been invoked, thus incrementing the counter.
        // This way the client will receive messages in the correct order
        int syncId = service.getDeploymentConfiguration().isSyncIdCheckEnabled()
                ? uiInternals.getServerSyncId()
                : -1;
        response.put(ApplicationConstants.SERVER_SYNC_ID, syncId);
        uiInternals.incrementServerId();
        return response;
    }

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
    public ObjectNode createUidl(UI ui, boolean async) {
        return createUidl(ui, async, false);
    }

    private static void populateDependencies(ObjectNode response,
            DependencyList dependencyList, ResolveContext context) {
        Collection<Dependency> pendingSendToClient = dependencyList
                .getPendingSendToClient();

        for (DependencyFilter filter : context.getService()
                .getDependencyFilters()) {
            pendingSendToClient = filter.filter(
                    new ArrayList<>(pendingSendToClient), context.getService());
        }

        if (!pendingSendToClient.isEmpty()) {
            groupDependenciesByLoadMode(pendingSendToClient, context)
                    .forEach((loadMode, dependencies) -> {
                        try {
                            response.set(loadMode.name(),
                                    JacksonUtils.getMapper()
                                            .readTree(dependencies.toString()));
                        } catch (JacksonException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        dependencyList.clearPendingSendToClient();
    }

    private static Map<LoadMode, ArrayNode> groupDependenciesByLoadMode(
            Collection<Dependency> dependencies, ResolveContext context) {
        Map<LoadMode, ArrayNode> result = new EnumMap<>(LoadMode.class);
        dependencies
                .forEach(dependency -> result.merge(dependency.getLoadMode(),
                        JacksonUtils.createArray(
                                dependencyToJson(dependency, context)),
                        JacksonUtils.asArray().combiner()));
        return result;
    }

    private static ObjectNode dependencyToJson(Dependency dependency,
            ResolveContext context) {
        ObjectNode dependencyJson = JacksonUtils
                .mapElemental(dependency.toJson());
        if (dependency.getLoadMode() == LoadMode.INLINE) {
            dependencyJson.put(Dependency.KEY_CONTENTS,
                    getDependencyContents(dependency.getUrl(), context));
            dependencyJson.remove(Dependency.KEY_URL);
        }
        return dependencyJson;
    }

    private static String getDependencyContents(String url,
            ResolveContext context) {
        try (InputStream inlineResourceStream = getInlineResourceStream(url,
                context)) {
            return IOUtils.toString(inlineResourceStream,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(String
                    .format(COULD_NOT_READ_URL_CONTENTS_ERROR_MESSAGE, url), e);
        }
    }

    private static InputStream getInlineResourceStream(String url,
            ResolveContext context) {
        VaadinService service = context.getService();
        InputStream stream = service.getResourceAsStream(url);

        if (stream == null) {
            String resolvedPath = service.resolveResource(url);
            getLogger().warn("The path '{}' for inline resource "
                    + "has been resolved to '{}'. "
                    + "But resource is not available via the servlet context. "
                    + "Trying to load '{}' as a URL", url, resolvedPath, url);
            try {
                stream = new URL(url).openConnection().getInputStream();
            } catch (MalformedURLException exception) {
                throw new IllegalStateException(String.format(
                        "The path '%s' is not a valid URL. "
                                + "Unable to fetch a resource addressed by it.",
                        url), exception);
            } catch (IOException e) {
                throw new IllegalStateException(String.format(
                        COULD_NOT_READ_URL_CONTENTS_ERROR_MESSAGE, url), e);
            }
        } else if (getLogger().isDebugEnabled()) {
            String resolvedPath = service.resolveResource(url);
            getLogger().debug(
                    "The path '{}' for inline resource has been successfully "
                            + "resolved to resource URL '{}'",
                    url, resolvedPath);
        }

        return stream;
    }

    // non-private for testing purposes
    static ArrayNode encodeExecuteJavaScriptList(
            List<PendingJavaScriptInvocation> executeJavaScriptList) {
        return executeJavaScriptList.stream()
                .map(UidlWriter::encodeExecuteJavaScript)
                .collect(JacksonUtils.asArray());
    }

    private static ReturnChannelRegistration createReturnValueChannel(
            StateNode owner, List<ReturnChannelRegistration> registrations,
            SerializableConsumer<JsonNode> action) {
        ReturnChannelRegistration channel = owner
                .getFeature(ReturnChannelMap.class)
                .registerChannel(arguments -> {
                    registrations.forEach(ReturnChannelRegistration::remove);

                    action.accept(arguments.get(0));
                });

        registrations.add(channel);

        return channel;
    }

    private static ArrayNode encodeExecuteJavaScript(
            PendingJavaScriptInvocation invocation) {
        List<Object> parametersList = invocation.getInvocation()
                .getParameters();

        Stream<Object> parameters = parametersList.stream();
        String expression = invocation.getInvocation().getExpression();

        if (invocation.isSubscribed()) {
            StateNode owner = invocation.getOwner();

            List<ReturnChannelRegistration> channels = new ArrayList<>();

            ReturnChannelRegistration successChannel = createReturnValueChannel(
                    owner, channels, invocation::complete);
            ReturnChannelRegistration errorChannel = createReturnValueChannel(
                    owner, channels, invocation::completeExceptionally);

            // Inject both channels as new parameters
            parameters = Stream.concat(parameters,
                    Stream.of(successChannel, errorChannel));
            int successIndex = parametersList.size();
            int errorIndex = successIndex + 1;

            /*
             * Run the original expression wrapped in a function to capture any
             * return statement. Pass the return value through Promise.resolve
             * which resolves regular values immediately and waits for thenable
             * values. Call either of the handlers once the promise completes.
             * If the expression throws synchronously, run the error handler.
             */
            //@formatter:off
            expression =
                  "try{"
                +   "Promise.resolve((async function(){"
                +     expression
                +   "})()).then($"+successIndex+",function(error){$"+errorIndex+"(''+error)})"
                + "}catch(error){"
                +   "$"+errorIndex+"(''+error)"
                + "}";
            //@formatter:on
        }

        // [argument1, argument2, ..., script]
        return Stream
                .concat(parameters.map(JacksonCodec::encodeWithTypeInfo),
                        Stream.of(JacksonUtils.createNode(expression)))
                .collect(JacksonUtils.asArray());
    }

    /**
     * Encodes the state tree changes of the given UI. The executions registered
     * at
     * {@link StateTree#beforeClientResponse(com.vaadin.flow.internal.StateNode, com.vaadin.flow.function.SerializableConsumer)}
     * at evaluated before the changes are encoded.
     *
     * @param ui
     *            the UI
     * @param stateChanges
     *            a JSON array to put state changes into
     * @see StateTree#runExecutionsBeforeClientResponse()
     */
    private void encodeChanges(UI ui, ArrayNode stateChanges) {
        UIInternals uiInternals = ui.getInternals();
        StateTree stateTree = uiInternals.getStateTree();

        stateTree.runExecutionsBeforeClientResponse();

        Set<Class<? extends Component>> componentsWithDependencies = new LinkedHashSet<>();
        Consumer<NodeChange> changesCollector = change -> {
            if (attachesComponent(change)) {
                ComponentMapping.getComponent(change.getNode())
                        .ifPresent(component -> addComponentHierarchy(ui,
                                componentsWithDependencies, component));
            }

            // Encode the actual change
            stateChanges.add(change.toJson(uiInternals.getConstantPool()));
        };
        // A collectChanges round may add additional changes that needs to be
        // collected.
        // For example NodeList.generateChangesFromEmpty adds a ListClearChange
        // in case of remove has been invoked previously
        // Usually, at most 2 rounds should be necessary, so stop checking after
        // five attempts to avoid infinite loops in case of bugs.
        int attempts = 5;
        while (stateTree.hasDirtyNodes() && attempts-- > 0) {
            stateTree.collectChanges(changesCollector);
        }
        if (stateTree.hasDirtyNodes()) {
            getLogger().warn("UI still dirty after collecting changes, "
                    + "this should not happen and may cause unexpected PUSH invocation.");
        }

        componentsWithDependencies
                .forEach(uiInternals::addComponentDependencies);
    }

    private static boolean attachesComponent(NodeChange change) {
        return change instanceof NodeAttachChange
                && change.getNode().hasFeature(ComponentMapping.class);
    }

    private void addComponentHierarchy(UI ui,
            Set<Class<? extends Component>> hierarchyStorage,
            Component component) {
        hierarchyStorage.add(component.getClass());
        if (component instanceof Composite) {
            addComponentHierarchy(ui, hierarchyStorage,
                    ((Composite<?>) component).getContent());
        }
    }

    /**
     * Adds the performance timing data (used by TestBench 3) to the UIDL
     * response.
     */
    private ArrayNode createPerformanceData(UI ui) {
        ArrayNode timings = JacksonUtils.createArrayNode();
        timings.add(ui.getSession().getCumulativeRequestDuration());
        timings.add(ui.getSession().getLastRequestDuration());
        return timings;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(UidlWriter.class.getName());
    }
}
