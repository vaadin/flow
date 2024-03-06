/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

/**
 * Link a {@link RouteScope @RouteScope} bean to its owner.
 * <p>
 * Owner is a router component. A {@link Route @Route}, or a
 * {@link RouterLayout}, or a {@link HasErrorParameter}.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD,
        ElementType.PARAMETER })
public @interface RouteScopeOwner {

    /**
     * Owner class of the qualified {@link RouteScope @RouteScope} bean.
     * <p>
     * A {@link Route @Route}, or a {@link RouterLayout}, or a
     * {@link HasErrorParameter}
     *
     * @return owner class
     */
    Class<? extends HasElement> value();
}
