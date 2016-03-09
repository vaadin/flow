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

/**
 * A {@link View} that has a slot for a sub view.
 *
 * @since
 * @author Vaadin Ltd
 */
public interface HasSubView extends View {
    /**
     * Sets the sub view to show. This method is invoked for each parent view
     * after {@link #onLocationChange(Location)} has been called for all used
     * views. If this view is also used as a sub view, the method will be
     * invoked for this view before it's invoked for the parent view.
     *
     * @param subView
     *            the sub view to set
     */
    void setSubView(View subView);
}
