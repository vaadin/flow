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
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on-going
     * @param navigationTarget
     *            Navigation target
     * @deprecated Use {@link #BeforeEnterEvent(NavigationEvent, Class, List)}
     *             instead.
     */
    @Deprecated
    public BeforeEnterEvent(NavigationEvent event, Class<?> navigationTarget) {
        super(event, navigationTarget);
    }

    /**
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on-going
     * @param navigationTarget
     *            Navigation target
     * @param layouts
     *            Navigation layout chain
     */
    public BeforeEnterEvent(NavigationEvent event, Class<?> navigationTarget,
            List<Class<? extends RouterLayout>> layouts) {
        super(event, navigationTarget, layouts);
    }

    /**
     * Constructs a new BeforeNavigation Event.
     *
     * @param router
     *            the router that triggered the change, not {@code null}
     * @param trigger
     *            the type of user action that triggered this location change,
     *            not <code>null</code>
     * @param location
     *            the new location, not {@code null}
     * @param navigationTarget
     *            navigation target class
     * @param ui
     *            the UI related to the navigation
     * @deprecated Use
     *             {@link #BeforeEnterEvent(Router, NavigationTrigger, Location, Class, UI, List)}
     *             instead.
     */
    @Deprecated
    public BeforeEnterEvent(Router router, NavigationTrigger trigger,
            Location location, Class<?> navigationTarget, UI ui) {
        super(router, trigger, location, navigationTarget, ui);
    }

    /**
     * Constructs a new BeforeNavigation Event.
     *
     * @param router
     *            the router that triggered the change, not {@code null}
     * @param trigger
     *            the type of user action that triggered this location change,
     *            not <code>null</code>
     * @param location
     *            the new location, not {@code null}
     * @param navigationTarget
     *            navigation target class
     * @param ui
     *            the UI related to the navigation
     * @param layouts
     *            the layout chain for the navigation target
     */
    public BeforeEnterEvent(Router router, NavigationTrigger trigger,
            Location location, Class<?> navigationTarget, UI ui,
            List<Class<? extends RouterLayout>> layouts) {
        super(router, trigger, location, navigationTarget, ui, layouts);
    }
}
