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
