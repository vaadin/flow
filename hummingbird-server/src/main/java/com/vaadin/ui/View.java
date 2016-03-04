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
package com.vaadin.ui;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.router.Location;

/**
 * A view that can be shown in a {@link UI} or {@link HasSubView}.
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
     * Called when the location changes. This method does nothing by default.
     *
     * @param location
     *            the new location, not <code>null</code>
     */
    default void onLocationChange(Location location) {
        // Does nothing by default
    }
}
