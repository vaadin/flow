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

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.Principal;
import java.util.Objects;
import java.util.function.Function;

import com.vaadin.flow.server.VaadinServletRequest;

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
public class AccessAnnotationChecker implements Serializable {

    /**
     * Checks if the user defined by the current active servlet request (using
     * {@link HttpServletRequest#getUserPrincipal()} and
     * {@link HttpServletRequest#isUserInRole(String)} has access to the given
     * method.
     *
     * @param method
     *            the method to check access to
     * @return {@code true} if the user has access to the given method,
     *         {@code false} otherwise
     */
    public boolean hasAccess(Method method) {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        if (request == null) {
            throw new IllegalStateException(
                    "No request is available. This method can only be used with an active VaadinServletRequest");
        }
        return hasAccess(method, request);
    }

    /**
     * Checks if the user defined by the current active servlet request (using
     * {@link HttpServletRequest#getUserPrincipal()} and
     * {@link HttpServletRequest#isUserInRole(String)} has access to the given
     * class.
     *
     * @param cls
     *            the class to check access to
     * @return {@code true} if the user has access to the given method,
     *         {@code false} otherwise
     */
    public boolean hasAccess(Class<?> cls) {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        if (request == null) {
            throw new IllegalStateException(
                    "No request is available. This method can only be used with an active VaadinServletRequest");
        }
        return hasAccess(cls, request);
    }

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
    public boolean hasAccess(Method method, HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("The request cannot be null");
        }
        return hasAccess(method, request.getUserPrincipal(),
                request::isUserInRole);
    }

    /**
     *
     * Checks if the user defined by the request (using
     * {@link HttpServletRequest#getUserPrincipal()} and
     * {@link HttpServletRequest#isUserInRole(String)} has access to the given
     * class.
     *
     * @param cls
     *            the class to check access to
     * @param request
     *            the http request to use for user information
     * @return {@code true} if the user has access to the given method,
     *         {@code false} otherwise
     */
    public boolean hasAccess(Class<?> cls, HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("The request cannot be null");
        }
        return hasAccess(cls, request.getUserPrincipal(),
                request::isUserInRole);
    }

    /**
     * Checks if the user defined by the given {@link Principal} and role
     * checker has access to the given method.
     *
     * @param method
     *            the method to check access to
     * @param principal
     *            the principal of the user
     * @param roleChecker
     *            a function that can answer if a user has a given role
     * @return {@code true} if the user has access to the given method,
     *         {@code false} otherwise
     */
    public boolean hasAccess(Method method, Principal principal,
            Function<String, Boolean> roleChecker) {
        return hasAccess(getSecurityTarget(method), principal, roleChecker);
    }

    /**
     * Checks if the user defined by the given {@link Principal} and role
     * checker has access to the given class.
     *
     * @param cls
     *            the class to check access to
     * @param principal
     *            the principal of the user
     * @param roleChecker
     *            a function that can answer if a user has a given role
     * @return {@code true} if the user has access to the given method,
     *         {@code false} otherwise
     */
    public boolean hasAccess(Class<?> cls, Principal principal,
            Function<String, Boolean> roleChecker) {
        return hasAccess(getSecurityTarget(cls), principal, roleChecker);
    }

    /**
     * Gets the method or class to check for security restrictions.
     *
     * @param method
     *            the method to look up
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

    /**
     * Gets the class to check for security restrictions.
     *
     * @param cls
     *            the class to check
     * @return the first annotated class in {@code cls}'s hierarchy that
     *         annotated with one of the access annotations, starting from the
     *         input {@code cls} class itself, going up in the hierarchy.
     *         <em>Note:</em> interfaces in the {@code cls}'s hierarchy are
     *         ignored.
     *         <p>
     *         If no class in the hierarchy was annotated with any of the access
     *         annotations, the {@code cls} input parameter itself would be
     *         returned.
     *         <p>
     *         Access annotations that being checked are:
     *         <ul>
     *         <li>{@code @AnonymousAllowed}
     *         <li>{@code @PermitAll}
     *         <li>{@code @RolesAllowed}
     *         <li>{@code @DenyAll}
     *         </ul>
     *
     * @throws NullPointerException
     *             if the input {@code cls} is null
     */
    public AnnotatedElement getSecurityTarget(Class<?> cls) {
        return securityTarget(cls);
    }

    static AnnotatedElement securityTarget(Class<?> cls) {
        Objects.requireNonNull(cls, "The input Class must not be null.");

        Class<?> clazz = cls;
        while (clazz != null && clazz != Object.class) {
            if (hasSecurityAnnotation(clazz)) {
                return clazz;
            }
            clazz = clazz.getSuperclass();
        }
        return cls;
    }

    private boolean hasAccess(AnnotatedElement annotatedClassOrMethod,
            Principal principal, Function<String, Boolean> roleChecker) {
        if (annotatedClassOrMethod.isAnnotationPresent(DenyAll.class)) {
            return false;
        }
        if (annotatedClassOrMethod
                .isAnnotationPresent(AnonymousAllowed.class)) {
            return true;
        }
        if (principal == null) {
            return false;
        }
        RolesAllowed rolesAllowed = annotatedClassOrMethod
                .getAnnotation(RolesAllowed.class);
        if (rolesAllowed == null) {
            return annotatedClassOrMethod.isAnnotationPresent(PermitAll.class);
        } else {
            return roleAllowed(rolesAllowed, roleChecker);
        }
    }

    private boolean roleAllowed(RolesAllowed rolesAllowed,
            Function<String, Boolean> roleChecker) {
        for (String role : rolesAllowed.value()) {
            if (roleChecker.apply(role)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasSecurityAnnotation(AnnotatedElement method) {
        return method.isAnnotationPresent(AnonymousAllowed.class)
                || method.isAnnotationPresent(PermitAll.class)
                || method.isAnnotationPresent(DenyAll.class)
                || method.isAnnotationPresent(RolesAllowed.class);
    }

}
