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
package com.vaadin.flow.router;

import java.util.EventObject;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * when the server starts handling a navigation for a UI, before any
 * {@link BeforeLeaveEvent} or {@link BeforeEnterEvent} listener runs.
 * <p>
 * The event is meant for monitoring tools that time navigations or count them
 * per route. Every started event is followed by exactly one
 * {@link NavigationEndedEvent} for the same navigation, fired on the same
 * thread when the handling has finished, whatever the outcome. A listener can
 * therefore keep timing state in a {@link ThreadLocal}. Both events are fired
 * on the request thread while the session is locked.
 * <p>
 * Only the outermost navigation is reported. The navigations that Flow runs
 * while handling it are part of it and fire no events of their own: a
 * {@link BeforeEvent#forwardTo(String) forward} or
 * {@link BeforeEvent#rerouteTo(String) reroute}, the redirect that adds or
 * removes a trailing slash, and the rendering of an error view. The ended event
 * tells which view was shown in the end.
 * <p>
 * The location and trigger are the ones requested, as they are before any
 * forward or reroute.
 *
 * @see NavigationEndedEvent
 */
public class NavigationStartedEvent extends EventObject {

    private final Location location;
    private final NavigationTrigger trigger;

    /**
     * Creates a new event.
     *
     * @param ui
     *            the UI that navigates, not {@code null}
     * @param location
     *            the requested location, not {@code null}
     * @param trigger
     *            the action that triggered the navigation, not {@code null}
     */
    public NavigationStartedEvent(UI ui, Location location,
            NavigationTrigger trigger) {
        super(ui);
        this.location = location;
        this.trigger = trigger;
    }

    /**
     * Gets the UI that navigates.
     *
     * @return the UI, never {@code null}
     */
    public UI getUI() {
        return (UI) getSource();
    }

    /**
     * Gets the location that was requested, before any forward or reroute.
     *
     * @return the requested location, never {@code null}
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the action that triggered the navigation, such as a page load, a
     * click on a router link or a call to {@link UI#navigate(String)}.
     *
     * @return the navigation trigger, never {@code null}
     */
    public NavigationTrigger getTrigger() {
        return trigger;
    }
}
