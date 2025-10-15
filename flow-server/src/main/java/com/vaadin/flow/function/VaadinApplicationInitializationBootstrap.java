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
package com.vaadin.flow.function;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletException;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;

/**
 * Represents Vaadin web application initialization bootstrap.
 * <p>
 * This is internal mechanism for bootstrapping Vaadin web application
 * initialization. It's executed before servlet initialization once the
 * {@code Lookup} instance is created. The internal implementation setups the
 * {@link Lookup} instance in the {@link VaadinContext} so that it becomes
 * available via {@link VaadinContext#getAttribute(Class)} and bootstraps all
 * initializers (basically {@link ServletContainerInitializer} impls) that
 * depends on {@link Lookup} presence.
 *
 * @author Vaadin Ltd
 *
 */
@FunctionalInterface
public interface VaadinApplicationInitializationBootstrap {

    /**
     * Bootstraps Vaadin application initialization.
     *
     * @param lookup
     *            a lookup instance required for initialization
     * @throws ServletException
     *             if lookup initialization failed
     */
    void bootstrap(Lookup lookup) throws ServletException;
}
