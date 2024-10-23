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

package com.vaadin.flow.spring.security;

import java.security.Principal;
import java.util.Optional;
import java.util.function.Predicate;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.spring.AuthenticationUtil;

/**
 * Helper class to access user information and perform roles checks.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class SecurityUtil {

    /**
     * Gets the principal for the currently logged in user.
     *
     * @param request
     *            the current request or {@code null} if no request is in
     *            progress (e.g. in a background thread)
     * @return a representation of the currently logged in user or {@code null}
     *         if no user is currently logged in
     */
    static Principal getPrincipal(VaadinRequest request) {
        if (request == null) {
            return AuthenticationUtil.getSecurityHolderAuthentication();
        }
        return request.getUserPrincipal();
    }

    /**
     * Gets a function for checking roles for the currently logged in user.
     *
     * @param request
     *            the current request or {@code null} if no request is in
     *            progress (e.g. in a background thread)
     * @return a function which takes a role name and returns {@code true} if
     *         the user is included in that role
     */
    static Predicate<String> getRolesChecker(VaadinRequest request) {
        if (request == null) {
            return Optional.ofNullable(VaadinService.getCurrent())
                    .map(service -> service.getContext()
                            .getAttribute(Lookup.class))
                    .map(lookup -> lookup.lookup(VaadinRolePrefixHolder.class))
                    .map(VaadinRolePrefixHolder::getRolePrefix)
                    .map(AuthenticationUtil::getSecurityHolderRoleChecker)
                    .orElseGet(
                            AuthenticationUtil::getSecurityHolderRoleChecker)::apply;
        }

        // Update active role prefix if it's not set yet.
        Optional.ofNullable(request.getService())
                .map(service -> service.getContext().getAttribute(Lookup.class))
                .map(lookup -> lookup.lookup(VaadinRolePrefixHolder.class))
                .filter(prefixHolder -> !prefixHolder.isSet()).ifPresent(
                        prefixHolder -> prefixHolder.resetRolePrefix(request));

        return request::isUserInRole;
    }

}
