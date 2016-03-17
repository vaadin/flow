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

import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * Event passed to {@link View#onLocationChange(LocationChangeEvent)} when the
 * context in which a view will be used changes.
 *
 * @since
 * @author Vaadin Ltd
 */
public class LocationChangeEvent extends EventObject {

    private final Location location;
    private final List<View> viewChain;
    private final RouterUI ui;
    private final Map<String, String> routePlaceholders;

    /**
     * Creates a new location change event.
     *
     * @param router
     *            the router that triggered the change, not <code>null</code>
     * @param ui
     *            the UI in which the view is used, not <code>null</code>
     * @param location
     *            the new location, not <code>null</code>
     * @param viewChain
     *            the view chain that will be used, not <code>null</code>
     * @param routePlaceholders
     *            a map containing actual path segment values used for
     *            placeholders in the used route mapping, not <code>null</code>
     */
    public LocationChangeEvent(Router router, RouterUI ui, Location location,
            List<View> viewChain, Map<String, String> routePlaceholders) {
        super(router);

        assert ui != null;
        assert location != null;
        assert viewChain != null;
        assert routePlaceholders != null;

        this.ui = ui;
        this.location = location;
        this.viewChain = Collections.unmodifiableList(viewChain);
        this.routePlaceholders = Collections.unmodifiableMap(routePlaceholders);
    }

    /**
     * Gets the new location.
     *
     * @return the new location, not <code>null</code>
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the chain of views that will be nested inside the UI, starting from
     * the most deeply nested view.
     *
     * @return the view chain, not <code>null</code>
     */
    public List<View> getViewChain() {
        return viewChain;
    }

    /**
     * Gets the UI in which the view is shown.
     *
     * @return the UI, not <code>null</code>
     */
    public RouterUI getUI() {
        return ui;
    }

    /**
     * Gets the part of the location that matched the <code>*</code> part of the
     * route.
     *
     * @return the wildcard part of the path, or <code>null</code> if not using
     *         a wildcard route
     */
    public String getRouteWildcard() {
        return getRoutePlaceholderValue("*");
    }

    /**
     * Gets the part of the location that matched <code>{placeholderName}</code>
     * of the route.
     *
     * @param placeholderName
     *            the name of the placeholder, not <code>null</code>
     * @return the placeholder value, or <code>null</code> if the placeholder
     *         name was not present in the route
     */
    public String getRoutePlaceholderValue(String placeholderName) {
        assert placeholderName != null;
        assert !placeholderName.contains("{") && !placeholderName.contains("}");

        return routePlaceholders.get(placeholderName);
    }

    @Override
    public Router getSource() {
        return (Router) super.getSource();
    }

}
