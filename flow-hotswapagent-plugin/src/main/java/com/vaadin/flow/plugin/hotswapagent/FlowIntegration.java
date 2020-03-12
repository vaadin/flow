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
package com.vaadin.flow.plugin.hotswapagent;

import java.util.HashSet;

import org.hotswap.agent.logging.AgentLogger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * Flow integration. Methods should be executed in the application classloader.
 *
 * To see the debug log messages, add the line
 *
 * <code>LOGGER = debug</code>
 *
 * to hotswap-agent.properties.
 */
public class FlowIntegration {

    private static AgentLogger LOGGER = AgentLogger
            .getLogger(FlowIntegration.class);

    private VaadinServlet vaadinServlet = null;

    /**
     * Sets the Vaadin service once the servlet has been instantiated.
     * 
     * @param servlet
     *            the Vaadin service
     */
    public void servletInitialized(VaadinServlet servlet) {
        vaadinServlet = servlet;
        LOGGER.info("{} initialized for servlet {}", getClass(), servlet);
    }

    /**
     * Update Flow route registry and push refresh to UIs (concrete parameter
     * types as {@link org.hotswap.agent.command.ReflectionCommand} determines
     * the method from actual argument types).
     * 
     * @param modifiedClasses
     *            returns classes that have been added or modified
     * @param deletedClasses
     *            returns classes that have been deleted
     */
    public void reload(HashSet<Class<?>> modifiedClasses,
                       HashSet<Class<?>> deletedClasses) {
        assert (vaadinServlet != null);

        LOGGER.debug("The following classes were modified:");
        modifiedClasses.forEach(cls -> LOGGER.debug("+ {}", cls));

        LOGGER.debug("The following classes were deleted:");
        deletedClasses.forEach(cls -> LOGGER.debug("- {}", cls));

        VaadinContext vaadinContext = vaadinServlet.getService().getContext();
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(vaadinContext);
        RouteConfiguration routeConf = RouteConfiguration.forRegistry(registry);

        registry.getRegisteredRoutes().stream()
                .map(RouteData::getNavigationTarget)
                .filter(deletedClasses::contains)
                .forEach(clazz -> {
                    LOGGER.debug("Removing route to {}", clazz);
                    routeConf.removeRoute(clazz);
                });
        // add new routes to registry
        modifiedClasses.stream().filter(Component.class::isAssignableFrom)
                .filter(clazz -> clazz.isAnnotationPresent(Route.class))
                .forEach(clazz -> {
                    Class<? extends Component> componentClass = (Class<? extends Component>) clazz;
                    LOGGER.debug("Updating route {} to class {}",
                            componentClass.getAnnotation(Route.class).value(),
                            clazz);
                    routeConf.removeRoute(componentClass);
                    routeConf.setAnnotatedRoute(componentClass);
                });

        // TODO: trigger a browser reload

        // clear the modified classes for next reload
        modifiedClasses.clear();
        deletedClasses.clear();
    }
}
