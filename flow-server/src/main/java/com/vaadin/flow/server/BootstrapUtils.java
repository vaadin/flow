/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouteUtil;

/**
 * Utility methods used by the BootstrapHandler.
 *
 * @since 1.0
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
        return context.getPageConfigurationAnnotation(Viewport.class)
                .map(Viewport::value);
    }

    /**
     * Returns the map which contains name and content of the customized meta
     * tag for the target route chain that was navigated to, specified with
     * {@link Meta} on the {@link Route} class or the {@link ParentLayout} of
     * the route.
     *
     * @param context
     *            the bootstrap context
     * @return the map contains name and content value string for the customized
     *         meta tag
     */
    static Map<String, String> getMetaTargets(
            BootstrapHandler.BootstrapContext context) {
        List<Meta> metaAnnotations = context
                .getPageConfigurationAnnotations(Meta.class);
        boolean illegalValue = false;
        Map<String, String> map = new HashMap<>();
        for (Meta meta : metaAnnotations) {
            if (!meta.name().isEmpty() && !meta.content().isEmpty()) {
                map.put(meta.name(), meta.content());
            } else {
                illegalValue = true;
                break;
            }
        }
        if (illegalValue) {
            throw new IllegalStateException(
                    "Meta tags added via Meta annotation contain null value on name or content attribute.");
        }
        return map;
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
        Router router = ui.getRouter();
        NavigationEvent navigationEvent = new NavigationEvent(router,
                new Location(pathInfo,
                        QueryParameters.full(request.getParameterMap())),
                ui, NavigationTrigger.PAGE_LOAD);

        List<HasElement> components = ui.getChildren()
                .map(component -> (HasElement) component)
                .collect(Collectors.toList());

        AfterNavigationEvent afterNavigationEvent = new AfterNavigationEvent(
                new LocationChangeEvent(navigationEvent.getSource(),
                        navigationEvent.getUI(), navigationEvent.getTrigger(),
                        navigationEvent.getLocation(), components));

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
    static String getBodySizeContent(
            BootstrapHandler.BootstrapContext context) {

        Optional<BodySize> bodySize = context
                .getPageConfigurationAnnotation(BodySize.class);

        // Set full size by default if @BodySize is not used
        String height = bodySize.map(BodySize::height).orElse("100vh");
        String width = bodySize.map(BodySize::width).orElse("100vw");

        StringBuilder bodyString = new StringBuilder();

        bodyString.append("body {");
        if (!height.isEmpty()) {
            bodyString.append("height:").append(height).append(";");
        }
        if (!width.isEmpty()) {
            bodyString.append("width:").append(width).append(";");
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
        List<Inline> inlineAnnotations = context
                .getPageConfigurationAnnotations(Inline.class);

        if (inlineAnnotations.isEmpty()) {
            return Optional.empty();
        } else {
            InlineTargets inlines = new InlineTargets();
            inlineAnnotations.forEach(inline -> inlines
                    .addInlineDependency(inline, context.getRequest()));
            return Optional.of(inlines);
        }
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

    /**
     * Finds the class on on which page configuration annotation should be
     * defined.
     *
     * @param ui
     *            the UI for which to do the lookup, not <code>null</code>
     * @param request
     *            the request for which to do the lookup, not <code>null</code>
     * @return the class for which page configuration annotations should be
     *         defined, or an empty optional if no such class is available
     */
    public static Optional<Class<?>> resolvePageConfigurationHolder(UI ui,
                                                                    VaadinRequest request) {
        assert ui != null;
        assert request != null;
        if (ui.getRouter() == null) {
            return Optional.empty();
        }
        Optional<Class<?>> navigationTarget = ui.getRouter()
                .resolveNavigationTarget(request.getPathInfo(),
                        request.getParameterMap())
                .map(navigationState -> resolveTopParentLayout(ui,
                        navigationState));
        if (navigationTarget.isPresent()) {
            return navigationTarget;
        }
        // If there is no route target available then let's ask for "route not
        // found" target
        return ui.getRouter().resolveRouteNotFoundNavigationTarget()
                .map(state -> {
                    /*
                     * {@code resolveTopParentLayout} is theoretically the
                     * correct way to get the parent layout. But in fact it does
                     * work for non route targets.
                     */
                    List<Class<? extends RouterLayout>> layouts = RouteUtil
                            .getParentLayoutsForNonRouteTarget(
                                    state.getNavigationTarget());
                    if (layouts.isEmpty()) {
                        return state.getNavigationTarget();
                    } else {
                        return layouts.get(layouts.size() - 1);
                    }
                });
    }

    private static Class<?> resolveTopParentLayout(UI ui,
                                                   NavigationState navigationState) {
        Class<? extends RouterLayout> parentLayout = getTopParentLayout(
                ui.getRouter(), navigationState);
        if (parentLayout != null) {
            return parentLayout;
        }

        return navigationState.getNavigationTarget();
    }

    private static Class<? extends RouterLayout> getTopParentLayout(
            Router router, NavigationState navigationState) {
        List<Class<? extends RouterLayout>> routeLayouts = router.getRegistry()
                .getRouteLayouts(navigationState.getResolvedPath(),
                        navigationState.getNavigationTarget());
        if (routeLayouts.isEmpty()) {
            return null;
        }
        return routeLayouts.get(routeLayouts.size() - 1);
    }
}
