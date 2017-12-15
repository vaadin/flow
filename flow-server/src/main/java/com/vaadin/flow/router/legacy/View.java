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

import java.io.Serializable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.util.AnnotationReader;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.HasElement;

/**
 * A view that can be shown in a {@link UI} or {@link HasChildView}.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
@FunctionalInterface
public interface View extends HasElement, Serializable {

    /**
     * Gets the element which is shown when the user enters the view.
     * <p>
     * This methods is used by the framework to update what is shown when the
     * user navigates between views. The returned element will be attached to
     * the parent view if there is one. Otherwise it will be attached to the UI
     * element (the {@literal <body>} tag).
     * <p>
     * Typically a view extends {@link Component}, which implements this method.
     * <p>
     * The returned element must not change during the lifecycle of the view.
     *
     * @return the root element for the view
     */
    @Override
    Element getElement();

    /**
     * Called when the location changes. This method is called for all view
     * instances that will be in use for the new location, starting from the
     * actual view and progressing upwards in the hierarchy through each used
     * parent view. It is called for both newly created view instances and
     * instances that were used for the previous location but that are now
     * reused. This method is called before the view's root element is attached
     * to the DOM, but care should be taken since a view instance that is reused
     * in the same location might not be detached and attached back again.
     * <p>
     * A view class can override this method to dynamically update its contents
     * based on the location, e.g. to fetch content from a database based on an
     * identifier in the URL.
     * <p>
     * This method can call any of the <code>reroute</code> methods from the
     * provided location change event to make some other view or navigation
     * target be used instead of this view.
     * <p>
     * This method does nothing by default.
     *
     * @param locationChangeEvent
     *            event object with information about the new location
     */
    default void onLocationChange(LocationChangeEvent locationChangeEvent) {
        // Does nothing by default
    }

    /**
     * Get the page title to show for this view.
     * <p>
     * By default returns the value specified with the
     * {@link PageTitle @PageTitle} annotation, or an empty string if the
     * annotation is not present. The empty string will clear any previously set
     * title. In that case the browser will decide what to show as the title.
     * <p>
     * May <b>NOT</b> return <code>null</code>.
     * <p>
     * This method is triggered after the
     * {@link #onLocationChange(LocationChangeEvent)} callback and after the
     * view has been attached to the UI.
     *
     * @param locationChangeEvent
     *            event object with information about the new location
     * @return the page title to set, not <code>null</code>
     */
    default String getTitle(LocationChangeEvent locationChangeEvent) {
        // by default use whatever was defined with the title-annotation or if
        // it is not available, "" to clear any previous title
        return AnnotationReader.getPageTitle(getClass()).orElse("");
    }
}
