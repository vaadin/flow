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

import java.util.ArrayList;
import java.util.List;

import com.vaadin.annotations.Route;
import com.vaadin.annotations.Title;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

/**
 * Handles navigation events by rendering a component of a specific type in the
 * target UI.
 * 
 * @see Route
 */
public abstract class NEW_RouteTargetRenderer implements NavigationHandler {

    /**
     * Gets the route target type to show.
     *
     * @param <T>
     *            the route target type
     * @param event
     *            the event which triggered showing of the route target
     *
     * @return the route target class, not <code>null</code>
     */
    public abstract Class<? extends Component> getRouteTargetType(
            NavigationEvent event);

    /**
     * Gets the component instance to use for the given type and the
     * corresponding navigation event.
     * <p>
     * Override this method to control the creation of view instances.
     * <p>
     * By default always creates new instances.
     *
     * @param <T>
     *            the route target type
     * @param routeTargetType
     *            the class of the route target component
     * @param event
     *            the navigation event that uses the route target
     * @return an instance of the route target component
     */
    protected <T extends Component> T getRouteTarget(Class<T> routeTargetType,
            NavigationEvent event) {
        return ReflectTools.createInstance(routeTargetType);
    }

    @Override
    public int handle(NavigationEvent event) {
        UI ui = event.getUI();

        Class<? extends Component> routeTargetType = getRouteTargetType(event);

        assert routeTargetType != null;

        Component componentInstance = getRouteTarget(routeTargetType, event);

        List<Component> routeTargetChain = new ArrayList<>();
        routeTargetChain.add(componentInstance);

        NEW_LocationChangeEvent locationChangeEvent = createEvent(event,
                routeTargetChain);

        ui.getInternals().showRouteTarget(event.getLocation(),
                componentInstance);

        updatePageTitle(event, routeTargetType);

        return locationChangeEvent.getStatusCode();
    }

    /**
     * Updates the page title according to the currently visible component.
     * <p>
     * Uses the {@link Title} to resolve the title.
     *
     * @param navigationEvent
     *            the event object about the navigation
     * @param routeTargetType
     *            the type of the route target
     */
    protected void updatePageTitle(NavigationEvent navigationEvent,
            Class<? extends Component> routeTargetType) {

        Title annotation = routeTargetType.getAnnotation(Title.class);
        if (annotation == null || annotation.value() == null) {
            navigationEvent.getUI().getPage().setTitle("");
        } else {
            navigationEvent.getUI().getPage().setTitle(annotation.value());
        }
    }

    private NEW_LocationChangeEvent createEvent(NavigationEvent event,
            List<Component> routeTargetChain) {
        return new NEW_LocationChangeEvent(event.getSource(), event.getUI(),
                event.getTrigger(), event.getLocation());
    }
}
