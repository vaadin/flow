/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server.connect.auth;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;

/**
 * Checks if a given user has access to a given method.
 * <p>
 * Check is performed as follows when called for a method:
 * <ol>
 * <li>A security annotation (see below) is searched for on that particular
 * method.</li>
 * <li>If a security annotation was not found on the method, checks the class
 * the method is declared in.</li>
 * <li>If no security annotation was found, deny access by default</li>
 * </ol>
 * <p>
 * The security annotations checked and their meaning are:
 * <ul>
 * <li>{@link AnonymousAllowed} - allows access to any logged on or not logged
 * in user. Public access.</li>
 * <li>{@link PermitAll} - allows access to any logged in user but denies access
 * to anonymous users.</li>
 * <li>{@link RolesAllowed} - allows access there is a logged in user that has
 * any of the roles mentioned in the annotation</li>
 * <li>{@link DenyAll} - denies access.</li>
 * </ul>
 */
public class AccessAnnotationChecker {

    /**
     * 
     * Checks if the user defined by the request (using
     * {@link HttpServletRequest#getUserPrincipal()} and
     * {@link HttpServletRequest#isUserInRole(String)} has access to the given
     * method.
     * 
     * @param method
     *            the method to check access to
     * @param request
     *            the http request to use for user information
     * @return {@code true} if the user has access to the given method,
     *         {@code false} otherwise
     */
    public boolean annotationAllowsAccess(Method method,
            HttpServletRequest request) {
        return annotationAllowsAccess(getSecurityTarget(method), request);
    }

    /**
     * Gets the entity to check for security restrictions.
     *
     * @param method
     *            the method to analyze, not {@code null}
     * @return the entity that is responsible for security settings for the
     *         method passed
     * @throws IllegalArgumentException
     *             if the method is not public
     */
    public AnnotatedElement getSecurityTarget(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException(String.format(
                    "The method '%s' is not public hence cannot have a security target",
                    method));
        }
        return hasSecurityAnnotation(method) ? method
                : method.getDeclaringClass();
    }

    private boolean annotationAllowsAccess(
            AnnotatedElement annotatedClassOrMethod,
            HttpServletRequest request) {
        if (annotatedClassOrMethod.isAnnotationPresent(DenyAll.class)) {
            return false;
        }
        if (annotatedClassOrMethod
                .isAnnotationPresent(AnonymousAllowed.class)) {
            return true;
        }
        if (request.getUserPrincipal() == null) {
            return false;
        }
        RolesAllowed rolesAllowed = annotatedClassOrMethod
                .getAnnotation(RolesAllowed.class);
        if (rolesAllowed == null) {
            return annotatedClassOrMethod.isAnnotationPresent(PermitAll.class);
        } else {
            return roleAllowed(rolesAllowed, request);
        }
    }

    private boolean roleAllowed(RolesAllowed rolesAllowed,
            HttpServletRequest request) {
        for (String role : rolesAllowed.value()) {
            if (request.isUserInRole(role)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasSecurityAnnotation(Method method) {
        return method.isAnnotationPresent(AnonymousAllowed.class)
                || method.isAnnotationPresent(PermitAll.class)
                || method.isAnnotationPresent(DenyAll.class)
                || method.isAnnotationPresent(RolesAllowed.class);
    }

}
