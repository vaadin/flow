/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.security.Principal;
import java.util.Arrays;
import java.util.function.Predicate;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.menu.AvailableViewInfo;

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

    /**
     * Determines if current user has permissions to access the given view.
     * <p>
     * </p>
     * It checks view against authentication state: - If view does not require
     * login -> allow - If not authenticated and login required -> deny. - If
     * user doesn't have correct roles -> deny.
     *
     * @param viewInfo
     *            view info
     * @return true if the view is accessible, false if something is not
     *         authenticated.
     */
    default boolean canAccessView(AvailableViewInfo viewInfo) {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) {
            return !viewInfo.loginRequired();
        }
        return canAccessView(viewInfo, request.getUserPrincipal(),
                request::isUserInRole);
    }

    /**
     * Check view against authentication state.
     * <p>
     * If not authenticated and login required -> invalid. If user doesn't have
     * correct roles -> invalid.
     *
     * @param viewInfo
     *            view info
     * @param principal
     *            current user, can be {@literal null}
     * @param roleChecker
     *            function to authenticate if user has role
     * @return true if accessible, false if something is not authenticated
     */
    static boolean canAccessView(AvailableViewInfo viewInfo,
            Principal principal, Predicate<String> roleChecker) {
        boolean isUserAuthenticated = principal != null;
        if (viewInfo.loginRequired() && !isUserAuthenticated) {
            return false;
        }
        String[] roles = viewInfo.rolesAllowed();
        return roles == null || roles.length == 0
                || Arrays.stream(roles).anyMatch(roleChecker);
    }

}
