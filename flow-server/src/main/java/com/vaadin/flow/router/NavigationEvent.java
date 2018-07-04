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

import java.util.EventObject;

import com.vaadin.flow.component.UI;

/**
 * Event object with data related to navigation.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class NavigationEvent extends EventObject {
    private final Location location;
    private final UI ui;
    private final NavigationTrigger trigger;

    /**
     * Creates a new navigation event.
     *
     * @param router
     *            the router handling the navigation, not {@code null}
     * @param location
     *            the new location, not {@code null}
     * @param ui
     *            the UI in which the navigation occurs, not {@code null}
     * @param trigger
     *            the type of user action that triggered this navigation event,
     *            not {@code null}
     */
    public NavigationEvent(Router router, Location location, UI ui,
            NavigationTrigger trigger) {
        super(router);

        assert location != null;
        assert ui != null;
        assert trigger != null;

        this.location = location;
        this.ui = ui;
        this.trigger = trigger;
    }

    @Override
    public Router getSource() {
        return (Router) super.getSource();
    }

    /**
     * Gets the new location.
     *
     * @return the new location, not {@code null}
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the UI in which the navigation occurs.
     *
     * @return the UI of the navigation
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Gets the type of user action that triggered this navigation event.
     *
     * @return the type of user action that triggered this navigation event, not
     *         {@code null}
     */
    public NavigationTrigger getTrigger() {
        return trigger;
    }
}
