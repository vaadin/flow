/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import java.io.Serializable;
import java.util.ServiceLoader;

import com.vaadin.flow.component.Component;

/**
 * A filter that can prevent specific navigation targets from being registered.
 * <p>
 * Listener instances are by discovered and instantiated using
 * {@link ServiceLoader}. This means that all implementations must have a
 * zero-argument constructor and the fully qualified name of the implementation
 * class must be listed on a separate line in a
 * META-INF/services/cocom.vaadin.flow.server.startup.NavigationTargetFilter
 * file present in the jar file containing the implementation class.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface NavigationTargetFilter extends Serializable {
    /**
     * Tests whether the given navigation target class should be included.
     *
     * @param navigationTarget
     *            the navigation target class to test
     * @return <code>true</code> to include the navigation target,
     *         <code>false</code> to discard it
     */
    boolean testNavigationTarget(Class<? extends Component> navigationTarget);

    /**
     * Tests whether the given error navigation target class should be included.
     *
     * @param errorNavigationTarget
     *            the error navigation target class to test
     * @return <code>true</code> to include the error navigation target,
     *         <code>false</code> to discard it
     */
    boolean testErrorNavigationTarget(Class<?> errorNavigationTarget);
}
