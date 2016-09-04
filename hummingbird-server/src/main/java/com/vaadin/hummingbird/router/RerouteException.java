/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

/**
 * Exception thrown from {@link View#onLocationChange(LocationChangeEvent)} to
 * bail out from showing that view and instead use an alternative navigation
 * handler.
 *
 * @author Vaadin Ltd
 */
public class RerouteException extends Exception {
    // Helper handler that delegates to the error handler of the used router
    private static final NavigationHandler errorTargetHandler = new NavigationHandler() {
        @Override
        public int handle(NavigationEvent event) {
            return event.getSource().getConfiguration().getErrorHandler()
                    .handle(event);
        }
    };

    private final NavigationHandler targetHandler;

    /**
     * Creates a new reroute exception with the given navigation handler.
     *
     * @param navigationHandler
     *            the navigation handler to use for continuing navigation, not
     *            <code>null</code>
     */
    public RerouteException(NavigationHandler navigationHandler) {
        if (navigationHandler == null) {
            throw new IllegalArgumentException("Target handler cannot be null");
        }
        targetHandler = navigationHandler;
    }

    /**
     * Gets the new navigation handler that should be used for resolving the
     * current navigation.
     *
     * @return the navigation handler to use, not <code>null</code>
     */
    public NavigationHandler getNavigationHandler() {
        return targetHandler;
    }

    /**
     * Creates a reroute exception that causes the application's default error
     * view to be shown.
     *
     * @return a reroute exception showing the default error view, not
     *         <code>null</code>
     */
    public static RerouteException errorView() {
        return new RerouteException(errorTargetHandler);
    }

}
