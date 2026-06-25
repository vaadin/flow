/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.scopes;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.vaadin.flow.server.VaadinSession;

/**
 * Implementation of Spring's
 * {@link org.springframework.beans.factory.config.Scope} that binds the beans
 * to the current {@link com.vaadin.flow.server.VaadinSession} (as opposed to
 * the current Servlet session). Registered by default as the scope "
 * {@value #VAADIN_SESSION_SCOPE_NAME}".
 *
 * @see com.vaadin.flow.spring.annotation.VaadinSessionScope
 *
 * @author Vaadin Ltd
 *
 */
public class VaadinSessionScope extends AbstractScope {

    public static final String VAADIN_SESSION_SCOPE_NAME = "vaadin-session";

    private static class SessionBeanStore extends BeanStore {

        private SessionBeanStore(VaadinSession session) {
            super(session);
            session.addSessionDestroyListener(event -> destroy());
        }

        @Override
        Void doDestroy() {
            getVaadinSession().setAttribute(BeanStore.class, null);
            return super.doDestroy();
        }
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) {
        beanFactory.registerScope(VAADIN_SESSION_SCOPE_NAME, this);
        ObjectFactory<VaadinSession> factory = this::getVaadinSession;
        beanFactory.registerResolvableDependency(VaadinSession.class, factory);
    }

    @Override
    public String getConversationId() {
        return getVaadinSession().getSession().getId();
    }

    @Override
    protected BeanStore getBeanStore() {
        final VaadinSession session = getVaadinSession();
        session.getLockInstance().lock();
        try {
            BeanStore beanStore = session.getAttribute(BeanStore.class);
            if (beanStore == null) {
                beanStore = new SessionBeanStore(session);
                session.setAttribute(BeanStore.class, beanStore);
            }
            return beanStore;
        } finally {
            session.getLockInstance().unlock();
        }
    }

}
