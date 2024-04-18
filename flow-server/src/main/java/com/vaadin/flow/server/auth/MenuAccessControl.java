/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow.server.auth;

import java.io.Serializable;

/**
 * Interface for controlling access to routes in the application's menu
 * component.
 */
public interface MenuAccessControl extends Serializable {

    /**
     * Enum for controlling how the client-side menu should be populated.
     */
    enum PopulateClientMenu {
        /**
         * Always populate the menu with server side routes.
         */
        ALWAYS,
        /**
         * Never populate the menu with server side routes.
         */
        NEVER,
        /**
         * Populate the menu with server side routes only if client-side menu
         * exists.
         */
        AUTOMATIC
    };

    /**
     * Sets whether the Hilla application's main menu should be populated
     * automatically with server side routes and therefore routes information
     * sent to the browser. Three possible values:
     * {@link PopulateClientMenu#ALWAYS}, {@link PopulateClientMenu#NEVER},
     * {@link PopulateClientMenu#AUTOMATIC}.
     *
     * @param populateClientSideMenu
     *            whether the client-side menu should be populated with server
     *            side routes
     */
    void setPopulateClientSideMenu(PopulateClientMenu populateClientSideMenu);

    /**
     * Gets whether the Hilla application's main menu should be populated
     * automatically with server side routes and therefore routes information
     * sent to the browser.
     *
     * @return enum of type {@link PopulateClientMenu}
     */
    PopulateClientMenu getPopulateClientSideMenu();

}
