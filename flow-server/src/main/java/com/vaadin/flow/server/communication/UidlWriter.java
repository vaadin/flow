/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.stream.Stream;

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
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.change.NodeAttachChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.DependencyFilter.FilterContext;
import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.theme.AbstractTheme;

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
 * @since 1.0
 */
public class UidlWriter implements Serializable {
    private static final String COULD_NOT_READ_URL_CONTENTS_ERROR_MESSAGE = "Could not read url %s contents";

    /**
     * Provides context information for the resolve operations.
     */
    public static class ResolveContext implements Serializable {
        private VaadinService service;
        private WebBrowser browser;
        private AbstractTheme theme;

        /**
         * Creates a new context.
         *
         * @param service
         *            the service which is resolving
         * @param browser
         *            the browser
         * @param theme
         *            the theme, or <code>null</code> for no theme
         */
        public ResolveContext(VaadinService service, WebBrowser browser,
                AbstractTheme theme) {
            this.service = Objects.requireNonNull(service);
            this.browser = Objects.requireNonNull(browser);
            this.theme = theme;
        }

        /**
         * Gets the related Vaadin service.
         *
         * @return the service
         */
        public VaadinService getService() {
            return service;
        }

        /**
         * Gets the browser info used for resolving.
         *
         * @return the browser
         */
        public WebBrowser getBrowser() {
            return browser;
        }

