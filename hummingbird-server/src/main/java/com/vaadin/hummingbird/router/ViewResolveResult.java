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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

import com.vaadin.ui.HasSubView;
import com.vaadin.ui.View;

/**
 * The result of resolving a URL to a view type, optionally nested in a chain of
 * parent view types.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ViewResolveResult implements ResolveResult {

    private final Class<? extends View> viewType;
    // Starts with the view's immediate parent
    private final Class<? extends HasSubView>[] parentViewTypes;

    /**
     * Creates a resolve result for the given view type and optional parent view
     * types.
     *
     * @param viewType
     *            the view type to show
     * @param parentViewTypes
     *            an array of parent view types to show, starting from the
     *            parent view immediately wrapping the view type
     */
    @SafeVarargs
    public ViewResolveResult(Class<? extends View> viewType,
            Class<? extends HasSubView>... parentViewTypes) {
        this.viewType = viewType;
        this.parentViewTypes = parentViewTypes;
    }

    @Override
    public void show(NavigationEvent event) {
        RouterUI ui = event.getUI();

        try {
            // Linked list since we will be adding to the beginning
            LinkedList<HasSubView> parentViews = new LinkedList<>();

            // Starts with the view, followed by its immediate parent
            List<View> viewChain = ui.getViewChain();
            ListIterator<View> activeChainIterator = viewChain
                    .listIterator(viewChain.size());

            ListIterator<Class<? extends HasSubView>> parentTypeIterator = Arrays
                    .asList(parentViewTypes)
                    .listIterator(parentViewTypes.length);

            // Reuse or create instances for the requested parent view types
            while (parentTypeIterator.hasPrevious()) {
                Class<? extends HasSubView> parentType = parentTypeIterator
                        .previous();

                HasSubView parentView = null;
                while (activeChainIterator.hasPrevious()) {
                    View previousParentView = activeChainIterator.previous();
                    if (previousParentView.getClass() == parentType) {
                        // Reuse if a match is found
                        parentView = (HasSubView) previousParentView;
                        break;
                    }
                }

                if (parentView == null) {
                    parentView = parentType.newInstance();
                }

                parentViews.addFirst(parentView);
            }

            // Reuse or create a view instance
            View viewInstance;
            if (!viewChain.isEmpty()
                    && viewChain.get(0).getClass() == viewType) {
                viewInstance = viewChain.get(0);
            } else {
                viewInstance = viewType.newInstance();
            }

            // Notify view and parent views about the new location
            Stream.concat(Stream.of(viewInstance), parentViews.stream())
                    .forEach(
                            view -> view.onLocationChange(event.getLocation()));

            // Show the new view and parent views
            ui.showView(viewInstance, parentViews);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Target view or layout cannot be instantiated", e);
        }
    }
}
