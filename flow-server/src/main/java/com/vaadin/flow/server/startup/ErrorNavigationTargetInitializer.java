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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContainerInitializer;
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
 *
 * @since 1.0
 */
@HandlesTypes(HasErrorParameter.class)
public class ErrorNavigationTargetInitializer
        implements ServletContainerInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
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
