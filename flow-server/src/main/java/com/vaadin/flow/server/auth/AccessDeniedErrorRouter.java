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
 * }
 *
 * &#64;Tag(Tag.DIV)
 * public class CustomAccessDeniedError
 *         implements {@code HasErrorParameter<CustomAccessDeniedException>} {
 *
 *     &#64;Override
 *     public int setErrorParameter(BeforeEnterEvent event,
 *             {@code ErrorParameter<CustomAccessDeniedException>} parameter) {
 *         getElement().setText("Access denied.");
 *         return HttpStatusCode.UNAUTHORIZED.getCode();
 *     }
 * }
 * </pre>
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
     *
     * @return Type of the access denied exception for the access denied error
     *         view.
     */
    Class<? extends RuntimeException> rerouteToError() default AccessDeniedException.class;
}
