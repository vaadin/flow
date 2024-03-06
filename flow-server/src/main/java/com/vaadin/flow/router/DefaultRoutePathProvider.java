/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.router.internal.RouteUtil;

/**
 * Default implementation for {@link RoutePathProvider}.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(service = RoutePathProvider.class, property = Constants.SERVICE_RANKING
        + ":Integer=" + Integer.MIN_VALUE)
public class DefaultRoutePathProvider implements RoutePathProvider {

    @Override
    public String getRoutePath(Class<?> navigationTarget) {
        Route route = navigationTarget.getAnnotation(Route.class);
        if (route == null) {
            return null;
        }
        return RouteUtil.resolve(navigationTarget, route);
    }

}
