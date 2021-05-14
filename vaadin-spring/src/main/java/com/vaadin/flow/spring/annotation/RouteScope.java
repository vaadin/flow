/*
 * Copyright 2000-2021 Vaadin Ltd.
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
 * The lifecycle of a {@link RouteScope @RouteScope} bean is controlled by
 * route navigation.
 * <p>
 * Every scoped bean belongs to one router component owner. It can be a
 * {@link Route @Route}, or a {@link RouterLayout}, or a
 * {@link HasErrorParameter HasErrorParameter}. Beans are qualified by
 * {@link RouteScopeOwner @RouteScopeOwner} to link with their owner.
 * <p>
 * As long as the owner component stays attached, all beans owned by it
 * remain in the scope.
 * <p>
 * When a scoped bean is a router component, an owner can be any ancestor
 * {@link RouterLayout}, or the bean itself. Without the {@link RouteScopeOwner}
 * annotation the scope effectively behaves as prototype scope.
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
