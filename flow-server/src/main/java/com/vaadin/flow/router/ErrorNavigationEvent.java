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

import com.vaadin.flow.component.UI;

/**
 * Event object with data related to error navigation.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ErrorNavigationEvent extends NavigationEvent {

    private final ErrorParameter<?> errorParameter;

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
     * @param errorParameter
     *            parameter containing navigation error information
     */
    public ErrorNavigationEvent(Router router, Location location, UI ui,
            NavigationTrigger trigger, ErrorParameter<?> errorParameter) {
        super(router, location, ui, trigger);

        this.errorParameter = errorParameter;
    }

    /**
     * Gets the ErrorParameter if set.
     *
     * @return set error parameter or null if not set
     */
    public ErrorParameter<?> getErrorParameter() {
        return errorParameter;
    }
}
