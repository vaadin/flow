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
package com.vaadin.flow.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouterUtil;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Utility methods used by the BootstrapHandler.
 */
class BootstrapUtils {

    private BootstrapUtils() {
    }

    /**
     * Returns the specified viewport content for the target route chain that
     * was navigated to, specified with {@link Viewport} on the {@link Route}
     * annotated class or the {@link ParentLayout} of the route.
     *
     * @param context
     *            the bootstrap context
     * @return the content value string for viewport meta tag
     */
    static Optional<String> getViewportContent(
            BootstrapHandler.BootstrapContext context) {

        Optional<NavigationState> navigationTarget = getNavigationState(
                context);
        if (!navigationTarget.isPresent()) {
            return Optional.empty();
        }

        return navigationTarget.flatMap(BootstrapUtils::getViewportAnnotation)
                .map(Viewport::value);
    }

    private static Optional<Viewport> getViewportAnnotation(
            NavigationState state) {

        Class<? extends RouterLayout> parentLayout = RouterUtil
                .getTopParentLayout(state.getNavigationTarget(),
                        state.getResolvedPath());

        if (parentLayout == null) {
            return AnnotationReader.getAnnotationFor(
                    state.getNavigationTarget(), Viewport.class);
        }
        return AnnotationReader.getAnnotationFor(parentLayout, Viewport.class);
    }

    /**
     * Get initial page settings if a {@link PageConfigurator} is found for the
     * current component tree after navigation has resolved.
     *
     * @param context
     *            the bootstrap context
     * @return initial page settings or empty optional if no
     *         {@link PageConfigurator} found
     */
    static Optional<InitialPageSettings> getInitialPageSettings(
            BootstrapHandler.BootstrapContext context) {
        UI ui = context.getUI();

        Optional<PageConfigurator> pageConfigurator = ui.getChildren()
                .filter(component -> component instanceof PageConfigurator)
                .map(component -> (PageConfigurator) component).findFirst();
        if (pageConfigurator.isPresent()) {
            InitialPageSettings settings = createInitialPageSettingsObject(
                    context);

            pageConfigurator.get().configurePage(settings);

            return Optional.of(settings);
        }

        return Optional.empty();
    }

    private static InitialPageSettings createInitialPageSettingsObject(
            BootstrapHandler.BootstrapContext context) {
        UI ui = context.getUI();
        VaadinRequest request = context.getRequest();
        WebBrowser browser = context.getSession().getBrowser();

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "";
        } else {
            assert pathInfo.startsWith("/");
            pathInfo = pathInfo.substring(1);
        }
        Optional<Router> router = ui.getRouter();
        NavigationEvent navigationEvent = new NavigationEvent(
                router.isPresent() ? router.get() : null,
                new Location(pathInfo,
                        QueryParameters.full(request.getParameterMap())),
                ui, NavigationTrigger.PAGE_LOAD);

        List<HasElement> components = ui.getChildren()
                .map(component -> (HasElement) component)
                .collect(Collectors.toList());

        AfterNavigationEvent afterNavigationEvent = new AfterNavigationEvent(
                RouterUtil.createEvent(navigationEvent, components));

