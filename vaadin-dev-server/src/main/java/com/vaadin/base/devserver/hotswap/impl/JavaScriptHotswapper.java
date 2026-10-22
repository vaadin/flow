/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.base.devserver.hotswap.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
import com.vaadin.base.devserver.hotswap.HotswapCompleteEvent;
import com.vaadin.base.devserver.hotswap.UIUpdateStrategy;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.internal.ComponentMetaData;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.server.FrontendDependencyUrlResolver;
import com.vaadin.flow.server.VaadinService;

/**
 * Reloads the page when a {@link JavaScript} value that is loaded at runtime is
 * removed or changed on a component in use, and warns when a bundled
 * {@link JavaScript} value changes.
 * <p>
 * A runtime {@link JavaScript} value is added to the page as a {@code script}
 * element, and a script that has run cannot be removed from the page again. A
 * refresh would only add the new script next to the previous one, so the
 * affected UIs are reloaded instead. Adding a value does not need a reload, as
 * the refresh adds the new script.
 * <p>
 * Bundled values are only collected into the bundle when the frontend is built,
 * so changing them, or moving a value between bundled and runtime, needs a
 * restart. That is logged rather than handled.
 * <p>
 * A component in use is also affected when the change is in one of its
 * superclasses or in a class it refers to with {@link Uses}. The cached
 * metadata of such a component still holds the previous values, so it is read
 * as the previous values and then cleared.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.4
 */
public class JavaScriptHotswapper implements VaadinHotswapper {

    // class name -> scripts before the redefinition, using the class name to
    // match also when the class is reloaded
    private final ConcurrentHashMap<String, Set<Script>> previousScripts = new ConcurrentHashMap<>();

    // class names whose bundled values changed and that have been warned about
    // during the ongoing hotswap
    private final Set<String> warnedClasses = ConcurrentHashMap.newKeySet();

    private record Script(String value, JavaScript.Type type, boolean runtime) {
    }

    /**
     * Creates the hotswapper. It is found through the service loader.
     */
    public JavaScriptHotswapper() {
        // Default constructor
    }

    @Override
    public void onClassesChange(HotswapClassEvent event) {
        if (!event.isRedefined()) {
            return;
        }
        // The component metadata cache of the changed classes is cleared only
        // after all hotswappers have handled this event, so it still holds
        // the values from before the redefinition
        for (Class<?> type : event.getChangedClasses()) {
            if (Component.class.isAssignableFrom(type)) {
                previousScripts
                        .put(type.getName(),
                                scripts(ComponentUtil
                                        .getDependencies(
                                                event.getVaadinService(),
                                                type.asSubclass(
                                                        Component.class))
                                        .getJavaScripts()));
            }
        }
    }

    @Override
    public void onClassesChange(HotswapClassSessionEvent event) {
        if (!event.isRedefined()) {
            return;
        }
        Set<String> changedClasses = event.getChangedClasses().stream()
                .map(Class::getName).collect(Collectors.toSet());
        Map<Class<?>, Boolean> reloadNeeded = new HashMap<>();
        for (UI ui : event.getVaadinSession().getUIs()) {
            if (ui.isClosing()) {
                continue;
            }
            boolean reload = componentClassesInUse(ui).stream()
                    .anyMatch(type -> reloadNeeded.computeIfAbsent(type,
                            key -> isReloadNeeded(event.getVaadinService(),
                                    type, changedClasses)));
            if (reload) {
                event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);
            }
        }
    }

    @Override
    public void onHotswapComplete(HotswapCompleteEvent event) {
        previousScripts.clear();
        warnedClasses.clear();
    }

    private boolean isReloadNeeded(VaadinService service,
            Class<? extends Component> type, Set<String> changedClasses) {
        if (!dependsOn(type, changedClasses, new HashSet<>())) {
            return false;
        }
        Set<Script> previous = previousScripts.computeIfAbsent(type.getName(),
                name -> changedClasses.contains(name) ? null
                        : readStaleScripts(service, type));
        if (previous == null) {
            return false;
        }
        // Read the same way as the previous values, so that @Uses
        // dependencies are on both sides
        Set<Script> current = scripts(new ComponentMetaData(type)
                .getDependencyInfo(service).getJavaScripts());
        if (!bundled(current).equals(bundled(previous))
                && warnedClasses.add(type.getName())) {
            warnAboutChangedBundledScripts(type);
        }
        return !runtime(current).containsAll(runtime(previous));
    }

    /**
     * Warns that the bundled {@link JavaScript} values used by the given
     * component class changed, which needs a restart.
     *
     * @param type
     *            the component class whose bundled values changed
     */
    void warnAboutChangedBundledScripts(Class<?> type) {
        getLogger().warn("The bundled @JavaScript values used by {} changed. "
                + "Bundled values are collected into the frontend bundle when the application starts, "
                + "so until the application is restarted a removed script or one moved to a runtime URL keeps running "
                + "from the bundle, and an added one or one moved from a runtime URL is not loaded.",
                type.getName());
    }

    /**
     * Reads the scripts of a component whose class was not redefined but that
     * depends on a redefined class. Its cached metadata was computed before the
     * redefinition, so it holds the previous values; it is cleared so that the
     * component is read again with the new values from now on.
     */
    private static Set<Script> readStaleScripts(VaadinService service,
            Class<? extends Component> type) {
        Set<Script> scripts = scripts(
                ComponentUtil.getDependencies(service, type).getJavaScripts());
        ReflectionCache.clearAll(type);
        return scripts;
    }

    /**
     * Tells whether the given class, one of its superclasses, or a class it
     * refers to with {@link Uses} (also transitively) is one of the given
     * classes.
     */
    private static boolean dependsOn(Class<?> type, Set<String> classNames,
            Set<Class<?>> visited) {
        if (!visited.add(type)) {
            return false;
        }
        for (Class<?> clazz = type; clazz != null && Component.class
                .isAssignableFrom(clazz); clazz = clazz.getSuperclass()) {
            if (classNames.contains(clazz.getName())) {
                return true;
            }
        }
        return AnnotationReader.getAnnotationsFor(type, Uses.class).stream()
                .anyMatch(uses -> dependsOn(uses.value(), classNames, visited));
    }

    private static Set<Class<? extends Component>> componentClassesInUse(
            UI ui) {
        Set<Class<? extends Component>> classes = new LinkedHashSet<>();
        ui.getChildren().forEach(child -> collectClasses(child, classes));
        return classes;
    }

    private static void collectClasses(Component component,
            Set<Class<? extends Component>> classes) {
        classes.add(component.getClass());
        component.getChildren()
                .forEach(child -> collectClasses(child, classes));
    }

    private static Set<Script> scripts(Collection<JavaScript> annotations) {
        return annotations.stream()
                .map(js -> new Script(js.value(), js.type(),
                        FrontendDependencyUrlResolver.isRuntimeJavaScript(js)))
                .collect(Collectors.toSet());
    }

    private static Set<Script> runtime(Set<Script> scripts) {
        return scripts.stream().filter(Script::runtime)
                .collect(Collectors.toSet());
    }

    private static Set<Script> bundled(Set<Script> scripts) {
        return scripts.stream().filter(script -> !script.runtime())
                .collect(Collectors.toSet());
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(JavaScriptHotswapper.class);
    }
}
