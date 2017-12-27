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

import com.vaadin.flow.component.page.Page;

/**
 * A {@link Page#setTitle(String) page title} generator based on a
 * {@link LocationChangeEvent}.
 * <p>
 * By default {@link DefaultPageTitleGenerator} is used.
 *
 * @see RouterConfiguration#setPageTitleGenerator(PageTitleGenerator)
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
@FunctionalInterface
public interface PageTitleGenerator extends Serializable {

    /**
     * Gets the page title to set based on the new location.
     * <p>
     * Returning an empty string will clear any previous title that has been set
     * and let the browser decide what to show as the title.
     * <p>
     * May <b>NOT</b> return <code>null</code>.
     *
     * @param event
     *            the event object with information about the new location
     * @return the page title to set, not <code>null</code>
     */
    String getPageTitle(LocationChangeEvent event);
}
