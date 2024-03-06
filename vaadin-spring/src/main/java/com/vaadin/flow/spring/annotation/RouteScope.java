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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;

import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.scopes.VaadinRouteScope;

/**
 * The lifecycle of a {@link RouteScope @RouteScope} bean is controlled by route
 * navigation.
 * <p>
 * Every scoped bean belongs to one router component owner. It can be a
 * {@link Route @Route}, or a {@link RouterLayout}, or a
 * {@link HasErrorParameter HasErrorParameter}. Beans are qualified by
 * {@link RouteScopeOwner @RouteScopeOwner} to link with their owner.
 * <p>
 * As long as the owner component stays attached, all beans owned by it remain
 * in the scope.
 * <p>
 * Without the {@link RouteScopeOwner} annotation the owner is the current route
 * target component (dynamically calculated). With nested routing hierarchies,
 * the target is the "leaf" or "bottom most" routing component. The beans are
 * preserved as long as the owner component remains in the navigation chain. It
 * means that the bean may be preserved even if the navigation target is changed
 * (but the "initial" calculated owner is still in the navigation chain).
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@Scope(VaadinRouteScope.VAADIN_ROUTE_SCOPE_NAME)
@Inherited
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RouteScope {

}
