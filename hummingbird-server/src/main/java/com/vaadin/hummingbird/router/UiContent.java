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

import com.vaadin.ui.HasContent;
import com.vaadin.ui.HasElement;

public class UiContent implements Content {

    private final Class<? extends HasElement> viewType;
    // Innermost at the end
    private final Class<? extends HasContent>[] layoutTypes;

    @SafeVarargs
    public UiContent(Class<? extends HasElement> viewType,
            Class<? extends HasContent>... layoutTypes) {
        this.viewType = viewType;
        this.layoutTypes = layoutTypes;
    }

    public Class<? extends HasElement> getView() {
        return viewType;
    }

    @Override
    public void show(NavigationEvent event) {
        RouterUI ui = event.getUI();

        try {
            // Linked list since we will be adding to the beginning
            LinkedList<HasContent> layouts = new LinkedList<>();

            // Innermost is at the end
            List<HasElement> viewChain = ui.getViewChain();
            ListIterator<HasElement> activeChainIterator = viewChain
                    .listIterator(viewChain.size());

            ListIterator<Class<? extends HasContent>> layoutTypeIterator = Arrays
                    .asList(layoutTypes).listIterator(layoutTypes.length);

            // Reuse or create instances for the requested layout types
            while (layoutTypeIterator.hasPrevious()) {
                Class<? extends HasContent> layoutType = layoutTypeIterator
                        .previous();

                HasContent layout = null;
                while (activeChainIterator.hasPrevious()) {
                    HasElement previousLayout = activeChainIterator.previous();
                    if (previousLayout.getClass() == layoutType) {
                        // Reuse if a match is found
                        layout = (HasContent) previousLayout;
                        break;
                    }
                }

                if (layout == null) {
                    layout = layoutType.newInstance();
                }

                layouts.addFirst(layout);
            }

            // Reuse or create view instance
            HasElement viewInstance;
            if (!viewChain.isEmpty()
                    && viewChain.get(0).getClass() == viewType) {
                viewInstance = viewChain.get(0);
            } else {
                viewInstance = viewType.newInstance();
            }

            // Notify view and layout about the new location
            Stream.concat(Stream.of(viewInstance), layouts.stream())
                    .forEach(potentialHandler -> {
                        if (potentialHandler instanceof LocationChangeHandler) {
                            ((LocationChangeHandler) potentialHandler)
                                    .onLocationChange(event.getLocation());
                        }
                    });

            // Show the new view in the layouts
            ui.showView(viewInstance, layouts);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Target view or layout cannot be instantiated", e);
        }
    }
}
