/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.router;

import com.vaadin.flow.server.VaadinSession;

/**
 * @author Vaadin Ltd
 *
 */
public class RouteRegistry {

    private static final RouteRegistry APPLICATION_REGISTRY = new RouteRegistry();

    private final VaadinSession session;

    private RouteRegistry() {
        session = null;
    }

    private RouteRegistry(VaadinSession session) {
        this.session = session;
    }

    public static RouteRegistry getApplicationRegistry() {
        return APPLICATION_REGISTRY;
    }

    public static RouteRegistry getSessionRegistry(VaadinSession session) {
        RouteRegistry registry = session.getAttribute(RouteRegistry.class);
        if (registry == null) {
            registry = new RouteRegistry(session);
            session.setAttribute(RouteRegistry.class, registry);
        }
        return registry;
    }

}
