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
