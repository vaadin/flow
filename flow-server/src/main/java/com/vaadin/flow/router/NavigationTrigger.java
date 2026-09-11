/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.router;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;

/**
 * The type of user action that triggered navigation.
 *
 * @see LocationChangeEvent#getTrigger()
 * @see HistoryStateChangeEvent#getTrigger()
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public enum NavigationTrigger {
    /**
     * Navigation was triggered by the user opening the application or reloading
     * in the browser.
     */
    PAGE_LOAD,

    /**
     * Navigation was triggered by the user following a router link.
     *
     * @see RouterLink
     */
    ROUTER_LINK,

    /**
     * Navigation was triggered by the user going forward or back in the
     * browser's history.
     */
    HISTORY,

    /**
     * Navigation was triggered programmatically via forward/reroute action.
     */
    PROGRAMMATIC,

    /**
     * Navigation was triggered via
     * {@link UI#navigate(String, QueryParameters)}. It's for internal use only.
     * 
     * @since 4.0
     */
    UI_NAVIGATE,

    /**
     * Navigation was triggered by client-side.
     * 
     * @since 3.0
     */
    CLIENT_SIDE,

    /**
     * Navigation is for a reload event on a preserveOnRefresh route.
     * 
     * @since 23.2.8
     */
    REFRESH,

    /**
     * Navigation was triggered via {@link UI#refreshCurrentRoute(boolean)}.
     * It's for internal use only.
     * 
     * @since 24.5.8
     */
    REFRESH_ROUTE,

}
