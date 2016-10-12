/*
 * Copyright 2015 The original authors
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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;

import com.vaadin.navigator.ViewDisplay;
import com.vaadin.spring.internal.UIScopeImpl;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.SingleComponentContainer;

/**
 * Stereotype annotation for a bean (implementing either {@link ViewDisplay},
 * {@link SingleComponentContainer} or {@link ComponentContainer}) that should
 * act as a view container for Vaadin Navigator.
 *
 * There should only be one bean annotated as the view container in the scope of
 * a UI. If a view container bean implements multiple interfaces, it is
 * primarily treated as a {@link ViewDisplay} if possible.
 *
 * @author Vaadin Ltd
 */
@Scope(UIScopeImpl.VAADIN_UI_SCOPE_NAME)
@Retention(RUNTIME)
@Target(TYPE)
@Documented
public @interface ViewContainer {
}
