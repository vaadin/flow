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
package com.vaadin.quarkus.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Scope;

import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The lifecycle of a RouteScoped component is controlled by route navigation.
 * <p>
 * Every RouteScoped bean belongs to one router component owner. It can be a
 * {@link Route @Route} component, or a {@link RouterLayout}, or a
 * {@link HasErrorParameter HasErrorParameter}. Beans are qualified by
 * {@link RouteScopeOwner @RouteScopeOwner} to link with their owner.
 * <p>
 * Until owner remains active, all beans owned by it remain in the scope.
 * <p>
 * Without the {@link RouteScopeOwner} annotation the owner is the current route
 * target component (dynamically calculated). With nested routing hierarchies,
 * the target is the "leaf" or "bottom most" routing component. The beans are
 * preserved as long as the owner component remains in the navigation chain. It
 * means that the bean may be preserved even if the navigation target is changed
 * (but the "initial" calculated owner is still in the navigation chain).
 * <p>
 * Injection with this annotation will create a direct reference to the object
 * rather than a proxy.
 * <p>
 * There are some limitations when not using proxies. Circular referencing (that
 * is, injecting A to B and B to A) will not work. Injecting into a larger scope
 * will bind the instance from the currently active smaller scope, and will
 * ignore smaller scope change. For example after being injected into session
 * scope it will point to the same RouteScoped bean instance ( even it is
 * destroyed ) regardless of UI, or any navigation change.
 * <p>
 * The sister annotation to this is the {@link NormalRouteScoped}. Both
 * annotations reference the same underlying scope, so it is possible to get
 * both a proxy and a direct reference to the same object by using different
 * annotations.
 */
@Scope
@Inherited
@Target({ ANNOTATION_TYPE, TYPE, FIELD, METHOD, CONSTRUCTOR })
@Retention(RUNTIME)
public @interface RouteScoped {
}
