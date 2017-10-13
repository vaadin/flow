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
package com.vaadin.spring.scopes;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinSessionState;

/**
 * Implementation of Spring's
 * {@link org.springframework.beans.factory.config.Scope} that binds the beans
 * to the current {@link com.vaadin.server.VaadinSession} (as opposed to the
 * current Servlet session). Registered by default as the scope "
 * {@value #VAADIN_SESSION_SCOPE_NAME}".
 *
 * @see com.vaadin.spring.annotation.VaadinSessionScope
 *
 * @author Vaadin Ltd
 *
 */
public class VaadinSessionScope implements Scope, BeanFactoryPostProcessor {

    public static final String VAADIN_SESSION_SCOPE_NAME = "vaadin-session";

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerScope(VAADIN_SESSION_SCOPE_NAME, this);
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        return getBeanStore().get(name, objectFactory);
    }

    @Override
    public Object remove(String name) {
        return getBeanStore().remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        getBeanStore().registerDestructionCallback(name, callback);
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return getVaadinSession().getSession().getId();
    }

    private VaadinSession getVaadinSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            throw new IllegalStateException(
                    "No VaadinSession bound to current thread");
        }
        if (session.getState() != VaadinSessionState.OPEN) {
            throw new IllegalStateException(
                    "Current VaadinSession is not open");
        }
        return session;
    }

    private BeanStore getBeanStore() {
        final VaadinSession session = getVaadinSession();
        session.lock();
        try {
            BeanStore beanStore = session.getAttribute(BeanStore.class);
            if (beanStore == null) {
                beanStore = new BeanStore(session);
                session.setAttribute(BeanStore.class, beanStore);
            }
            return beanStore;
        } finally {
            session.unlock();
        }
    }

}
