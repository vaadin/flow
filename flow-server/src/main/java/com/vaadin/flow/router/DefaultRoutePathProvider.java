/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

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
        return getRoutePath(route.value(), navigationTarget);

    }

    public static String getRoutePath(String routeValue, Class<?> routeClass) {

        if (routeValue.equals(Route.NAMING_CONVENTION)) {
            String simpleName = routeClass.getSimpleName();
            if ("MainView".equals(simpleName) || "Main".equals(simpleName)) {
                return "";
            }
            if (simpleName.endsWith("View")) {
                return simpleName
                        .substring(0, simpleName.length() - "View".length())
                        .toLowerCase();
            }
            return simpleName.toLowerCase();
        }
        return routeValue;

    }

}
