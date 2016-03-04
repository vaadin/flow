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
 * A view class implementing this interface will get notified whenever the
 * location changes while the view is active. The view will also get notified
 * when the view is first activated.
 *
 * @since
 * @author Vaadin Ltd
 */
public interface LocationChangeHandler {
    /**
     * Called when the location changes.
     * 
     * @param location
     *            the new location, not null
     */
    void onLocationChange(Location location);
}
