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
package com.vaadin.flow.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.page.AppShellConfigurator;

/**
 *
 * Defines routes to load eagerly.
 * <p>
 * All dependencies for the routes you add to this annotation will be loaded
 * when the user first opens the application. Dependencies for other routes will
 * be loaded when the corresponding route is visited the first time.
 * <p>
 * Define the classes you want to load eagerly as parameters, e.g.
 * {@code @LoadDependenciesOnStartup({MainView.class,
 * TheOtherEntryPointView.class})} to load the {@code MainView} and
 * {@code TheOtherEntryPointView}.
 * <p>
 * If you this annotation without any parameters, i.e.
 * {@code @LoadDependenciesOnStartup()} then the dependencies for all routes
 * will be loaded on startup.
 * <p>
 * This annotation must be added to the class implementing
 * {@link AppShellConfigurator}.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LoadDependenciesOnStartup {

    /**
     * The views for which to load dependencies when the application is opened
     * for the first time.
     * <p>
     * Note that all classes must extend {@link Component}. The the type is
     * {@code Class<?>} because of a VS Code issue.
     *
     * @return a collection of views to load eagerly or an empty array to load
     *         all dependencies eagerly
     */
    Class<?>[] value() default {};

}
