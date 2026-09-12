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

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.BeanManager;

import java.lang.annotation.Annotation;

import com.vaadin.cdi.annotation.UIScoped;
import com.vaadin.cdi.annotation.VaadinSessionScoped;
import com.vaadin.cdi.util.AbstractContext;
import com.vaadin.cdi.util.BeanProvider;
import com.vaadin.cdi.util.ContextualStorage;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * UIScopedContext is the context for {@link UIScoped @UIScoped} beans.
 */
public class UIScopedContext extends AbstractContext {

    private ContextualStorageManager contextualStorageManager;

    public UIScopedContext(final BeanManager beanManager) {
        super(beanManager);
    }

    @Override
    protected ContextualStorage getContextualStorage(Contextual<?> contextual,
            boolean createIfNotExist) {
        return contextualStorageManager.getContextualStorage(createIfNotExist);
    }

    public void init(BeanManager beanManager) {
        contextualStorageManager = BeanProvider.getContextualReference(
                beanManager, ContextualStorageManager.class, false);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return UIScoped.class;
    }

    @Override
    public boolean isActive() {
        return VaadinSession.getCurrent() != null && UI.getCurrent() != null
                && contextualStorageManager != null;
    }

    @VaadinSessionScoped
    public static class ContextualStorageManager
            extends AbstractContextualStorageManager<Integer> {

        public ContextualStorageManager() {
            // Session lock checked in VaadinSessionScopedContext while
            // getting the session attribute of this beans context.
            super(false);
        }

        public ContextualStorage getContextualStorage(
                boolean createIfNotExist) {
            final Integer uiId = UI.getCurrent().getUIId();
            return super.getContextualStorage(uiId, createIfNotExist);
        }

        @Override
        protected ContextualStorage newContextualStorage(Integer uiId) {
            UI.getCurrent().addDetachListener(this::destroy);
            return super.newContextualStorage(uiId);
        }

        private void destroy(DetachEvent event) {
            final int uiId = event.getUI().getUIId();
            super.destroy(uiId);
        }
    }
}
