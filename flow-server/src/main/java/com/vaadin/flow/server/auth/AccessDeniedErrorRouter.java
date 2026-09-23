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
package com.vaadin.flow.server.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.router.AccessDeniedException;

/**
 * Annotation for customizing route specific rerouting of access denied error in
 * {@link AnnotatedViewAccessChecker}. Annotation is to be used together with
 * {@code @Route}, or if present, together with access annotation listed here:
 * <ul>
 * <li>{@code @AnonymousAllowed}
 * <li>{@code @PermitAll}
 * <li>{@code @RolesAllowed}
 * <li>{@code @DenyAll}
 * </ul>
 * For example, following TestView and SubView routes would reroute user without
 * "admin" role to CustomAccessDeniedError error page:
 *
 * <pre>
 * &#64;AccessDeniedErrorRouter(rerouteToError = CustomAccessDeniedException.class)
 * &#64;RolesAllowed("admin")
 * &#64;Route("test")
 * public class TestView extends Div {
 * }
 *
 * &#64;AccessDeniedErrorRouter(rerouteToError = CustomAccessDeniedException.class)
 * &#64;RolesAllowed("admin")
 * public class ParentView extends Div {
 * }
 *
 * &#64;Route("subview")
 * public class SubView extends ParentView {
 * }
 *
 * public class CustomAccessDeniedException extends RuntimeException {
 *     public CustomAccessDeniedException() {
 *     }
 *
 *     public CustomAccessDeniedException(String message) {
 *         super(message);
 *     }
 * }
 *
 * &#64;Tag(Tag.DIV)
 * public class CustomAccessDeniedError
 *         implements {@code HasErrorParameter<CustomAccessDeniedException>} {
 *
 *     &#64;Override
 *     public int setErrorParameter(BeforeEnterEvent event,
 *             {@code ErrorParameter<CustomAccessDeniedException>} parameter) {
 *         getElement().setText(parameter.hasCustomMessage()
 *                 ? parameter.getCustomMessage() : "Access denied.");
 *         return HttpStatusCode.UNAUTHORIZED.getCode();
 *     }
 * }
 * </pre>
 * <p>
 * Note that the exception class named by {@link #rerouteToError()} is
 * instantiated reflectively when access is denied, so it needs to have a public
 * no-arg constructor. That is why {@code CustomAccessDeniedException} above
 * declares one explicitly: giving the exception only a message constructor
 * would remove the implicit no-arg constructor and make the access denied
 * navigation fail with an internal server error instead of showing the error
 * view.
 * 
 * @since 24.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface AccessDeniedErrorRouter {

    /**
     * Reroute access denied error by the given exception. Exception is
     * {@link AccessDeniedException} by default. It can be changed to other
     * exception like {@link com.vaadin.flow.router.NotFoundException} or any
     * other exception mapped to
     * {@link com.vaadin.flow.router.HasErrorParameter} error view.
     * <p>
     * Exception class needs to have default no-arg constructor, since the
     * exception is instantiated by
     * {@link com.vaadin.flow.router.BeforeEvent#rerouteToError(Class, String)}
     * when access is denied. The exception does not need to carry the reason
     * for the denial: the reason is passed separately to the error view and is
     * available there as
     * {@link com.vaadin.flow.router.ErrorParameter#getCustomMessage()}.
     *
     * @return Type of the access denied exception for the access denied error
     *         view.
     */
    Class<? extends RuntimeException> rerouteToError() default AccessDeniedException.class;
}
