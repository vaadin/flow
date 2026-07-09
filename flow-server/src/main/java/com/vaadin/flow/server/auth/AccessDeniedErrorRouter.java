/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
     *
     * @return Type of the access denied exception for the access denied error
     *         view.
     */
    Class<? extends RuntimeException> rerouteToError() default AccessDeniedException.class;
}
