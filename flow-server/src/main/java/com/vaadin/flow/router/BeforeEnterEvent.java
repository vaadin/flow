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
package com.vaadin.flow.router;

import java.util.List;

import com.vaadin.flow.component.UI;

/**
 * Event created before navigation happens.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class BeforeEnterEvent extends BeforeEvent {

    /**
     * Constructs event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on-going, not <code>null</code>
     * @param navigationTarget
     *            navigation target, not <code>null</code>
     * @param layouts
     *            navigation layout chain, not <code>null</code>
     */
    public BeforeEnterEvent(NavigationEvent event, Class<?> navigationTarget,
            List<Class<? extends RouterLayout>> layouts) {
        super(event, navigationTarget, layouts);
    }

    /**
     * Constructs event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on-going, not <code>null</code>
     * @param navigationTarget
     *            navigation target, not <code>null</code>
     * @param parameters
     *            route parameters, not <code>null</code>
     * @param layouts
     *            navigation layout chain, not <code>null</code>
     */
    public BeforeEnterEvent(NavigationEvent event, Class<?> navigationTarget,
            RouteParameters parameters,
            List<Class<? extends RouterLayout>> layouts) {
        super(event, navigationTarget, parameters, layouts);
    }

    /**
     * Constructs a new BeforeEnterEvent.
     *
     * @param router
     *            the router that triggered the change, not <code>null</code>
     * @param trigger
     *            the type of user action that triggered this location change,
     *            not <code>null</code>
     * @param location
     *            the new location, not <code>null</code>
     * @param navigationTarget
     *            navigation target class, not <code>null</code>
     * @param ui
     *            the UI related to the navigation, not <code>null</code>
     * @param layouts
     *            the layout chain for the navigation target, not
     *            <code>null</code>
     */
    public BeforeEnterEvent(Router router, NavigationTrigger trigger,
            Location location, Class<?> navigationTarget, UI ui,
            List<Class<? extends RouterLayout>> layouts) {
        super(router, trigger, location, navigationTarget, ui, layouts);
    }

    /**
     * Constructs a new BeforeEnterEvent.
     *
     * @param router
     *            the router that triggered the change, not <code>null</code>
     * @param trigger
     *            the type of user action that triggered this location change,
     *            not <code>null</code>
     * @param location
     *            the new location, not <code>null</code>
     * @param navigationTarget
     *            navigation target class, not <code>null</code>
     * @param parameters
     *            route parameters, not <code>null</code>
     * @param ui
     *            the UI related to the navigation, not <code>null</code>
     * @param layouts
     *            the layout chain for the navigation target, not
     *            <code>null</code>
     */
    public BeforeEnterEvent(Router router, NavigationTrigger trigger,
            Location location, Class<?> navigationTarget,
            RouteParameters parameters, UI ui,
            List<Class<? extends RouterLayout>> layouts) {
        super(router, trigger, location, navigationTarget, parameters, ui, layouts);
    }
}
