/*
 * Copyright (C) 2000-2024 Vaadin Ltd
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

/**
 * A security annotation, granting anonymous access to the Vaadin endpoint (or
 * its method) it is placed onto.
 * <p>
 * This means that any user will be able to trigger an endpoint method (if
 * placed on an endpoint class) or the particular endpoint method (if placed on
 * an endpoint method) without providing an authentication token.
 * <p>
 * If there are other supported security annotations present on the same class
 * or method:
 * <ul>
 * <li>{@link AnonymousAllowed} is overridden by
 * {@link javax.annotation.security.DenyAll} annotation, disallowing any user
 * from accessing the method</li>
 * <li>{@link AnonymousAllowed} annotation overrides
 * {@link javax.annotation.security.PermitAll} and
 * {@link javax.annotation.security.RolesAllowed} annotations, allowing all
 * users to access the method (anonymous and authenticated users with any
 * security roles)</li>
 * </ul>
 *
 * @see AccessAnnotationChecker for security rules check implementation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface AnonymousAllowed {
}
