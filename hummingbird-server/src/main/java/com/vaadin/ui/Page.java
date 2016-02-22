/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.namespace.DependencyListNamespace;

/**
 * Represents the web page open in the browser, containing the UI it is
 * connected to.
 *
 * @author Vaadin
 * @since
 */
public class Page implements Serializable {

    private DependencyListNamespace namespace;

    /**
     * Creates a page instance for the given UI.
     *
     * @param stateTree
     *            the state tree this page instance is connected to
     */
    public Page(StateNode node) {
        namespace = node.getNamespace(DependencyListNamespace.class);
    }

    /**
     * Adds the given dependency to the page and ensures that it is loaded
     * successfully.
     *
     * @param dependency
     *            the dependency to load
     */
    public void addDependency(Dependency dependency) {
        namespace.add(dependency);
    }
}
