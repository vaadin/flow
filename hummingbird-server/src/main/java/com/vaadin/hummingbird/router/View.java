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

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.ui.UI;

/**
 * A view that can be shown in a {@link UI} or {@link HasChildView}.
 *
 * @since
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface View {
    /**
     * Gets the element to show.
     *
     * @return the element
     */
    Element getElement();

    /**
     * Called when the location changes. This method is called for all view
     * instances that will be in use for the new location, starting from the the
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
     * By default returns the value specified with {@link Title @Title}
     * annotation, or <code>null</code> if {@link Title @Title} is not present.
     * <p>
     * Note that returning <code>null</code> will always keep the previously
     * used page title.
     * <p>
     * An empty string will clear any previous page title. In that case the
     * browser will decide what to show as the title.
     * <p>
     * This method is triggered after the
     * {@link #onLocationChange(LocationChangeEvent)} callback and after the
     * view has been attached to the UI.
     *
     * @param locationChangeEvent
     *            event object with information about the new location
     * @return the page title to set
     */
    default String getTitle(LocationChangeEvent locationChangeEvent) {
        // by default use whatever was defined with the title-annotation or if
        // it is not available, null is used to keep previous
        return AnnotationReader.getPageTitle(getClass());
    }
}
