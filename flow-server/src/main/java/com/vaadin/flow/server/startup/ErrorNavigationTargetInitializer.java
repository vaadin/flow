/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import javax.servlet.annotation.HandlesTypes;

import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.VaadinContext;

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
        implements VaadinServletContextStartupInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Set<Class<?>> classSet, VaadinContext context) {
        classSet = AbstractAnnotationValidator
                .removeHandleTypesSelfReferences(classSet, this);
        Set<Class<? extends Component>> routes = classSet.stream()
                .map(clazz -> (Class<? extends Component>) clazz)
                .collect(Collectors.toSet());

        ApplicationRouteRegistry.getInstance(context)
                .setErrorNavigationTargets(routes);
    }

}
