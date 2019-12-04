/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.demo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.NodeVisitor;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.server.Command;

/**
 * Helper that invokes a callback once all Vaadin custom elements from a
 * component tree are defined. This is used for all demo contents because of the
 * way custom element definitions are lazy loaded to improve performance on
 * vaadin.com.
 *
 * @author Vaadin Ltd
 */
public class WhenDefinedManager implements Serializable {
    /**
     * Marker type for the marker instance that is used as the value for tags
     * that have already been defined. We need to have an explicit class in this
     * case to support the edge case when sessions may be migrated between
     * different JVMs.
     */
    private static class DoneMarker extends ArrayList<Command> {
        @Override
        public boolean add(Command ignore) {
            // Reduce the risk of accidental modification
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Marker instance for tags that have already been defined.
     */
    private static final DoneMarker DONE_MARKER = new DoneMarker();

    private final UI ui;
    private final HashMap<String, ArrayList<Command>> tagToWaiters = new HashMap<>();

    private WhenDefinedManager(UI ui) {
        this.ui = ui;
    }

    /**
     * Runs the provided command once the custom element has been defined for
     * all Vaadin elements from the give component trees have.
     * <p>
     * The command may run immediately in case no custom elements are used or if
     * they have already been defined.
     *
     * @param rootComponents
     *            an array of component hierarchies from which to find used
     *            Vaadin elements
     * @param command
     *            the command to run once all custom elements are defined
     */
    public void whenDefined(Component[] rootComponents, Command command) {
        Set<String> vaadinTagNames = collectVaadinTagNames(rootComponents);

        collectComponentClasses(rootComponents)
                .forEach(ui.getInternals()::addComponentDependencies);

        HashSet<String> missingTagNames = new HashSet<>();
        for (String tagName : vaadinTagNames) {
            ArrayList<Command> tagWaiters = tagToWaiters.get(tagName);
            if (tagWaiters instanceof DoneMarker) {
                continue;
            }

            missingTagNames.add(tagName);

            // First one to wait for this tag
            if (tagWaiters == null) {
                tagWaiters = new ArrayList<>();
                tagToWaiters.put(tagName, tagWaiters);
                ui.getPage()
                        .executeJs("return customElements.whenDefined($0)",
                                tagName)
                        .then(ignore -> handleLoadedTag(tagName));
            }

            tagWaiters.add(() -> {
                missingTagNames.remove(tagName);
                if (missingTagNames.isEmpty()) {
                    command.execute();
                }
            });
        }

        // If everything was already loaded
        if (missingTagNames.isEmpty()) {
            command.execute();
        }
    }

    private static Set<Class<? extends Component>> collectComponentClasses(
            Component[] rootComponents) {
        Set<Class<? extends Component>> classes = new HashSet<>();

        LinkedList<Component> queue = new LinkedList<>(
                Arrays.asList(rootComponents));
        while (!queue.isEmpty()) {
            Component component = queue.removeLast();
            classes.add(component.getClass());
            component.getChildren().forEach(queue::add);
        }

        return classes;
    }

    private void handleLoadedTag(String tagName) {
        ArrayList<Command> waiters = tagToWaiters.get(tagName);
        tagToWaiters.put(tagName, DONE_MARKER);
        waiters.forEach(Command::execute);
    }

    private static Set<String> collectVaadinTagNames(
            Component[] rootComponents) {
        Set<String> vaadinTagNames = new HashSet<>();

        for (Component rootComponent : rootComponents) {
            rootComponent.getElement().accept(new NodeVisitor() {
                @Override
                public boolean visit(ElementType type, Element element) {
                    if (!element.isTextNode()) {
                        String tag = element.getTag();
                        if (tag.startsWith("vaadin-")) {
                            vaadinTagNames.add(tag);
                        }
                    }
                    return true;
                }

                @Override
                public boolean visit(ShadowRoot root) {
                    return true;
                }
            });
        }

        return vaadinTagNames;
    }

    /**
     * Gets or creates the manager instance for the given UI.
     *
     * @param ui
     *            the UI for which to get an instance, not <code>null</code>
     * @return the manager for the given UI, not <code>null</code>
     */
    public static WhenDefinedManager get(UI ui) {
        WhenDefinedManager instance = ComponentUtil.getData(ui,
                WhenDefinedManager.class);
        if (instance == null) {
            instance = new WhenDefinedManager(ui);
            ComponentUtil.setData(ui, WhenDefinedManager.class, instance);
        }
        return instance;
    }
}
