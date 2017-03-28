/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;

import com.vaadin.spring.internal.ViewScopeImpl;

/**
 * Stereotype annotation for Spring's {@code @Scope("vaadin-view")}. The
 * lifecycle of a bean in this scope starts when a user navigates to a view that
 * refers to the bean, and ends when the user navigates out of the view (or the
 * Vaadin UI itself is destroyed). Please note that the
 * {@link com.vaadin.navigator.View class} itself must also be in this scope. In
 * other words, it is <b>not</b> possible to use view scoped beans inside a
 * prototype or {@link com.vaadin.spring.annotation.UIScope UI} scoped view.
 *
 * @author Petter Holmström (petter@vaadin.com)
 */
@Scope(ViewScopeImpl.VAADIN_VIEW_SCOPE_NAME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ViewScope {
}