        /**
         * Gets the theme used for resolving.
         *
         * @return the theme or <code>null</code> for no theme
         */
        public AbstractTheme getTheme() {
            return theme;
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
    public JsonObject createUidl(UI ui, boolean async, boolean resync) {
        JsonObject response = Json.createObject();

        UIInternals uiInternals = ui.getInternals();

        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();

        // Purge pending access calls as they might produce additional changes
        // to write out
        service.runPendingAccessTasks(session);

        // Paints components
        getLogger().debug("* Creating response to client");

        int syncId = service.getDeploymentConfiguration().isSyncIdCheckEnabled()
                ? uiInternals.getServerSyncId() : -1;

        response.put(ApplicationConstants.SERVER_SYNC_ID, syncId);
        if (resync) {
            response.put(ApplicationConstants.RESYNCHRONIZE_ID, true);
        }
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

        encodeChanges(ui, stateChanges);

        populateDependencies(response, uiInternals.getDependencyList(),
                new ResolveContext(service, session.getBrowser(), null));

        if (uiInternals.getConstantPool().hasNewConstants()) {
            response.put("constants",
                    uiInternals.getConstantPool().dumpConstants());
        }
        if (stateChanges.length() != 0) {
            response.put("changes", stateChanges);
        }

        List<PendingJavaScriptInvocation> executeJavaScriptList = uiInternals
                .dumpPendingJavaScriptInvocations();
        if (!executeJavaScriptList.isEmpty()) {
            response.put(JsonConstants.UIDL_KEY_EXECUTE,
                    encodeExecuteJavaScriptList(executeJavaScriptList));
        }
        if (ui.getSession().getService().getDeploymentConfiguration()
                .isRequestTiming()) {
            response.put("timings", createPerformanceData(ui));
        }
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
    public JsonObject createUidl(UI ui, boolean async) {
        return createUidl(ui, async, false);
    }

    private static void populateDependencies(JsonObject response,
            DependencyList dependencyList, ResolveContext context) {
        Collection<Dependency> pendingSendToClient = dependencyList
                .getPendingSendToClient();

        FilterContext filterContext = new FilterContext(context.getService(),
                context.getBrowser());

        for (DependencyFilter filter : context.getService()
                .getDependencyFilters()) {
            pendingSendToClient = filter.filter(
                    new ArrayList<>(pendingSendToClient), filterContext);
        }

        if (!pendingSendToClient.isEmpty()) {
            groupDependenciesByLoadMode(pendingSendToClient, context)
                    .forEach((loadMode, dependencies) -> response
                            .put(loadMode.name(), dependencies));
        }
        dependencyList.clearPendingSendToClient();
    }

    private static Map<LoadMode, JsonArray> groupDependenciesByLoadMode(
            Collection<Dependency> dependencies, ResolveContext context) {
        Map<LoadMode, JsonArray> result = new EnumMap<>(LoadMode.class);
        dependencies
                .forEach(dependency -> result.merge(dependency.getLoadMode(),
                        JsonUtils.createArray(
                                dependencyToJson(dependency, context)),
                        JsonUtils.asArray().combiner()));
        return result;
    }

    private static JsonObject dependencyToJson(Dependency dependency,
            ResolveContext context) {
        JsonObject dependencyJson = dependency.toJson();
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
        WebBrowser browser = context.getBrowser();
        InputStream stream = service.getResourceAsStream(url, browser,
                context.getTheme());

        if (stream == null) {
            String resolvedPath = service.resolveResource(url, browser);
            getLogger().warn(
                    "The path '{}' for inline resource "
                            + "has been resolved to '{}'. "
                            + "But resource is not available via the servlet context. "
                            + "Trying to load '{}' as a URL",
                    url, resolvedPath, url);
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
            String resolvedPath = service.resolveResource(url, browser);
            getLogger().debug(
                    "The path '{}' for inline resource has been successfully "
                            + "resolved to resource URL '{}'",
                    url, resolvedPath);
        }

        return stream;
    }

    // non-private for testing purposes
    static JsonArray encodeExecuteJavaScriptList(
            List<PendingJavaScriptInvocation> executeJavaScriptList) {
        return executeJavaScriptList.stream()
                .map(UidlWriter::encodeExecuteJavaScript)
                .collect(JsonUtils.asArray());
    }

    private static ReturnChannelRegistration createReturnValueChannel(
            StateNode owner, List<ReturnChannelRegistration> registrations,
            SerializableConsumer<JsonValue> action) {
        ReturnChannelRegistration channel = owner
                .getFeature(ReturnChannelMap.class)
                .registerChannel(arguments -> {
                    registrations.forEach(ReturnChannelRegistration::remove);

                    action.accept(arguments.get(0));
                });

        registrations.add(channel);

        return channel;
    }

    private static JsonArray encodeExecuteJavaScript(
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
                +   "Promise.resolve((function(){"
                +     expression
                +   "})()).then($"+successIndex+",function(error){$"+errorIndex+"(''+error)})"
                + "}catch(error){"
                +   "$"+errorIndex+"(''+error)"
                + "}";
            //@formatter:on
        }

        // [argument1, argument2, ..., script]
        return Stream
                .concat(parameters.map(JsonCodec::encodeWithTypeInfo),
                        Stream.of(Json.create(expression)))
                .collect(JsonUtils.asArray());
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
    private void encodeChanges(UI ui, JsonArray stateChanges) {
        UIInternals uiInternals = ui.getInternals();
        StateTree stateTree = uiInternals.getStateTree();

        stateTree.runExecutionsBeforeClientResponse();

        Set<Class<? extends Component>> componentsWithDependencies = new LinkedHashSet<>();
        stateTree.collectChanges(change -> {
            if (attachesComponent(change)) {
                ComponentMapping.getComponent(change.getNode())
                        .ifPresent(component -> addComponentHierarchy(ui,
                                componentsWithDependencies, component));
            }

            // Encode the actual change
            stateChanges.set(stateChanges.length(),
                    change.toJson(uiInternals.getConstantPool()));
        });

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
    private JsonValue createPerformanceData(UI ui) {
        JsonArray timings = Json.createArray();
        timings.set(0, ui.getSession().getCumulativeRequestDuration());
        timings.set(1, ui.getSession().getLastRequestDuration());
        return timings;
    }

    private static final Logger getLogger() {
        return LoggerFactory.getLogger(UidlWriter.class.getName());
    }
}
