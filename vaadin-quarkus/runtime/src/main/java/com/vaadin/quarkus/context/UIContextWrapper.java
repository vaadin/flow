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
import io.quarkus.arc.InjectableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import com.vaadin.quarkus.annotation.NormalUIScoped;
import com.vaadin.quarkus.annotation.UIScoped;

/**
 * Used to bind multiple scope annotations to a single context. Will delegate
 * all context-related operations to it's underlying instance, apart from
 * getting the scope of the context.
 */
public class UIContextWrapper implements InjectableContext {

    @Override
    public Class<? extends Annotation> getScope() {
        return UIScoped.class;
    }

    @Override
    public <T> T get(final Contextual<T> component,
            final CreationalContext<T> creationalContext) {
        return getContext().get(component, creationalContext);
    }

    @Override
    public <T> T get(final Contextual<T> component) {
        return getContext().get(component);
    }

    @Override
    public boolean isActive() {
        return getContext().isActive();
    }

    @Override
    public void destroy(final Contextual<?> contextual) {
        getContext().destroy(contextual);
    }

    @Override
    public void destroy() {
        getContext().destroy();
    }

    @Override
    public ContextState getState() {
        return getContext().getState();
    }

    /**
     * Gets a delegating context.
     * <p>
     * Not a private for testing purposes only.
     *
     * @return a delegating context
     */
    InjectableContext getContext() {
        return Arc.container().getActiveContext(NormalUIScoped.class);
    }
}
