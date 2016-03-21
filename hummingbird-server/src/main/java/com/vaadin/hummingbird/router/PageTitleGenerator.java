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

import java.io.Serializable;

import com.vaadin.ui.Page;

/**
 * A {@link Page#setTitle(String) page title} generator based on a
 * {@link LocationChangeEvent}.
 * <p>
 * By default {@link DefaultPageTitleGenerator} is used.
 */
@FunctionalInterface
public interface PageTitleGenerator extends Serializable {

    /**
     * Gets the page title to set based on the new location.
     * <p>
     * Returning a <code>null</code> page title will keep the previous title.
     *
     * @param event
     *            the event object with information about the new location
     * @param viewRenderer
     *            the viewRenderer handling the event
     * @return the page title to set
     */
    String getPageTitle(LocationChangeEvent event, ViewRenderer viewRenderer);
}
