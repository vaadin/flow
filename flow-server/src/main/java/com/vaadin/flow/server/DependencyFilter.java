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

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.shared.ui.Dependency;

/**
 * Filter for dependencies loaded using {@link StyleSheet @StyleSheet}, and *
 * {@link JavaScript @JavaScript}.
 *
 * @see ServiceInitEvent#addDependencyFilter(DependencyFilter)
 * @since 1.0
 */
@FunctionalInterface
public interface DependencyFilter extends Serializable {

    /**
     * Filters the list of dependencies and returns a (possibly) updated
     * version.
     * <p>
     * Called whenever dependencies are about to be sent to the client side for
     * loading and when templates are parsed on the server side.
     *
     * @param dependencies
     *            the collected dependencies, possibly already modified by other
     *            filters
     * @param service
     *            a Vaadin service
     * @return a list of dependencies to load
     */
    List<Dependency> filter(List<Dependency> dependencies,
            VaadinService service);

}
