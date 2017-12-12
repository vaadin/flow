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
package com.vaadin.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.router.Location;
import com.vaadin.router.NavigationState;
import com.vaadin.router.NavigationTrigger;
import com.vaadin.router.QueryParameters;
import com.vaadin.router.Router;
import com.vaadin.router.RouterLayout;
import com.vaadin.router.event.AfterNavigationEvent;
import com.vaadin.router.event.NavigationEvent;
import com.vaadin.router.util.RouterUtil;
import com.vaadin.ui.BodySize;
import com.vaadin.ui.Inline;
import com.vaadin.ui.UI;
import com.vaadin.ui.Viewport;
import com.vaadin.ui.common.HasElement;
import com.vaadin.util.AnnotationReader;

/**
 * Utility methods used by the BootstrapHandler.
 */
class BootstrapUtils {

    private BootstrapUtils() {
    }

    /**
     * Returns the specified viewport content for the target route chain that
     * was navigated to, specified with {@link Viewport} on the
     * {@link com.vaadin.router.Route} annotated class or the
     * {@link com.vaadin.router.ParentLayout} of the route.
     *
     * @param ui
     *            the application ui
     * @param request
     *            the request for the ui
     * @return the content value string for viewport meta tag
     */
    static Optional<String> getViewportContent(UI ui, VaadinRequest request) {

        Optional<Router> router = ui.getRouter();
        if (router.isPresent()) {
            Optional<NavigationState> navigationTarget = getRouteTargetInformation(
                    request, router.get());

            return navigationTarget
                    .flatMap(BootstrapUtils::getViewportAnnotation)
                    .map(Viewport::value);
        }
        return Optional.empty();
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
     * was navigated to, specified with {@link BodySize} on the
     * {@link com.vaadin.router.Route} annotated class or the
     * {@link com.vaadin.router.ParentLayout} of the route.
     *
     * @param ui
     *            the application ui
     * @param request
     *            the request for the ui
     * @return the content value string for body size style element
     */
    static Optional<String> getBodySizeContent(UI ui, VaadinRequest request) {
        Optional<Router> router = ui.getRouter();
        if (router.isPresent()) {
            Optional<NavigationState> navigationTarget = getRouteTargetInformation(
                    request, router.get());

            return navigationTarget
                    .flatMap(BootstrapUtils::getBodySizeAnnotation)
                    .map(BootstrapUtils::composeBodySizeString);
        }
        return Optional.empty();
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
     * was navigated to, specified with {@link Inline} on the
     * {@link com.vaadin.router.Route} annotated class or the
     * {@link com.vaadin.router.ParentLayout} of the route.
     *
     * @param context
     *            the bootstrap context
     * @return the content value string for viewport meta tag
     */
    static Optional<InlineTargets> getInlineTargets(
            BootstrapHandler.BootstrapContext context) {

        UI ui = context.getUI();
        VaadinRequest request = context.getRequest();

        Optional<Router> router = ui.getRouter();
        if (router.isPresent()) {
            Optional<NavigationState> navigationTarget = getRouteTargetInformation(
                    request, router.get());

            if (navigationTarget.isPresent()) {
                List<Inline> inlineAnnotations = getInlineAnnotations(
                        navigationTarget.get());
                if (!inlineAnnotations.isEmpty()) {
                    InlineTargets inlines = new InlineTargets();
                    inlineAnnotations.forEach(inline -> inlines
                            .addInlineDependency(inline, request));
                    return Optional.of(inlines);
                }
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

}
