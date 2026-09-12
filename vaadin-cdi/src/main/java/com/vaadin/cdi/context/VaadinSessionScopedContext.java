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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import java.lang.annotation.Annotation;

import com.vaadin.cdi.annotation.VaadinSessionScoped;
import com.vaadin.cdi.util.AbstractContext;
import com.vaadin.cdi.util.BeanProvider;
import com.vaadin.cdi.util.ContextUtils;
import com.vaadin.cdi.util.ContextualStorage;
import com.vaadin.flow.server.VaadinSession;

/**
 * Context for {@link VaadinSessionScoped @VaadinSessionScoped} beans.
 * <p>
 * Stores contextuals in {@link VaadinSession}. Other Vaadin CDI contexts are
 * stored in the corresponding {@link VaadinSessionScoped} context.
 * <p>
 * The context is active when the current {@link VaadinSession} is bound to the
 * calling thread and that thread holds the session lock — i.e. when the code
 * runs inside an active Vaadin request, a {@code UI#access} block, or a
 * {@code VaadinSession#access} block. Code running on a background thread that
 * has only set {@link VaadinSession#setCurrent(VaadinSession)} without
 * acquiring the session lock is operating outside the supported usage of the
 * framework; behavior in that case is not guaranteed.
 *
 * @since 3.0
 */
public class VaadinSessionScopedContext extends AbstractContext {

    private ContextualStorageManager contextManager;

    public VaadinSessionScopedContext(BeanManager beanManager) {
        super(beanManager);
    }

    public void init(BeanManager beanManager) {
        contextManager = BeanProvider.getContextualReference(beanManager,
                ContextualStorageManager.class, false);
    }

    @Override
    protected ContextualStorage getContextualStorage(Contextual<?> contextual,
            boolean createIfNotExist) {
        return contextManager.getContextualStorage(contextual,
                createIfNotExist);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return VaadinSessionScoped.class;
    }

    @Override
    public boolean isActive() {
        return contextManager != null && contextManager.isActive();
    }

    public static void destroy(VaadinSession session) {
        ContextualStorageManager.destroy(session);
    }

    /**
     * Guess whether this context is undeployed.
     *
     * Tomcat expires sessions after contexts are undeployed. Need this guess to
     * prevent exceptions when try to properly destroy contexts on session
     * expiration.
     *
     * @return true when context is not active, but sure it should
     */
    public static boolean guessContextIsUndeployed() {
        // Given there is a current VaadinSession, we should have an active
        // context,
        // except we get here after the application is undeployed.
        return (VaadinSession.getCurrent() != null
                && !ContextUtils.isContextActive(VaadinSessionScoped.class));
    }

    /**
     * For internal use only.
     */
    @ApplicationScoped
    public static class ContextualStorageManager {

        protected static final String ATTRIBUTE_NAME = VaadinSessionScopedContext.class
                .getName();

        @Inject
        private BeanManager beanManager;

        protected boolean isActive() {
            VaadinSession session = VaadinSession.getCurrent();
            return session != null && session.hasLock();
        }

        protected ContextualStorage getContextualStorage(
                Contextual<?> contextual, boolean createIfNotExist) {
            VaadinSession session = VaadinSession.getCurrent();
            ContextualStorage storage = findContextualStorage(session);
            if (storage == null && createIfNotExist) {
                storage = new ContextualStorage(beanManager, false, true);
                session.setAttribute(ATTRIBUTE_NAME, storage);
            }
            return storage;
        }

        private static ContextualStorage findContextualStorage(
                VaadinSession session) {
            // session lock is checked inside
            return (ContextualStorage) session.getAttribute(ATTRIBUTE_NAME);
        }

        private static void destroy(VaadinSession session) {
            ContextualStorage storage = findContextualStorage(session);
            if (storage != null) {
                AbstractContext.destroyAllActive(storage);
            }
        }

    }

}
