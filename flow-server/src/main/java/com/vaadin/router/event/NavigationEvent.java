/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.router.event;

import java.util.EventObject;

import com.vaadin.router.Location;
import com.vaadin.router.NavigationTrigger;
import com.vaadin.router.RouterInterface;
import com.vaadin.ui.UI;

/**
 * Event object with data related to navigation.
 *
 * @author Vaadin Ltd
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
    public NavigationEvent(RouterInterface router, Location location, UI ui,
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
    public RouterInterface getSource() {
        return (RouterInterface) super.getSource();
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
