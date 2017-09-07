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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.annotations.Route;
import com.vaadin.annotations.Title;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasElement;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

/**
 * Handles navigation events by rendering a component of a specific type in the
 * target UI.
 *
 * @see Route
 */
public abstract class RouteTargetRenderer implements NavigationHandler {

    /**
     * Gets the route target type to show.
     *
     * @param event
     *            the event which triggered showing of the route target
     *
     * @return the route target type, not <code>null</code>
     */
    public abstract Class<? extends Component> getRouteTargetType(
            NavigationEvent event);

    /**
     * Gets the parent router layout types to show for the given target type,
     * starting from the parent layout immediately wrapping the target type.
     *
     * @see #getRouteTargetType(NavigationEvent)
     *
     * @param event
     *            the event which triggered showing of the target component and
     *            its parents
     * @param targetType
     *            target type to show
     *
     * @return a list of parent component types, not <code>null</code>
     */
    public abstract List<Class<? extends RouterLayout>> getParentTargetTypes(
            NavigationEvent event, Class<? extends Component> targetType);

    /**
     * Gets the component instance to use for the given type and the
     * corresponding navigation event.
     * <p>
     * Override this method to control the creation of view instances.
     * <p>
     * By default always creates new instances.
     *
     * @param routeTargetType
     *            the type of the route target component
     * @param event
     *            the navigation event that uses the route target
     * @return an instance of the route target component
     */
    protected <T extends HasElement> T getRouteTarget(Class<T> routeTargetType,
            NavigationEvent event) {
        return ReflectTools.createInstance(routeTargetType);
    }

    @Override
    public int handle(NavigationEvent event) {
        UI ui = event.getUI();

        Class<? extends Component> targetType = getRouteTargetType(event);
        List<Class<? extends RouterLayout>> parentLayoutTypes = getParentTargetTypes(
                event, targetType);

        assert targetType != null;
        assert parentLayoutTypes != null;

        checkDuplicates(targetType, parentLayoutTypes);

        Component routeTarget = getRouteTarget(targetType, event);

        List<HasElement> routeTargetChain = new ArrayList<>();
        routeTargetChain.add(routeTarget);

        for (Class<? extends RouterLayout> parentType : parentLayoutTypes) {
            routeTargetChain.add(getRouteTarget(parentType, event));
        }

        NewLocationChangeEvent locationChangeEvent = createEvent(event,
                routeTargetChain);

        @SuppressWarnings("unchecked")
        List<RouterLayout> parentViews = (List<RouterLayout>) (List<?>) routeTargetChain
                .subList(1, routeTargetChain.size());

        // Show the new view and parent views
        ui.getInternals().showRouteTarget(event.getLocation(), routeTarget,
                parentViews);

        updatePageTitle(event, targetType);

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

    /**
     * Checks that the same type is not used in multiple parts of a router
     * targets chain.
     *
     * @param routeTargetType
     *            the actual router target in the chain
     * @param parentLayouts
     *            the parent types in the view chain
     */
    protected static void checkDuplicates(
            Class<? extends Component> routeTargetType,
            Collection<Class<? extends RouterLayout>> parentLayouts) {
        Set<Class<?>> duplicateCheck = new HashSet<>();
        duplicateCheck.add(routeTargetType);
        for (Class<?> parentType : parentLayouts) {
            if (!duplicateCheck.add(parentType)) {
                throw new IllegalArgumentException(
                        parentType + " is used in multiple locations");
            }
        }
    }

    private NewLocationChangeEvent createEvent(NavigationEvent event,
            List<HasElement> routeTargetChain) {
        return new NewLocationChangeEvent(event.getSource(), event.getUI(),
                event.getTrigger(), event.getLocation(), routeTargetChain);
    }
}
