/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.JsonCodec;
import com.vaadin.flow.StateTree;
import com.vaadin.flow.change.MapPutChange;
import com.vaadin.flow.change.NodeAttachChange;
import com.vaadin.flow.change.NodeChange;
import com.vaadin.flow.nodefeature.ComponentMapping;
import com.vaadin.flow.nodefeature.TemplateMap;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.View;
import com.vaadin.flow.nodefeature.NodeProperties;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.flow.util.JsonUtils;
import com.vaadin.server.DependencyFilter;
import com.vaadin.server.DependencyFilter.FilterContext;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinUriResolverFactory;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.JsonConstants;
import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.common.DependencyList;
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
    private static final String COULD_NOT_READ_URL_CONTENTS_ERROR_MESSAGE = "Could not read url %s contents";

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
        getLogger().debug("* Creating response to client");

        int syncId = service.getDeploymentConfiguration().isSyncIdCheckEnabled()
                ? uiInternals.getServerSyncId()
                : -1;

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

        populateDependencies(response, session,
                uiInternals.getDependencyList());

        if (uiInternals.getConstantPool().hasNewConstants()) {
            response.put("constants",
                    uiInternals.getConstantPool().dumpConstants());
        }
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
        if (!ui.getSession().getService().getDeploymentConfiguration()
                .isProductionMode()) {
            response.put("timings", createPerformanceData(ui));
        }
        uiInternals.incrementServerId();
        return response;
    }

    private static void populateDependencies(JsonObject response,
            VaadinSession session, DependencyList dependencyList) {
        Collection<Dependency> pendingSendToClient = dependencyList
                .getPendingSendToClient();

        FilterContext context = new FilterContext(session);

        for (DependencyFilter filter : session.getService()
                .getDependencyFilters()) {
            pendingSendToClient = filter
                    .filter(new ArrayList<>(pendingSendToClient), context);
        }

        if (!pendingSendToClient.isEmpty()) {
            groupDependenciesByLoadMode(pendingSendToClient)
                    .forEach((loadMode, dependencies) -> response
                            .put(loadMode.name(), dependencies));
        }
        dependencyList.clearPendingSendToClient();
    }

    private static Map<LoadMode, JsonArray> groupDependenciesByLoadMode(
            Collection<Dependency> dependencies) {
        Map<LoadMode, JsonArray> result = new EnumMap<>(LoadMode.class);
        dependencies
                .forEach(dependency -> result.merge(dependency.getLoadMode(),
                        JsonUtils.createArray(dependencyToJson(dependency)),
                        JsonUtils.asArray().combiner()));
        return result;
    }

    private static JsonObject dependencyToJson(Dependency dependency) {
        JsonObject dependencyJson = dependency.toJson();
        if (dependency.getLoadMode() == LoadMode.INLINE) {
            dependencyJson.put(Dependency.KEY_CONTENTS,
                    getDependencyContents(dependency.getUrl()));
            dependencyJson.remove(Dependency.KEY_URL);
        }
        return dependencyJson;
    }

    private static String getDependencyContents(String url) {
        HttpServletRequest currentRequest = ((VaadinServletRequest) VaadinService
                .getCurrentRequest()).getHttpServletRequest();
        Charset requestCharset = Optional
                .ofNullable(currentRequest.getCharacterEncoding())
                .filter(string -> !string.isEmpty()).map(Charset::forName)
                .orElse(StandardCharsets.UTF_8);

        try (InputStream inlineResourceStream = getInlineResourceStream(url,
                currentRequest);
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(inlineResourceStream,
                                requestCharset))) {
            return bufferedReader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IllegalStateException(String
                    .format(COULD_NOT_READ_URL_CONTENTS_ERROR_MESSAGE, url), e);
        }
    }

    private static InputStream getInlineResourceStream(String url,
            HttpServletRequest currentRequest) {
        VaadinUriResolverFactory uriResolverFactory = VaadinSession.getCurrent()
                .getAttribute(VaadinUriResolverFactory.class);

        assert uriResolverFactory != null;

        String resolvedPath = uriResolverFactory
                .toServletContextPath(VaadinService.getCurrentRequest(), url);
        InputStream stream = currentRequest.getServletContext()
                .getResourceAsStream(resolvedPath);

        if (stream == null) {
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
        } else {
            getLogger().info("The path '{}' for inline resource has been successfully "
                            + "resolved to resource URL '{}'", url, resolvedPath);
        }
        return stream;
    }

    // non-private for testing purposes
    static JsonArray encodeExecuteJavaScriptList(
            List<JavaScriptInvocation> executeJavaScriptList) {
        return executeJavaScriptList.stream()
                .map(UidlWriter::encodeExecuteJavaScript)
                .collect(JsonUtils.asArray());
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
                .collect(JsonUtils.asArray());
    }

    /**
     * Encodes the state tree changes of the given UI. The runnables registered
     * at
     * {@link StateTree#beforeClientResponse(com.vaadin.flow.StateNode, Runnable)}
     * at evaluated before the changes are encoded.
     *
     * @param ui
     *            the UI
     * @param stateChanges
     *            a JSON array to put state changes into
     * @param templates
     *            a JSON object to put new template nodes into
     * @see StateTree#runExecutionsBeforeClientResponse()
     */
    private void encodeChanges(UI ui, JsonArray stateChanges,
            JsonObject templates) {
        UIInternals uiInternals = ui.getInternals();
        StateTree stateTree = uiInternals.getStateTree();

        stateTree.runExecutionsBeforeClientResponse();

        Consumer<TemplateNode> templateEncoder = new Consumer<TemplateNode>() {
            @Override
            public void accept(TemplateNode templateNode) {
                // Send to client if it's a new template
                if (!uiInternals.isTemplateSent(templateNode)) {
                    uiInternals.setTemplateSent(templateNode);
                    templates.put(Integer.toString(templateNode.getId()),
                            templateNode.toJson(this));
                }
            }
        };

        Set<Class<? extends Component>> componentsWithDependencies = new LinkedHashSet<>();
        stateTree.collectChanges(change -> {
            // Ensure new templates are sent to the client
            runIfNewTemplateChange(change, templateEncoder);

            if (attachesComponent(change)) {
                change.getNode().getFeature(ComponentMapping.class)
                        .getComponent()
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
        getParentViews(ui, component).stream().map(
                newClass -> newClass.<Component> asSubclass(Component.class))
                .forEach(hierarchyStorage::add);
        hierarchyStorage.add(component.getClass());
    }

    private List<Class<? extends HasChildView>> getParentViews(UI ui,
            Component component) {
        if (!ui.getRouterInterface().isPresent() || !(component instanceof View)) {
            return Collections.emptyList();
        }
        List<Class<? extends HasChildView>> parentViewsAscending = ui
                .getRouterInterface().get().getConfiguration()
                .getParentViewsAscending(
                        component.getClass().asSubclass(View.class))
                .filter(Component.class::isAssignableFrom)
                .collect(Collectors.toCollection(ArrayList::new));
        if (parentViewsAscending.size() > 1) {
            Collections.reverse(parentViewsAscending);
        }
        return parentViewsAscending;
    }

    private static void runIfNewTemplateChange(NodeChange change,
            Consumer<TemplateNode> consumer) {
        if (change instanceof MapPutChange) {
            MapPutChange put = (MapPutChange) change;
            if (put.getFeature() == TemplateMap.class
                    && put.getKey().equals(NodeProperties.ROOT_TEMPLATE_ID)) {
                Integer id = (Integer) put.getValue();
                TemplateNode templateNode = TemplateNode.get(id.intValue());

                consumer.accept(templateNode);
            }
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
