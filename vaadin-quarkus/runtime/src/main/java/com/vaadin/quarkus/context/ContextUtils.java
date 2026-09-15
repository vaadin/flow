/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus.context;

import java.lang.annotation.Annotation;

import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.BeanManager;

/**
 * A modified copy of org.apache.deltaspike.core.util.ContextUtils.
 *
 * A set of utility methods for working with contexts.
 */
@Typed()
public abstract class ContextUtils {
    private ContextUtils() {
        // prevent instantiation
    }

    /**
     * Checks if the context for the given scope annotation is active.
     *
     * @param scopeAnnotationClass
     *            The scope annotation (e.g. @RequestScoped.class)
     * @return If the context is active.
     */
    public static boolean isContextActive(
            Class<? extends Annotation> scopeAnnotationClass) {
        return isContextActive(scopeAnnotationClass,
                Arc.container().beanManager());
    }

    /**
     * Checks if the context for the given scope annotation is active.
     *
     * @param scopeAnnotationClass
     *            The scope annotation (e.g. @RequestScoped.class)
     * @param beanManager
     *            The {@link BeanManager}
     * @return If the context is active.
     */
    public static boolean isContextActive(
            Class<? extends Annotation> scopeAnnotationClass,
            BeanManager beanManager) {
        try {
            if (beanManager.getContext(scopeAnnotationClass) == null
                    || !beanManager.getContext(scopeAnnotationClass)
                            .isActive()) {
                return false;
            }
        } catch (ContextNotActiveException e) {
            return false;
        }

        return true;
    }
}
