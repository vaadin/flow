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
package com.vaadin.flow.router.internal;

import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationStateBuilder;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteResolver;
import com.vaadin.flow.server.RouteRegistry;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@link RouteResolver} interface.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class DefaultRouteResolver implements RouteResolver {

    @Override
    public NavigationState resolve(ResolveRequest request) {
        RouteRegistry registry = request.getRouter().getRegistry();

        final String path = request.getLocation().getPath();
        NavigationRouteTarget navigationResult = getNavigationTarget(registry,
                path);

        if (!navigationResult.hasTarget()) {
            return null;
        }
        
        NavigationStateBuilder builder = new NavigationStateBuilder(
                request.getRouter());
        try {
            builder.withTarget(navigationResult.getRouteTarget(),
                    navigationResult.getUrlParameters());
            builder.withPath(navigationResult.getPath());
        } catch (NotFoundException nfe) {
            String message = "Exception while navigation to path " + path;
            LoggerFactory.getLogger(this.getClass().getName()).warn(message,
                    nfe);
            throw nfe;
        }

        return builder.build();
    }

    private NavigationRouteTarget getNavigationTarget(
            RouteRegistry registry, String path) throws NotFoundException {
        return registry.getNavigationRouteTarget(path);
    }

}
