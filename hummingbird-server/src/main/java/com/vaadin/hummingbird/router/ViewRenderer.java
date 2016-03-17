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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles navigation events by rendering a view of a specific type in the
 * target UI. The view can optionally be nested in a chain of parent views.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ViewRenderer implements NavigationHandler {

    private final Class<? extends View> viewType;
    // Starts with the view's immediate parent
    private final List<Class<? extends HasChildView>> parentViewTypes;

    /**
     * Creates a renderer for the given view type and optional parent view
     * types. The same type may not be used in multiple positions in the same
     * view renderer.
     *
     * @param viewType
     *            the view type to show
     * @param parentViewTypes
     *            an array of parent view types to show, starting from the
     *            parent view immediately wrapping the view type
     */
    @SafeVarargs
    public ViewRenderer(Class<? extends View> viewType,
            Class<? extends HasChildView>... parentViewTypes) {
        this(viewType, Arrays.asList(parentViewTypes));
    }

    /**
     * Creates a renderer for the given view type and optional an optional list
     * of parent view types. The same type may not be used in multiple positions
     * in the same view renderer.
     *
     * @param viewType
     *            the view type to show
     * @param parentViewTypes
     *            a list of parent view types to show, starting from the parent
     *            view immediately wrapping the view type
     */
    public ViewRenderer(Class<? extends View> viewType,
            List<Class<? extends HasChildView>> parentViewTypes) {
        Set<Class<?>> duplicateCheck = new HashSet<>();
        duplicateCheck.add(viewType);
        for (Class<?> parentType : parentViewTypes) {
            if (!duplicateCheck.add(parentType)) {
                throw new IllegalArgumentException(
                        parentType + " is used in multiple locations");
            }
        }

        this.viewType = viewType;
        this.parentViewTypes = parentViewTypes;
    }

    @Override
    public void handle(NavigationEvent event) {
        RouterUI ui = event.getUI();

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

            LocationChangeEvent locationChangeEvent = new LocationChangeEvent(
                    event.getSource(), ui, event.getLocation(), viewChain);

            // Notify view and parent views about the new location
            viewChain.forEach(
                    view -> view.onLocationChange(locationChangeEvent));

            @SuppressWarnings("unchecked")
            List<HasChildView> parentViews = (List<HasChildView>) (List<?>) viewChain
                    .subList(1, viewChain.size());

            // Show the new view and parent views
            ui.showView(event.getLocation(), viewInstance, parentViews);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Cannot instantiate view", e);
        }
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
