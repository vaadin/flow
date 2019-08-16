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

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.UI;

/**
 * Event created before navigation happens.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class BeforeLeaveEvent extends BeforeEvent {

    private ContinueNavigationAction continueNavigationAction = null;

    /**
     * The action to resume a postponed {@link BeforeEnterEvent}.
     *
     * @author Vaadin Ltd
     * @since 1.0.
     */
    public class ContinueNavigationAction implements Serializable {

        private NavigationHandler handler = null;
        private NavigationEvent event = null;

        private ContinueNavigationAction() {
        }

        /**
         * Sets the navigation {@code handler} and the navigation {@code event}
         * for this action.
         *
         * @param handler
         *            the navigation handler
         * @param event
         *            the navigation event
         */
        public void setReferences(NavigationHandler handler,
                NavigationEvent event) {
            if (event != null) {
                event.getUI().getSession().hasLock();
            } else {
                assert UI.getCurrent() != null
                        && UI.getCurrent().getSession().hasLock();
            }
            this.handler = handler;
            this.event = event;
        }

        /**
         * Resumes the page transition associated with the postponed event.
         */
        public void proceed() {
            BeforeLeaveEvent.this.continueNavigationAction = null;
            if (handler != null && event != null) {
                if (!event.getUI().getSession().hasLock()) {
                    throw new IllegalStateException(
                            "The method 'proceed' may not be called without the session lock. "
                                    + "Use UI.access() to execute any UI related code from a separate thread properly");
                }

                handler.handle(event);
                setReferences(null, null);
            }
        }
    }

    /**
     * Construct event from a NavigationEvent.
     *
     * @param event
     *            NavigationEvent that is on-going
     * @param navigationTarget
     *            Navigation target
     * @deprecated Use {@link #BeforeLeaveEvent(NavigationEvent, Class, List)}
     *             instead.
     */
    @Deprecated
    public BeforeLeaveEvent(NavigationEvent event, Class<?> navigationTarget) {
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
    public BeforeLeaveEvent(NavigationEvent event, Class<?> navigationTarget,
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
     *             {@link #BeforeLeaveEvent(Router, NavigationTrigger, Location, Class, UI, List)}
     *             instead.
     */
    @Deprecated
    public BeforeLeaveEvent(Router router, NavigationTrigger trigger,
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
    public BeforeLeaveEvent(Router router, NavigationTrigger trigger,
            Location location, Class<?> navigationTarget, UI ui,
            List<Class<? extends RouterLayout>> layouts) {
        super(router, trigger, location, navigationTarget, ui, layouts);
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
