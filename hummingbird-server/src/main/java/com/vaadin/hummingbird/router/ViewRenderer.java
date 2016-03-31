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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.ui.UI;

/**
 * Handles navigation events by rendering a view of a specific type in the
 * target UI. The view can optionally be nested in a chain of parent views.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class ViewRenderer implements NavigationHandler {

    /**
     * Gets the view type show.
     *
     * @return the view type, not <code>null</code>
     */
    public abstract Class<? extends View> getViewType();

    /**
     * Gets the parent view types to show, starting from the parent view
     * immediately wrapping the view type.
     *
     * @return a list of parent view types, not <code>null</code>
     */
    public abstract List<Class<? extends HasChildView>> getParentViewTypes();

    /**
     * Gets the route for which the view types were mapped. Returns
     * <code>null</code> if no route definition is available.
     *
     * @return the route definition, or <code>null</code> if no route is
     *         available
     */
    protected String getRoute() {
        return null;
    }

    @Override
    public void handle(NavigationEvent event) {
        UI ui = event.getUI();

        Class<? extends View> viewType = getViewType();
        List<Class<? extends HasChildView>> parentViewTypes = getParentViewTypes();

        assert viewType != null;
        assert parentViewTypes != null;

        checkDuplicates(viewType, parentViewTypes);

        try {
            // Instances currently in use that we want to reuse if possible
            Map<Class<? extends View>, View> availableInstances = ui
                    .getActiveViewChain().stream()
                    .collect(Collectors.toMap(i -> i.getClass(), i -> i));

            View viewInstance = reuseOrCreate(viewType, availableInstances);

            List<View> viewChain = new ArrayList<>();
            viewChain.add(viewInstance);

            for (Class<? extends HasChildView> parentType : parentViewTypes) {
                viewChain.add(reuseOrCreate(parentType, availableInstances));
            }

            LocationChangeEvent locationChangeEvent = createEvent(event,
                    viewChain);

            // Notify view and parent views about the new location
            viewChain.forEach(
                    view -> view.onLocationChange(locationChangeEvent));

            @SuppressWarnings("unchecked")
            List<HasChildView> parentViews = (List<HasChildView>) (List<?>) viewChain
                    .subList(1, viewChain.size());

            // Show the new view and parent views
            ui.getFrameworkData().showView(event.getLocation(), viewInstance,
                    parentViews);

            updatePageTitle(event, locationChangeEvent);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Cannot instantiate view", e);
        }
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
                event.getLocation(), viewChain, routePlaceholders);
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

        Optional<Location> maybeCurrentDestination = Optional.of(destination);
        Optional<RouteLocation> maybeCurrentRouteDefinition = Optional
                .of(routeDefinition);

        while (maybeCurrentRouteDefinition.isPresent()) {
            RouteLocation currentRouteDefinition = maybeCurrentRouteDefinition
                    .get();
            Location currentDestination = maybeCurrentDestination.get();

            if (currentRouteDefinition.startsWithPlaceholder()) {
                String placeholderName = currentRouteDefinition
                        .getPlaceholderName();

                String placeholderValue = currentDestination.getFirstSegment();

                assert !routePlaceholders.containsKey(placeholderName);
                routePlaceholders.put(placeholderName, placeholderValue);
            } else if (currentRouteDefinition.startsWithWildcard()) {
                assert !routePlaceholders.containsKey("*");

                routePlaceholders.put("*", currentDestination.getPath());
            }

            maybeCurrentDestination = currentDestination.getSubLocation();
            maybeCurrentRouteDefinition = currentRouteDefinition
                    .getRouteSubLocation();
        }

        return routePlaceholders;
    }

    private static <T extends View> T reuseOrCreate(Class<T> type,
            Map<Class<? extends View>, View> availableInstances)
            throws InstantiationException, IllegalAccessException {
        T instance = type.cast(availableInstances.remove(type));
        if (instance == null) {
            instance = type.newInstance();
        }
        return instance;
    }
}
