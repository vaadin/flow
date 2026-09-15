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
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Scope;

import com.vaadin.flow.component.UI;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * The lifecycle of a UIScoped component is bound to a browser tab.
 * <p>
 * Injection with this annotation will create a direct reference to the object
 * rather than a proxy.
 * <p>
 * There are some limitations when not using proxies. Circular referencing (that
 * is, injecting A to B and B to A) will not work. Injecting into a larger scope
 * will bind the instance from the currently active smaller scope, and will
 * ignore smaller scope change. For example after being injected into session
 * scope it will point to the same {@link UIScoped} bean instance ( even its
 * {@link UI} is closed ) regardless of {@link UI} change.
 * <p>
 * The sister annotation to this is the {@link NormalUIScoped}. Both annotations
 * reference the same underlying scope, so it is possible to get both a proxy
 * and a direct reference to the same object by using different annotations.
 */
@Scope
@Inherited
@Target({ ANNOTATION_TYPE, TYPE, FIELD, METHOD, CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface UIScoped {
}
