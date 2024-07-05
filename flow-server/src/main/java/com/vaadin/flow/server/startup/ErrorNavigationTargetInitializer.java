/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * Servlet initializer for collecting all available error handler navigation
 * targets implementing {@link HasErrorParameter} on startup.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
@HandlesTypes(HasErrorParameter.class)
public class ErrorNavigationTargetInitializer
        implements ClassLoaderAwareServletContainerInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void process(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        if (classSet == null) {
            classSet = new HashSet<>();
        }
        Set<Class<? extends Component>> routes = classSet.stream()
                // Liberty 18 also includes the interface itself in the set...
                .filter(clazz -> clazz != HasErrorParameter.class)
                .map(clazz -> (Class<? extends Component>) clazz)
                .collect(Collectors.toSet());

        ApplicationRouteRegistry
                .getInstance(new VaadinServletContext(servletContext))
                .setErrorNavigationTargets(routes);
    }

}
