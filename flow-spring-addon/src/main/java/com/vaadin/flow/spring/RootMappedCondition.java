/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.servlet.DispatcherServlet;

import com.vaadin.flow.server.VaadinServlet;

/**
 * Condition to check whether the Vaadin servlet is mapped to the root
 * ({@literal "/*"}).
 * <p>
 * In this case a {@link DispatcherServlet} is used. It's mapped to the root
 * instead of VaadinServlet and forwards requests to {@link VaadinServlet}. If
 * there are other mappings (via Spring endpoints e.g.) then
 * {@link DispatcherServlet} makes it possible to handle them properly via those
 * endpoints. Otherwise {@link VaadinServlet} will handle all the URLs because
 * it has the highest priority.
 *
 * @author Vaadin Ltd
 *
 */
public class RootMappedCondition implements Condition {

    public static final String URL_MAPPING_PROPERTY = "vaadin.urlMapping";

    @Override
    public boolean matches(ConditionContext context,
            AnnotatedTypeMetadata metadata) {
        return isRootMapping(
                context.getEnvironment().getProperty(URL_MAPPING_PROPERTY));
    }

    /**
     * Returns {@code true} if {@code mapping} is the root mapping
     * ({@literal "/*"}).
     * <p>
     * The mapping is controlled via the {@code vaadin.urlMapping} property
     * value. By default it's {@literal "/*"}.
     *
     * @param mapping
     *            the mapping string to check
     * @return {@code true} if {@code mapping} is the root mapping and
     *         {@code false} otherwise
     */
    public static boolean isRootMapping(String mapping) {
        if (mapping == null) {
            return true;
        }
        return mapping.trim().replaceAll("(/\\**)?$", "").isEmpty();
    }
}
