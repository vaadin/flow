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

/**
 * A {@link View} that has a slot for a child view.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public interface HasChildView extends View {
    /**
     * Sets the child view to show. This method is invoked for each parent view
     * after {@link #onLocationChange(LocationChangeEvent)} has been called for
     * all used views, if the child view has changed since the last time this
     * method was called. If this view is also used as a child view, the method
     * will be invoked for this view before it's invoked for the parent view.
     *
     * @param childView
     *            the child view to set
     */
    void setChildView(View childView);
}
