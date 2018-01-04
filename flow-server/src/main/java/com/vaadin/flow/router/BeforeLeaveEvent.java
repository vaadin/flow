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
package com.vaadin.flow.router;

import com.vaadin.flow.router.internal.ContinueNavigationAction;

/**
 * Event created before navigation happens.
 *
 * @author Vaadin Ltd
 */
public class BeforeLeaveEvent extends BeforeEvent {

    private ContinueNavigationAction continueNavigationAction = null;

    /**
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on going
     * @param navigationTarget
     *            Navigation target
     */
    public BeforeLeaveEvent(NavigationEvent event, Class<?> navigationTarget) {
        super(event, navigationTarget);
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
     */
    public BeforeLeaveEvent(RouterInterface router, NavigationTrigger trigger,
            Location location, Class<?> navigationTarget) {
        super(router, trigger, location, navigationTarget);
    }

    /**
     * Initiates the postponement of the current navigation transition, allowing
     * a listener to e.g. display a confirmation dialog before finishing the
     * transition.
     * <p>
     * This is only valid while leaving (deactivating) a page; if the method is
     * called while entering / activating the new page, it will throw an
     * {@link IllegalStateException}.
     *
     * @return the action to run when the transition is to be resumed, or null
     */
    public ContinueNavigationAction postpone() {
        continueNavigationAction = new ContinueNavigationAction();
        return continueNavigationAction;
    }

    /**
     * Checks whether this event was postponed.
     *
     * @return true if the event was postponed, false otherwise
     */
    public boolean isPostponed() {
        return continueNavigationAction != null;
    }

    /**
     * Gets the action used to resume this event, if it was postponed.
     *
     * @return the action used to resume this event if it was postponed, or null
     *         if it is not being postponed
     */
    public ContinueNavigationAction getContinueNavigationAction() {
        return continueNavigationAction;
    }

}
