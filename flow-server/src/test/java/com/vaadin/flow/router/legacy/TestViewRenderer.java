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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.legacy.HasChildView;
import com.vaadin.flow.router.legacy.View;
import com.vaadin.flow.router.legacy.ViewRenderer;

/**
 * An {@link ViewRenderer} where the view types are decided at construction
 * time.
 *
 * @author Vaadin Ltd
 */
public class TestViewRenderer extends ViewRenderer {

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
    public TestViewRenderer(Class<? extends View> viewType,
            Class<? extends HasChildView>... parentViewTypes) {
        this(viewType, Arrays.asList(parentViewTypes));
    }

    /**
     * Creates a renderer for the given view type and an optional list of parent
     * view types. The same type may not be used in multiple positions in the
     * same view renderer.
     *
     * @param viewType
     *            the view type to show
     * @param parentViewTypes
     *            a list of parent view types to show, starting from the parent
     *            view immediately wrapping the view type
     */
    public TestViewRenderer(Class<? extends View> viewType,
            List<Class<? extends HasChildView>> parentViewTypes) {
        ViewRenderer.checkDuplicates(viewType, parentViewTypes);

        this.viewType = viewType;
        this.parentViewTypes = parentViewTypes;
    }

    @Override
    public Class<? extends View> getViewType(NavigationEvent event) {
        return viewType;
    }

    @Override
    public List<Class<? extends HasChildView>> getParentViewTypes(
            NavigationEvent event, Class<? extends View> viewType) {
        return Collections.unmodifiableList(parentViewTypes);
    }

    @Override
    protected String getRoute() {
        return null;
    }
}