        return new InitialPageSettings(request, ui, afterNavigationEvent,
                browser);
    }

    /**
     * Returns the specified body size content for the target route chain that
     * was navigated to, specified with {@link BodySize} on the {@link Route}
     * annotated class or the {@link ParentLayout} of the route.
     *
     * @param context
     *            the bootstrap context
     * @return the content value string for body size style element
     */
    static Optional<String> getBodySizeContent(
            BootstrapHandler.BootstrapContext context) {
        Optional<NavigationState> navigationTarget = getNavigationState(
                context);

        if (!navigationTarget.isPresent()) {
            return Optional.empty();
        }

        return navigationTarget.flatMap(BootstrapUtils::getBodySizeAnnotation)
                .map(BootstrapUtils::composeBodySizeString);
    }

    private static Optional<BodySize> getBodySizeAnnotation(
            NavigationState state) {

        Class<? extends RouterLayout> parentLayout = RouterUtil
                .getTopParentLayout(state.getNavigationTarget(),
                        state.getResolvedPath());

        if (parentLayout == null) {
            return AnnotationReader.getAnnotationFor(
                    state.getNavigationTarget(), BodySize.class);
        }
        return AnnotationReader.getAnnotationFor(parentLayout, BodySize.class);
    }

    private static String composeBodySizeString(BodySize bodySize) {
        StringBuilder bodyString = new StringBuilder();
        bodyString.append("body {");
        if (!bodySize.height().isEmpty()) {
            bodyString.append("height:").append(bodySize.height()).append(";");
        }
        if (!bodySize.width().isEmpty()) {
            bodyString.append("width:").append(bodySize.width()).append(";");
        }
        bodyString.append("margin:0;");
        bodyString.append("}");
        return bodyString.toString();
    }

    /**
     * Returns the specified viewport content for the target route chain that
     * was navigated to, specified with {@link Inline} on the {@link Route}
     * annotated class or the {@link ParentLayout} of the route.
     *
     * @param context
     *            the bootstrap context
     * @return the content value string for viewport meta tag
     */
    static Optional<InlineTargets> getInlineTargets(
            BootstrapHandler.BootstrapContext context) {
        Optional<NavigationState> navigationTarget = getNavigationState(
                context);

        if (navigationTarget.isPresent()) {
            List<Inline> inlineAnnotations = getInlineAnnotations(
                    navigationTarget.get());
            if (!inlineAnnotations.isEmpty()) {
                InlineTargets inlines = new InlineTargets();
                inlineAnnotations.forEach(inline -> inlines
                        .addInlineDependency(inline, context.getRequest()));
                return Optional.of(inlines);
            }
        }

        return Optional.empty();
    }

    private static List<Inline> getInlineAnnotations(NavigationState state) {

        Class<? extends RouterLayout> parentLayout = RouterUtil
                .getTopParentLayout(state.getNavigationTarget(),
                        state.getResolvedPath());

        if (parentLayout == null) {
            return AnnotationReader.getAnnotationsFor(
                    state.getNavigationTarget(), Inline.class);
        }
        return AnnotationReader.getAnnotationsFor(parentLayout, Inline.class);
    }

    /**
     *
     * Read the contents of the given file from the classpath.
     *
     * @param request
     *            the request for the ui
     * @param file
     *            target file to read contents for
     * @return file contents as a {@link String}
     */
    static String getDependencyContents(VaadinRequest request, String file) {
        Charset requestCharset = Optional
                .ofNullable(request.getCharacterEncoding())
                .filter(string -> !string.isEmpty()).map(Charset::forName)
                .orElse(StandardCharsets.UTF_8);

        try (InputStream inlineResourceStream = getInlineResourceStream(request,
                file);
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(inlineResourceStream,
                                requestCharset))) {
            return bufferedReader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Could not read file %s contents", file), e);
        }
    }

    private static InputStream getInlineResourceStream(VaadinRequest request,
            String file) {
        InputStream stream = request.getService().getClassLoader()
                .getResourceAsStream(file);

        if (stream == null) {
            throw new IllegalStateException(String.format(
                    "File '%s' for inline resource is not available through "
                            + "the servlet context class loader.",
                    file));
        }
        return stream;
    }

    private static Optional<NavigationState> getRouteTargetInformation(
            VaadinRequest request, Router router) {
        return router.resolveNavigationTarget(request.getPathInfo(),
                request.getParameterMap());
    }

    static List<JsonObject> getThemeSettings(
            BootstrapHandler.BootstrapContext context) {

        Optional<Theme> themeAnnotation = getThemeAnnotation(context);

        if (themeAnnotation.isPresent()) {
            AbstractTheme theme = ReflectTools
                    .createInstance(themeAnnotation.get().value());
            return theme.getInlineContents().stream()
                    .map(BootstrapUtils::createInlineDependencyObject)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static JsonObject createInlineDependencyObject(String content) {
        JsonObject dependency = Json.createObject();
        dependency.put(Dependency.KEY_TYPE, "none");
        dependency.put("LoadMode", LoadMode.INLINE.toString());

        dependency.put(Dependency.KEY_CONTENTS, content);
        return dependency;
    }

    private static Optional<Theme> getThemeAnnotation(
            BootstrapHandler.BootstrapContext context) {
        Optional<NavigationState> navigationTarget = getNavigationState(
                context);

        if (!navigationTarget.isPresent()) {
            return Optional.empty();
        }

        NavigationState state = navigationTarget.get();

        Class<? extends RouterLayout> parentLayout = RouterUtil
                .getTopParentLayout(state.getNavigationTarget(),
                        state.getResolvedPath());

        if (parentLayout == null) {
            return AnnotationReader
                    .getAnnotationFor(state.getNavigationTarget(), Theme.class);
        }
        return AnnotationReader.getAnnotationFor(parentLayout, Theme.class);
    }

    private static Optional<NavigationState> getNavigationState(
            BootstrapHandler.BootstrapContext context) {
        UI ui = context.getUI();
        VaadinRequest request = context.getRequest();

        Optional<Router> router = ui.getRouter();
        if (!router.isPresent()) {
            return Optional.empty();
        }
        Optional<NavigationState> navigationTarget = getRouteTargetInformation(
                request, router.get());
        return navigationTarget;
    }

}
