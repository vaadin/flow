/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.server.ServiceDestroyEvent;
import com.vaadin.server.ServiceDestroyListener;
import com.vaadin.server.VaadinSession;

/**
 * Implementation of Spring's
 * {@link org.springframework.beans.factory.config.Scope} that binds the beans
 * to the current {@link com.vaadin.server.VaadinSession} (as opposed to the
 * current Servlet session). Registered by default as the scope "
 * {@value #VAADIN_SESSION_SCOPE_NAME}".
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @see com.vaadin.spring.annotation.VaadinSessionScope
 */
public class VaadinSessionScope implements Scope, BeanFactoryPostProcessor {

    public static final String VAADIN_SESSION_SCOPE_NAME = "vaadin-session";
    private static final Logger LOGGER = LoggerFactory
            .getLogger(VaadinSessionScope.class);

    private static BeanStoreRetrievalStrategy beanStoreRetrievalStrategy = new VaadinSessionBeanStoreRetrievalStrategy();

    /**
     * Sets the {@link BeanStoreRetrievalStrategy} to use.
     */
    public static synchronized void setBeanStoreRetrievalStrategy(
            BeanStoreRetrievalStrategy beanStoreRetrievalStrategy) {
        if (beanStoreRetrievalStrategy == null) {
            beanStoreRetrievalStrategy = new VaadinSessionBeanStoreRetrievalStrategy();
        }
        VaadinSessionScope.beanStoreRetrievalStrategy = beanStoreRetrievalStrategy;
    }

    /**
     * Returns the {@link BeanStoreRetrievalStrategy} to use. By default,
     * {@link com.vaadin.spring.internal.VaadinSessionScope.VaadinSessionBeanStoreRetrievalStrategy}
     * is used.
     */
    public static synchronized BeanStoreRetrievalStrategy getBeanStoreRetrievalStrategy() {
        return beanStoreRetrievalStrategy;
    }

    @Override
    public Object get(String s, ObjectFactory<?> objectFactory) {
        return getBeanStore().get(s, objectFactory);
    }

    @Override
    public Object remove(String s) {
        return getBeanStore().remove(s);
    }

    @Override
    public void registerDestructionCallback(String s, Runnable runnable) {
        getBeanStore().registerDestructionCallback(s, runnable);
    }

    @Override
    public Object resolveContextualObject(String s) {
        return null;
    }

    @Override
    public String getConversationId() {
        return getBeanStoreRetrievalStrategy().getConversationId();
    }

    private BeanStore getBeanStore() {
        return getBeanStoreRetrievalStrategy().getBeanStore();
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory configurableListableBeanFactory)
                    throws BeansException {
        LOGGER.debug("Registering VaadinSession scope with bean factory [{}]",
                configurableListableBeanFactory);
        configurableListableBeanFactory.registerScope(
                VAADIN_SESSION_SCOPE_NAME, this);
    }

    /**
     * Cleans up everything associated with the scope of a specific session.
     *
     * @param session
     *            the Vaadin session for which to do the clean up, not
     *            <code>null</code>
     */
    public static void cleanupSession(VaadinSession session) {
        assert session != null;

        BeanStore beanStore = session.getAttribute(BeanStore.class);
        if (beanStore != null) {
            LOGGER.debug("Vaadin session has been destroyed, destroying [{}]",
                    beanStore);

            beanStore.destroy();
        }
    }

    /**
     * Implementation of {@link BeanStoreRetrievalStrategy} that stores the
     * {@link BeanStore} in the current {@link com.vaadin.server.VaadinSession}.
     */
    public static class VaadinSessionBeanStoreRetrievalStrategy implements
    BeanStoreRetrievalStrategy {

        private VaadinSession getVaadinSession() {
            VaadinSession current = VaadinSession.getCurrent();
            if (current == null) {
                throw new IllegalStateException(
                        "No VaadinSession bound to current thread");
            }
            if (current.getState() != VaadinSession.State.OPEN) {
                throw new IllegalStateException(
                        "Current VaadinSession is not open");
            }
            return current;
        }

        @Override
        public BeanStore getBeanStore() {
            final VaadinSession session = getVaadinSession();
            session.lock();
            try {
                BeanStore beanStore = session.getAttribute(BeanStore.class);
                if (beanStore == null) {
                    beanStore = new SessionAwareBeanStore(session);
                }
                return beanStore;
            } finally {
                session.unlock();
            }
        }

        @Override
        public String getConversationId() {
            return getVaadinSession().getSession().getId();
        }
    }

    static class SessionAwareBeanStore extends SessionLockingBeanStore
            implements ServiceDestroyListener {

        private static final long serialVersionUID = -8170074251720975071L;
        private static final Logger LOGGER = LoggerFactory
                .getLogger(SessionAwareBeanStore.class);

        SessionAwareBeanStore(VaadinSession session) {
            super(session, "Session:" + session.getSession().getId(), null);
            session.getService().addServiceDestroyListener(this);
            session.setAttribute(BeanStore.class, this);
        }

        @Override
        public void destroy() {
            session.lock();
            try {
                try {
                    session.setAttribute(BeanStore.class, null);
                    session.getService().removeServiceDestroyListener(this);
                } finally {
                    super.destroy();
                }
            } finally {
                session.unlock();
            }
        }

        @Override
        public void serviceDestroy(ServiceDestroyEvent event) {
            LOGGER.debug("Vaadin service has been destroyed, destroying [{}]",
                    this);
            destroy();
        }
    }
}
