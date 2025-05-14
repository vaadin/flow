/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import jakarta.servlet.annotation.HandlesTypes;

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
