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
package com.vaadin.flow.internal;

import jakarta.servlet.ServletContextListener;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServlet;

/**
 * Allows to run initialization of {@link VaadinContext} which for some reasons
 * may not be done via {@link ServletContextListener}.
 * <p>
 * The functionality is intended to internal usage only. The implementation of
 * this interface may be available as an attribute in a {@link VaadinContext}.
 * In the latter case {@link VaadinServlet#init()} method will run
 * {@link #initialize(VaadinContext)} method.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface VaadinContextInitializer {

    /**
     * Initializes the Vaadin {@code context}.
     *
     * @param context
     *            the Vaadin context instance
     */
    void initialize(VaadinContext context);

}
