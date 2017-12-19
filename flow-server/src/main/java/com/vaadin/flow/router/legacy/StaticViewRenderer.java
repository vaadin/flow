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
package com.vaadin.flow.router.legacy;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.router.NavigationEvent;

/**
 * A simple view renderer that always uses the given view type with the parent
 * views configured for it.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class StaticViewRenderer extends ViewRenderer {

    private final String route;
    private final Class<? extends View> viewType;

    /**
     * Creates a new view renderer for the given view type and route.
     *
     * @param viewType
     *            the view type to show, not <code>null</code>
     * @param route
     *            the route to use when evaluating path parameters for the view,
     *            or <code>null</code> if the view should not get any path
     *            parameters
     */
    public StaticViewRenderer(Class<? extends View> viewType, String route) {
        if (viewType == null) {
            throw new IllegalArgumentException("viewType cannot be null");
        }

        this.route = route;
        this.viewType = viewType;
    }

    @Override
    public Class<? extends View> getViewType(NavigationEvent event) {
        return viewType;
    }

    @Override
    public List<Class<? extends HasChildView>> getParentViewTypes(
            NavigationEvent event, Class<? extends View> viewType) {
        assert viewType == this.viewType;
        return event.getSource().getConfiguration().getParentViewsAscending(viewType)
                .collect(Collectors.toList());
    }

    @Override
    protected String getRoute() {
        return route;
    }

}
