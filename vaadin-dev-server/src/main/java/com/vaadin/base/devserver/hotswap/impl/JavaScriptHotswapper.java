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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
import com.vaadin.base.devserver.hotswap.HotswapCompleteEvent;
import com.vaadin.base.devserver.hotswap.UIUpdateStrategy;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.FrontendDependencyUrlResolver;

/**
 * Reloads the page when a {@link JavaScript} value that is loaded at runtime is
 * removed or changed on a component in use.
 * <p>
 * A runtime {@link JavaScript} value is added to the page as a {@code script}
 * element, and a script that has run cannot be removed from the page again. A
 * refresh would only add the new script next to the previous one, so the
 * affected UIs are reloaded instead. Adding a value does not need a reload, as
 * the refresh adds the new script.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.4
 */
public class JavaScriptHotswapper implements VaadinHotswapper {

    // class name -> runtime scripts before the redefinition, using the class
    // name to match also when the class is reloaded
    private final ConcurrentHashMap<String, Set<RuntimeScript>> previousScripts = new ConcurrentHashMap<>();

    private record RuntimeScript(String value, JavaScript.Type type) {
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
        // The component metadata cache is cleared only after all
        // hotswappers have run, so it still holds the values from before the
        // redefinition
        for (Class<? extends Component> type : componentClasses(event)) {
            previousScripts.put(type.getName(),
                    runtimeScripts(ComponentUtil
                            .getDependencies(event.getVaadinService(), type)
                            .getJavaScripts()));
        }
    }

    @Override
    public void onClassesChange(HotswapClassSessionEvent event) {
        if (!event.isRedefined()) {
            return;
        }
        for (Class<? extends Component> type : componentClasses(event)) {
            Set<RuntimeScript> previous = previousScripts.get(type.getName());
            if (previous == null || runtimeScripts(
                    AnnotationReader.getJavaScriptAnnotations(type))
                    .containsAll(previous)) {
                continue;
            }
            for (UI ui : event.getVaadinSession().getUIs()) {
                if (!ui.isClosing()
                        && StyleSheetHotswapper.isComponentInUse(ui, type)) {
                    event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);
                }
            }
        }
    }

    @Override
    public void onHotswapComplete(HotswapCompleteEvent event) {
        event.getClasses()
                .forEach(type -> previousScripts.remove(type.getName()));
    }

    @SuppressWarnings("unchecked")
    private static List<Class<? extends Component>> componentClasses(
            HotswapClassEvent event) {
        return event.getChangedClasses().stream()
                .filter(Component.class::isAssignableFrom)
                .<Class<? extends Component>> map(
                        type -> (Class<? extends Component>) type)
                .toList();
    }

    private static Set<RuntimeScript> runtimeScripts(
            Collection<JavaScript> annotations) {
        return annotations.stream()
                .filter(FrontendDependencyUrlResolver::isRuntimeJavaScript)
                .map(js -> new RuntimeScript(js.value(), js.type()))
                .collect(Collectors.toSet());
    }
}
