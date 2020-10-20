/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;

/**
 * Internal collection of initializers which may not be executed immediately but
 * requires a {@link Lookup} instance which will be set in the
 * {@link VaadinContext} ({@link ServletContext}) only when
 * {@link LookupInitializer} completed.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
class DeferredServletContextInitializers {

    interface Initializer {
        void init(ServletContext context) throws ServletException;
    }

    private final List<Initializer> initializers = new CopyOnWriteArrayList<>();

    void addInitializer(Initializer initializer) {
        initializers.add(initializer);
    }

    void runInitializers(ServletContext context) throws ServletException {
        for (Initializer initializer : initializers) {
            initializer.init(context);
        }
    }
}
