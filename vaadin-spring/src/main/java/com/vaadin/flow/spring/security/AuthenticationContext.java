/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * An interface to access the authentication context of the application.
 * <p>
 * An instance of this interface is available for injection as bean in view and
 * layout classes.
 *
 * @author Vaadin Ltd
 * @since 23.3
 */
public interface AuthenticationContext {

    /**
     * Gets an {@link Optional} with an instance of the current user if it has
     * been authenticated, or empty if the user is not authenticated.
     *
     * @param <U>
     *            the type parameter of the expected user instance
     * @param userType
     *            the type of the expected user instance, a subclass of
     *            {@link UserDetails}
     * @return an {@link Optional} with the current authenticated user, or empty
     *         if none available
     */
    <U extends UserDetails> Optional<U> getAuthenticatedUser(Class<U> userType);

    /**
     * Gets an {@link Optional} with an instance of the current user if it has
     * been authenticated, or empty if the user is not authenticated.
     *
     * @return an {@link Optional} with the current authenticated user, or empty
     *         if none available
     */
    default Optional<UserDetails> getAuthenticatedUser() {
        return getAuthenticatedUser(UserDetails.class);
    }

    /**
     * Initiates the logout process of the current authenticated user by
     * invalidating the local session and then notifying
     * {@link org.springframework.security.web.authentication.logout.LogoutHandler}.
     */
    void logout();
}