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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.legacy.RouteLocation.RouteSegmentVisitor;
import com.vaadin.flow.util.ReflectTools;
import com.vaadin.ui.UI;

/**
 * Handles navigation events by rendering a view of a specific type in the
 * target UI. The view can optionally be nested in a chain of parent views.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public abstract class ViewRenderer implements NavigationHandler {

    /**
     * Gets the view type to show.
     *
     * @param event
     *            the event which triggered showing of the view
     *
     * @return the view type, not <code>null</code>
     */
    public abstract Class<? extends View> getViewType(NavigationEvent event);

    /**
     * Gets the parent view types to show for the given view type, starting from
     * the parent view immediately wrapping the view type.
     *
     * @see #getViewType(NavigationEvent)
     *
     * @param event
     *            the event which triggered showing of the view and its parents
     * @param viewType
     *            view type to show
     *
     * @return a list of parent view types, not <code>null</code>
     */
    public abstract List<Class<? extends HasChildView>> getParentViewTypes(
            NavigationEvent event, Class<? extends View> viewType);

    /**
     * Gets the view instance to use for the given view type and the
     * corresponding navigation event.
     * <p>
     * Override this method to control the creation of view instances.
     * <p>
     * By default reuses any currently used view instances if possible.
     *
     * @param <T>
     *            the type of the view
     * @param viewType
     *            the type of the view
     * @param event
     *            the navigation event that uses the view
     * @return an instance of the view
     */
    @SuppressWarnings("unchecked")
    protected <T extends View> T getView(Class<T> viewType,
            NavigationEvent event) {
        Optional<View> currentInstance = event.getUI().getInternals()
                .getActiveViewChain().stream()
                .filter(view -> view.getClass().equals(viewType)).findAny();
        return (T) currentInstance
                .orElseGet(() -> ReflectTools.createInstance(viewType));
    }

    /**
     * Gets the route for which the view types were mapped. Returns
     * <code>null</code> if no route definition is available.
     *
     * @return the route definition, or <code>null</code> if no route is
     *         available
     */
    protected abstract String getRoute();

    @Override
    public int handle(NavigationEvent event) {
        UI ui = event.getUI();

        Class<? extends View> viewType = getViewType(event);
        List<Class<? extends HasChildView>> parentViewTypes = getParentViewTypes(
                event, viewType);

        assert viewType != null;
        assert parentViewTypes != null;

        checkDuplicates(viewType, parentViewTypes);

        View viewInstance = getView(viewType, event);

        List<View> viewChain = new ArrayList<>();
        viewChain.add(viewInstance);

        for (Class<? extends HasChildView> parentType : parentViewTypes) {
            viewChain.add(getView(parentType, event));
        }

        LocationChangeEvent locationChangeEvent = createEvent(event, viewChain);

        // Notify view and parent views about the new location
        for (View view : viewChain) {
            view.onLocationChange(locationChangeEvent);

            // Use the new navigation handler if a reroute target was set
            Optional<NavigationHandler> rerouteTarget = locationChangeEvent
                    .getRerouteTarget();
            if (rerouteTarget.isPresent()) {
                return rerouteTarget.get().handle(event);
            }
        }

        @SuppressWarnings("unchecked")
        List<HasChildView> parentViews = (List<HasChildView>) (List<?>) viewChain
                .subList(1, viewChain.size());

        // Show the new view and parent views
        ui.getInternals().showView(event.getLocation(), viewInstance,
                parentViews);

        updatePageTitle(event, locationChangeEvent);

        return locationChangeEvent.getStatusCode();
    }

    /**
     * Updates the page title according to the currently opened views.
     * <p>
     * Uses the {@link RouterConfiguration#getPageTitleGenerator()} to resolve
     * the title.
     *
     * @param navigationEvent
     *            the event object about the navigation
     * @param locationChangeEvent
     *            event object with information on the new location
     */
    protected void updatePageTitle(NavigationEvent navigationEvent,
            LocationChangeEvent locationChangeEvent) {
        PageTitleGenerator pageTitleGenerator = navigationEvent.getSource()
                .getConfiguration().getPageTitleGenerator();

        String title = pageTitleGenerator.getPageTitle(locationChangeEvent);

        assert title != null : "You cannot not use a null title. Use an "
                + "explicit title or an empty string \"\" which will clear "
                + "previous title.";

        // Page.setTitle will throw IAE for null title
        navigationEvent.getUI().getPage().setTitle(title);
    }

    private LocationChangeEvent createEvent(NavigationEvent event,
            List<View> viewChain) {

        String route = getRoute();
        Map<String, String> routePlaceholders;
        if (route != null) {
            routePlaceholders = extractRoutePlaceholders(event.getLocation(),
                    new RouteLocation(new Location(route)));
        } else {
            routePlaceholders = Collections.emptyMap();
        }

        return new LocationChangeEvent(event.getSource(), event.getUI(),
                event.getTrigger(), event.getLocation(), viewChain,
                routePlaceholders);
    }

    /**
     * Checks that the same view type is not used in multiple parts of a view
     * chain.
     *
     * @param viewType
     *            the actual view in the view chain
     * @param parentViewTypes
     *            the parent types in the view chain
     */
    protected static void checkDuplicates(Class<? extends View> viewType,
            Collection<Class<? extends HasChildView>> parentViewTypes) {
        Set<Class<?>> duplicateCheck = new HashSet<>();
        duplicateCheck.add(viewType);
        for (Class<?> parentType : parentViewTypes) {
            if (!duplicateCheck.add(parentType)) {
                throw new IllegalArgumentException(
                        parentType + " is used in multiple locations");
            }
        }
    }

    private static Map<String, String> extractRoutePlaceholders(
            Location destination, RouteLocation routeDefinition) {
        assert destination != null;
        assert routeDefinition != null;

        Map<String, String> routePlaceholders = new HashMap<>();

        routeDefinition.visitSegments(new RouteSegmentVisitor() {
            Optional<Location> maybeCurrentDestination = Optional
                    .of(destination);

            private String getNextDestinationSegment() {
                Location currentDestination = maybeCurrentDestination.get();
                /*
                 * This might return an empty optional, but in that case there
                 * shouldn't be any more calls to any accept method, as long as
                 * the current destination actually matches the route
                 * definition.
                 */
                maybeCurrentDestination = currentDestination.getSubLocation();

                return currentDestination.getFirstSegment();
            }

            @Override
            public void acceptWildcard() {
                // This is always the last step, so not need to advance the
                // destination
                routePlaceholders.put("*",
                        maybeCurrentDestination.get().getPath());
            }

            @Override
            public void acceptSegment(String segmentName) {
                // Just advance the current destination
                getNextDestinationSegment();
            }

            @Override
            public void acceptPlaceholder(String placeholderName) {
                String placeholderValue = getNextDestinationSegment();

                assert !routePlaceholders.containsKey(placeholderName);
                routePlaceholders.put(placeholderName, placeholderValue);
            }
        });

        return routePlaceholders;
    }

}
