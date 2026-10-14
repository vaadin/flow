/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationStateBuilder;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteResolver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.server.menu.AvailableViewInfo;

/**
 * Default implementation of the {@link RouteResolver} interface.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class DefaultRouteResolver implements RouteResolver {

    @Override
    public NavigationState resolve(ResolveRequest request) {
        RouteRegistry registry = request.getRouter().getRegistry();

        final String path = request.getLocation().getPath();
        NavigationRouteTarget navigationResult = registry
                .getNavigationRouteTarget(path);

        if (!navigationResult.hasTarget()) {
            Optional<String> clientNavigationTargetPath = RouteUtil
                    .getClientNavigationRouteTargetTemplate(path);
            if (clientNavigationTargetPath.isPresent()) {
                String clientPath = clientNavigationTargetPath.get();
                AvailableViewInfo viewInfo = MenuRegistry.getClientRoutes(false)
                        .get(clientPath.isEmpty() ? clientPath
                                : clientPath.startsWith("/") ? clientPath
                                        : "/" + clientPath);
                if (viewInfo != null && viewInfo.flowLayout()) {

                    Class<? extends RouterLayout> layout = registry
                            .getLayout(path);
                    if (layout == null) {
                        throw new NotFoundException(
                                "No layout for client path '%s'"
                                        .formatted(path));
                    }
                    RouteTarget target = new RouteTarget(
                            (Class<? extends Component>) layout, RouteUtil
                                    .getParentLayoutsForNonRouteTarget(layout));
                    navigationResult = new NavigationRouteTarget(
                            navigationResult.getPath(), target,
                            Collections.emptyMap());
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        NavigationStateBuilder builder = new NavigationStateBuilder(
                request.getRouter());
        try {
            builder.withTarget(navigationResult.getRouteTarget(),
                    navigationResult.getRouteParameters());
            builder.withPath(navigationResult.getPath());
        } catch (NotFoundException nfe) {
            String message = "Exception while navigation to path " + path;
            LoggerFactory.getLogger(this.getClass().getName()).warn(message,
                    nfe);
            throw nfe;
        }

        return builder.build();
    }

}
