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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Link a {@link NormalRouteScoped @NormalRouteScoped} bean to its owner.
 * <p>
 * Owner is a router component. A {@link Route @Route}, or a
 * {@link RouterLayout}, or a {@link HasErrorParameter}.
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface RouteScopeOwner {
    /**
     * Owner class of the qualified {@link NormalRouteScoped @NormalRouteScoped}
     * bean.
     * <p>
     * A {@link Route @Route}, or a {@link RouterLayout}, or a
     * {@link HasErrorParameter}
     *
     * @return owner class
     */
    Class<? extends HasElement> value();
}
