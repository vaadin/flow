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
package com.vaadin.flow.component.internal;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.hotswap.HotswapClassEvent;
import com.vaadin.flow.hotswap.HotswapClassSessionEvent;
import com.vaadin.flow.hotswap.HotswapCompleteEvent;
import com.vaadin.flow.hotswap.HotswapResourceEvent;
import com.vaadin.flow.hotswap.UIUpdateStrategy;
import com.vaadin.flow.hotswap.VaadinHotswapper;
import com.vaadin.flow.internal.ActiveStyleSheetTracker;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.CssBundler;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.ui.Dependency;

/**
 * Handles the automatic hotswapping of CSS resources and classes annotated
 * with @StyleSheet, taking the appropriate actions to update stylesheets in the
 * browser.
 * <p>
 * When a resource with {@code .css} extension is changed, this class determines
 * if the related file exists in a known public folder (`META-INF/resource`,
 * `resource`, `static`, `public`) in the output resources folder (e.g.
 * `target/classes` for Maven) and triggers a resource update by the Vaadin Dev
 * Server.
 * <p>
 * When a class with {@link com.vaadin.flow.component.dependency.StyleSheet}
 * annotation is changed, this hotswapper:
 * <ul>
 * <li>Adds new link tags when annotation is added</li>
 * <li>Updates link tags when annotation value is changed</li>
 * <li>Removes link tags when annotation is removed</li>
 * </ul>
 * <p>
 * For {@link com.vaadin.flow.component.page.AppShellConfigurator} classes,
 * changes are applied globally to all UIs. For
 * {@link com.vaadin.flow.component.Component} classes, changes are applied when
 * the component is in use.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public class StyleSheetHotswapper implements VaadinHotswapper {

    public static final Logger LOGGER = LoggerFactory
            .getLogger(StyleSheetHotswapper.class);

    /**
     * Tracks stylesheet registrations per class per session.
     */
    private static final String STYLESHEET_REGISTRY_KEY = StyleSheetHotswapper.Registry.class
            .getName();

    // class name -> stylesheet urls
    // using class name to match also when class is reloaded
    private final ConcurrentHashMap<String, Set<String>> appShellStylesheets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> componentStylesheets = new ConcurrentHashMap<>();

    @Override
    public void onInit(VaadinService vaadinService) {
        AppShellRegistry appShellRegistry = AppShellRegistry
                .getInstance(vaadinService.getContext());
        Class<? extends AppShellConfigurator> appShellClass = appShellRegistry
                .getShell();

        if (appShellClass != null) {
            appShellStylesheets.put(appShellClass.getName(),
                    getStyleSheetUrls(appShellClass));
            trackAppShellUrls(vaadinService);
        }

        vaadinService.addUIInitListener(uiInitEvent -> {
            UI ui = uiInitEvent.getUI();
            VaadinSession session = ui.getSession();
            ActiveStyleSheetTracker tracker = ActiveStyleSheetTracker
                    .get(vaadinService);
            ui.addAfterNavigationListener(navigationEvent -> {
                UI newUi = navigationEvent.getLocationChangeEvent().getUI();
                Set<String> allUrls = new LinkedHashSet<>();
                lookupUrlsForComponents(newUi, allUrls, vaadinService);
                allUrls.forEach(tracker::trackAddForComponent);
            });
        });
    }

    @Override
    public void onHotswapComplete(HotswapCompleteEvent event) {
        // Update app shell stored state
        event.getClasses().stream()
                .filter(AppShellConfigurator.class::isAssignableFrom)
                .forEach(clazz -> appShellStylesheets.put(clazz.getName(),
                        getStyleSheetUrls(clazz)));
        trackAppShellUrls(event.getService());
    }

    @Override
    public void onClassesChange(HotswapClassEvent event) {
        Set<Class<?>> classes = filterClasses(event);
        if (classes.isEmpty()) {
            return;
        }
        // Register previous state before component metadata cache gets cleared
        // It's needed to later detect if there are any changes to the
        // StyleSheet annotation
        for (Class<?> clazz : classes) {
            if (Component.class.isAssignableFrom(clazz)) {
                List<String> styleSheets = ComponentUtil
                        .getDependencies(event.getVaadinService(),
                                (Class<? extends Component>) clazz)
                        .getStyleSheets().stream().map(StyleSheet::value)
                        .toList();
                if (!styleSheets.isEmpty()) {
                    componentStylesheets.put(clazz.getName(),
                            new LinkedHashSet<>(styleSheets));
                } else {
                    componentStylesheets.remove(clazz.getName());
                }
            }
        }
    }

    @Override
    public void onClassesChange(HotswapClassSessionEvent event) {
        Set<Class<?>> classes = filterClasses(event);
        if (classes.isEmpty()) {
            return;
        }

        VaadinSession session = event.getVaadinSession();
        Registry registry = getRegistry(session);
        if (!event.isRedefined()) {
            // For loaded but not modified classes, just store their current
            // state
            getRegistry(session).updateState(classes);
            return;
        }

        for (Class<?> clazz : classes) {
            try {
                handleClassChange(event, clazz, registry);
            } catch (Exception e) {
                LOGGER.debug("Failed to handle stylesheet changes for class {}",
                        clazz.getName(), e);
            }
        }
    }

    private static Set<Class<?>> filterClasses(HotswapClassEvent event) {
        return event.getChangedClasses().stream()
                .filter(clazz -> Component.class.isAssignableFrom(clazz)
                        || AppShellConfigurator.class.isAssignableFrom(clazz))
                .collect(Collectors.toSet());
    }

    private void handleClassChange(HotswapClassSessionEvent event,
            Class<?> clazz, Registry registry) {
        boolean isAppShellConfigurator = AppShellConfigurator.class
                .isAssignableFrom(clazz);
        boolean isComponent = Component.class.isAssignableFrom(clazz);

        // Get current stylesheets from the class
        Set<String> currentStylesheets = getStyleSheetUrls(clazz);

        // Get previous stylesheets
        Set<String> previousStylesheets = registry.previousState(clazz);

        // First change, add the initial stylesheets and update them with new
        // values
        if (previousStylesheets == null) {
            previousStylesheets = new LinkedHashSet<>();
            Map<String, Set<String>> initialState = isAppShellConfigurator
                    ? appShellStylesheets
                    : componentStylesheets;
            Set<String> initialStyles = initialState.get(clazz.getName());
            if (initialStyles != null) {
                previousStylesheets.addAll(initialStyles);
            }
        }

        // Detect changes
        Set<String> addedStylesheets = new LinkedHashSet<>(currentStylesheets);
        addedStylesheets.removeAll(previousStylesheets);

        Set<String> removedStylesheets = new LinkedHashSet<>(
                previousStylesheets);
        removedStylesheets.removeAll(currentStylesheets);

        // Only process if there are actual changes
        if (addedStylesheets.isEmpty() && removedStylesheets.isEmpty()) {
            return;
        }

        if (isAppShellConfigurator) {
            handleAppShellConfiguratorChange(event, clazz, addedStylesheets,
                    removedStylesheets, registry);
        } else if (isComponent) {
            handleComponentChange(event, clazz, addedStylesheets,
                    removedStylesheets, registry);
        }

        registry.updateState(clazz, currentStylesheets);
    }

    private void handleAppShellConfiguratorChange(
            HotswapClassSessionEvent event, Class<?> clazz,
            Set<String> addedStylesheets, Set<String> removedStylesheets,
            Registry registry) {

        // Verify this is actually the registered AppShell
        AppShellRegistry appShellRegistry = AppShellRegistry
                .getInstance(event.getVaadinService().getContext());
        Class<? extends AppShellConfigurator> appShellClass = appShellRegistry
                .getShell();

        if (appShellClass == null
                || !appShellClass.getName().equals(clazz.getName())) {
            return;
        }

        LOGGER.debug(
                "Processing AppShellConfigurator stylesheet changes for {}: added={}, removed={}",
                clazz.getName(), addedStylesheets, removedStylesheets);

        updateUIs(event, clazz, addedStylesheets, removedStylesheets, registry);

        Set<String> currentUrls = getStyleSheetUrls(clazz);
        appShellStylesheets.put(clazz.getName(), currentUrls);
        trackAppShellUrls(event.getVaadinService());
    }

    private void handleComponentChange(HotswapClassSessionEvent event,
            Class<?> clazz, Set<String> addedStylesheets,
            Set<String> removedStylesheets, Registry registry) {

        LOGGER.debug(
                "Processing Component stylesheet changes for {}: added={}, removed={}",
                clazz.getName(), addedStylesheets, removedStylesheets);
        updateUIs(event, clazz, addedStylesheets, removedStylesheets, registry);
    }

    private void updateUIs(HotswapClassSessionEvent event, Class<?> clazz,
            Set<String> addedStylesheets, Set<String> removedStylesheets,
            Registry registry) {

        boolean isAppShell = AppShellConfigurator.class.isAssignableFrom(clazz);

        VaadinSession session = event.getVaadinSession();
        // For components, check if they are currently in use in any UI
        for (UI ui : session.getUIs()) {
            if (ui.isClosing()) {
                continue;
            }

            // Check if component is in use by looking at the component tree
            if (isAppShell || isComponentInUse(ui, clazz)) {

                // Remove old stylesheets
                for (String url : removedStylesheets) {
                    if (registry.previousState(clazz) == null) {
                        // This is the first time the component class is
                        // processed by the hotswapper
                        // remove existing dependency for the url
                        Dependency dependency = ui.getInternals()
                                .getDependencyList().getDependencyByUrl(url,
                                        Dependency.Type.STYLESHEET);
                        if (dependency != null) {
                            ui.getInternals()
                                    .removeStyleSheet(dependency.getId());
                            // Track removal for this session
                            ActiveStyleSheetTracker
                                    .get(event.getVaadinService())
                                    .trackRemoveForComponent(url);
                            String normalized = normalizeStylesheetUrl(url);
                            if (normalized != null) {
                                event.updateClientResource(
                                        ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                                                + normalized,
                                        "");
                            }
                            event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);
                        }
                    } else {
                        registry.removeRegistration(clazz, url)
                                .ifPresent(dependencyId -> {
                                    ui.getInternals()
                                            .removeStyleSheet(dependencyId);
                                    // Track removal for this session
                                    ActiveStyleSheetTracker
                                            .get(event.getVaadinService())
                                            .trackRemoveForComponent(url);
                                    // Also notify client to remove any inline
                                    // style tag pushed earlier
                                    String normalized = normalizeStylesheetUrl(
                                            url);
                                    if (normalized != null) {
                                        event.updateClientResource(
                                                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                                                        + normalized,
                                                "");
                                    }
                                    event.triggerUpdate(ui,
                                            UIUpdateStrategy.REFRESH);
                                });
                    }
                    if (isAppShell) {
                        // Remove link tag added to head during page bootstrap
                        // Sends removal for stylesheet added by
                        // AppShellRegistry
                        ui.getInternals().removeStyleSheet("appShell-" + url);
                        event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);
                    }
                    LOGGER.debug("Removed stylesheet {} from Component {}", url,
                            clazz.getName());
                }

                // Add new stylesheets
                for (String url : addedStylesheets) {
                    try {
                        ui.getPage().addStyleSheet(url);
                        Dependency dependency = ui.getInternals()
                                .getDependencyList().getDependencyByUrl(url,
                                        Dependency.Type.STYLESHEET);
                        registry.addRegistration(clazz, dependency);
                        ActiveStyleSheetTracker.get(event.getVaadinService())
                                .trackAddForComponent(url);
                        // Immediately push bundled CSS content for the added
                        // URL so client applies it without link reload
                        String normalized = normalizeStylesheetUrl(url);
                        if (normalized != null) {
                            tryBundlePublicStylesheet(event, normalized)
                                    .ifPresent(content -> event
                                            .updateClientResource(
                                                    ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                                                            + normalized,
                                                    content));
                        }
                        event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);
                        LOGGER.debug("Added stylesheet {} to Component {}", url,
                                clazz.getName());
                    } catch (Exception e) {
                        LOGGER.debug(
                                "Failed to add stylesheet {} to Component {}",
                                url, clazz.getName(), e);
                    }
                }
            }
        }
    }

    @Override
    public void onResourcesChange(HotswapResourceEvent event) {
        // no-op: changes in CSS files are handled by a dedicated file watcher
    }

    private void trackAppShellUrls(VaadinService vaadinService) {
        ActiveStyleSheetTracker.get(vaadinService)
                .trackForAppShell(appShellStylesheets.values().stream()
                        .flatMap(Set::stream).collect(Collectors.toSet()));
    }

    private boolean isComponentInUse(UI ui, Class<?> componentClass) {
        // Check if any component in the UI tree is an instance of the given
        // class
        return ui.getChildren()
                .anyMatch(component -> componentClass.isInstance(component)
                        || hasChildOfType(component, componentClass));
    }

    private boolean hasChildOfType(Component component, Class<?> targetClass) {
        if (targetClass.isInstance(component)) {
            return true;
        }
        return component.getChildren()
                .anyMatch(child -> hasChildOfType(child, targetClass));
    }

    private static Set<String> getStyleSheetUrls(Class<?> clazz) {
        Set<String> urls = new LinkedHashSet<>();
        if (Component.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            Class<? extends Component> componentClass = (Class<? extends Component>) clazz;
            List<StyleSheet> annotations = AnnotationReader
                    .getStyleSheetAnnotations(componentClass);
            for (StyleSheet annotation : annotations) {
                String url = annotation.value();
                if (url != null && !url.isEmpty()) {
                    urls.add(url);
                }
            }
        } else {
            // For App Shell classes, check annotations directly
            StyleSheet[] annotations = clazz
                    .getAnnotationsByType(StyleSheet.class);
            for (StyleSheet annotation : annotations) {
                String url = annotation.value();
                if (url != null && !url.isEmpty()) {
                    urls.add(url);
                }
            }
        }
        return urls;
    }

    private Registry getRegistry(VaadinSession session) {
        Registry registry = (Registry) session
                .getAttribute(STYLESHEET_REGISTRY_KEY);
        if (registry == null) {
            registry = new Registry();
            session.setAttribute(STYLESHEET_REGISTRY_KEY, registry);
        }
        return registry;
    }

    private static class RegistryEntry {
        // url -> dependency id
        private final Map<String, String> registrations = new LinkedHashMap<>();
        // url
        private final Set<String> previousState = new LinkedHashSet<>();

        void updateState(Set<String> stylesheets) {
            previousState.clear();
            previousState.addAll(stylesheets);
        }

        Optional<String> removeRegistration(String url) {
            return Optional.ofNullable(registrations.remove(url));
        }

        void addRegistration(Dependency dependency) {
            if (dependency != null) {
                registrations.put(dependency.getUrl(), dependency.getId());
            }
        }

    }

    private static class Registry implements Serializable {
        // class name -> RegistryEntry
        private transient Map<String, RegistryEntry> entries = new ConcurrentHashMap<>();

        Set<String> previousState(Class<?> clazz) {
            RegistryEntry entry = entries.get(clazz.getName());
            if (entry != null) {
                return new LinkedHashSet<>(entry.previousState);
            }
            return null;
        }

        void updateState(Class<?> clazz, Set<String> stylesheets) {
            entries.computeIfAbsent(clazz.getName(), k -> new RegistryEntry())
                    .updateState(stylesheets);
        }

        void updateState(Set<Class<?>> classes) {
            for (Class<?> clazz : classes) {
                Set<String> stylesheets = getStyleSheetUrls(clazz);
                if (!stylesheets.isEmpty()) {
                    updateState(clazz, stylesheets);
                }
            }
        }

        Optional<String> removeRegistration(Class<?> clazz, String url) {
            return Optional.ofNullable(entries.get(clazz.getName()))
                    .flatMap(entry -> entry.removeRegistration(url));
        }

        void addRegistration(Class<?> clazz, Dependency dependency) {
            entries.computeIfAbsent(clazz.getName(), k -> new RegistryEntry())
                    .addRegistration(dependency);
        }

        @Serial
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            entries = new ConcurrentHashMap<>();
        }
    }

    static String normalizeStylesheetUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        url = url.trim();
        if (url.startsWith(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)) {
            url = url.substring(
                    ApplicationConstants.CONTEXT_PROTOCOL_PREFIX.length());
        }
        if (url.startsWith(ApplicationConstants.BASE_PROTOCOL_PREFIX)) {
            url = url.substring(
                    ApplicationConstants.BASE_PROTOCOL_PREFIX.length());
        }
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        // Normalize separators
        url = FrontendUtils.getUnixPath(new File(url).toPath());
        if (url.startsWith("./")) {
            url = url.substring(2);
        }
        return url;
    }

    private Optional<String> tryBundlePublicStylesheet(
            HotswapClassSessionEvent event, String normalizedPath) {
        try {
            VaadinService service = event.getVaadinService();
            ApplicationConfiguration config = ApplicationConfiguration
                    .get(service.getContext());
            File projectFolder = config != null ? config.getProjectFolder()
                    : null;
            if (projectFolder == null) {
                return Optional.empty();
            }
            File resourceFolder = config.getJavaResourceFolder();
            Collection<File> stylesheets = Stream.concat(Stream
                    .of("META-INF/resources", "resources", "static", "public")
                    .map(location -> new File(resourceFolder, location)),
                    Stream.of(new File(projectFolder, "src/main/webapp")))
                    .filter(root -> root.exists() && root.isDirectory())
                    .map(root -> new File(root, normalizedPath))
                    .filter(File::exists).filter(File::isFile).toList();

            for (File stylesheet : stylesheets) {
                try {
                    String root = getResourceRootFromAbsolutePath(
                            normalizedPath, stylesheet);
                    String contextPath = getContextPath(service);
                    String bundled = CssBundler.inlineImportsForPublicResources(
                            new File(root), stylesheet, contextPath);
                    return Optional.ofNullable(bundled);
                } catch (IOException ioe) {
                    LOGGER.debug("Failed to inline CSS imports for {}",
                            stylesheet, ioe);
                    return Optional.empty();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error while trying to bundle public stylesheet {}",
                    normalizedPath, e);
        }
        return Optional.empty();
    }

    private static String getResourceRootFromAbsolutePath(String normalizedPath,
            File stylesheet) {
        return stylesheet.getAbsolutePath().substring(0,
                stylesheet.getAbsolutePath().indexOf(normalizedPath));
    }

    private static String getContextPath(VaadinService service) {
        String contextPath = "";
        if (service
                .getContext() instanceof com.vaadin.flow.server.VaadinServletContext) {
            contextPath = ((com.vaadin.flow.server.VaadinServletContext) service
                    .getContext()).getContext().getContextPath();
        }
        return contextPath;
    }

    private void lookupUrlsForComponents(Component root, Set<String> allUrls,
            VaadinService vaadinService) {
        root.getChildren().forEach(child -> {
            if (child.getClass().isAnnotationPresent(StyleSheet.class)) {
                ComponentUtil.getDependencies(vaadinService, child.getClass())
                        .getStyleSheets().stream().map(StyleSheet::value)
                        .forEach(allUrls::add);
            }
            lookupUrlsForComponents(child, allUrls, vaadinService);
        });
    }
}
