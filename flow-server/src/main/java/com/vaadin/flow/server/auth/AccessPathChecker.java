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
import java.util.function.Predicate;

/**
 * Checks if a user has access to a given route path.
 * <p>
 * </p>
 * The path to check is relative to the Vaadin application and does not contain
 * any container specific details such as context or servlet path.
 * <p>
 * </p>
 * In addition, the path is never {@literal null} and never starts with a "/"
 * character.
 * <p>
 * </p>
 * Implementors of this interface are meant to be used in combination with
 * {@link RoutePathAccessChecker}.
 *
 * @see RoutePathAccessChecker
 */
public interface AccessPathChecker extends Serializable {

    /**
     * Checks if the user defined by the given {@link Principal} and role
     * checker has access to the given path.
     * <p>
     * The {@code path} is relative to the Vaadin application and does not
     * contain any container specific details such as context or servlet path.
     * <p>
     * The {@code path} is never {@literal null} and never starts with a "/"
     * character.
     *
     * @param path
     *            the path to check access to
     * @param principal
     *            the principal of the user
     * @param roleChecker
     *            a function that can answer if a user has a given role
     * @return {@code true} if the user has access to the given path,
     *         {@code false} otherwise.
     */
    boolean hasAccess(String path, Principal principal,
            Predicate<String> roleChecker);

}
