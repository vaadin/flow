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

/**
 * Event passed to {@link View#onLocationChange(ViewLocationChangeEvent)} when
 * the context in which a view will be used changes.
 *
 * @since
 * @author Vaadin Ltd
 */
public class LocationChangeEvent extends EventObject {

    private View view;
    private List<HasChildView> parentViews;
    private Location location;
    private List<View> viewChain;
    private RouterUI ui;

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
     */
    public LocationChangeEvent(Router router, RouterUI ui, Location location,
            List<View> viewChain) {
        super(router);

        assert ui != null;
        assert location != null;
        assert viewChain != null;

        this.ui = ui;
        this.location = location;
        this.viewChain = Collections.unmodifiableList(viewChain);
    }

    /**
     * Gets the new location
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

    @Override
    public Router getSource() {
        return (Router) super.getSource();
    }

}
