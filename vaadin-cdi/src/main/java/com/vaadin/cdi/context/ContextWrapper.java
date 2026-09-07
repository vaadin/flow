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
package com.vaadin.cdi.context;

import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import java.lang.annotation.Annotation;

import com.vaadin.cdi.util.AbstractContext;

/**
 * Used to bind multiple scope annotations to a single context. Will delegate
 * all context-related operations to it's underlying instance, apart from
 * getting the scope of the context.
 *
 */
public class ContextWrapper implements AlterableContext {

    private final AbstractContext context;
    private final Class<? extends Annotation> scope;

    public ContextWrapper(AbstractContext context,
            Class<? extends Annotation> scope) {
        this.context = context;
        this.scope = scope;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    @Override
    public <T> T get(Contextual<T> component,
            CreationalContext<T> creationalContext) {
        return context.get(component, creationalContext);
    }

    @Override
    public <T> T get(Contextual<T> component) {
        return context.get(component);
    }

    @Override
    public boolean isActive() {
        return context.isActive();
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        context.destroy(contextual);
    }
}
