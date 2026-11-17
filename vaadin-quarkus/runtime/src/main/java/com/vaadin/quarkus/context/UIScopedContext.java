/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.BeanManager;

import java.lang.annotation.Annotation;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.quarkus.annotation.NormalUIScoped;
import com.vaadin.quarkus.annotation.VaadinSessionScoped;

/**
 * UIScopedContext is the context for {@link NormalUIScoped @NormalUIScoped}
 * beans.
 * 
 * @since 1.0
 */
public class UIScopedContext extends AbstractContext {

    @Override
    protected ContextualStorage getContextualStorage(Contextual<?> contextual,
            boolean createIfNotExist) {
        return BeanProvider
                .getContextualReference(getBeanManager(),
                        ContextualStorageManager.class, false)
                .getContextualStorage(createIfNotExist);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return NormalUIScoped.class;
    }

    @Override
    public boolean isActive() {
        return VaadinSession.getCurrent() != null && UI.getCurrent() != null;
    }

    /**
     * Gets a bean manager.
     * <p>
     * Not a private for testing purposes only.
     *
     * @return a bean manager
     */
    BeanManager getBeanManager() {
        return Arc.container().beanManager();
    }

    /**
     * Manages the contextual storages of UI scoped beans, one for each UI.
     */
    @VaadinSessionScoped
    @Unremovable
    public static class ContextualStorageManager
            extends AbstractContextualStorageManager<Integer> {

        /**
         * Creates a storage manager that relies on the session lock instead of
         * locking on its own.
         */
        public ContextualStorageManager() {
            // Session lock checked in VaadinSessionScopedContext while
            // getting the session attribute of this beans context.
            super(false);
        }

        /**
         * Gets the contextual storage of the current UI.
         *
         * @param createIfNotExist
         *            whether to create the storage if it does not exist yet
         * @return the storage, or {@code null} if it does not exist and
         *         {@code createIfNotExist} is {@code false}
         */
        public ContextualStorage getContextualStorage(
                boolean createIfNotExist) {
            final Integer uiId = UI.getCurrentOrThrow().getUIId();
            return super.getContextualStorage(uiId, createIfNotExist);
        }

        @Override
        protected ContextualStorage newContextualStorage(Integer uiId) {
            UI.getCurrentOrThrow().addDetachListener(this::destroy);
            return super.newContextualStorage(uiId);
        }

        private void destroy(DetachEvent event) {
            final int uiId = event.getUI().getUIId();
            super.destroy(uiId);
        }
    }

}
