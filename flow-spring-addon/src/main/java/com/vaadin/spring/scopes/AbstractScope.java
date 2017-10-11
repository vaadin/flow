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

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinSessionState;

/**
 * @author Vaadin Ltd
 *
 */
abstract class AbstractScope implements Scope, BeanFactoryPostProcessor {

    @Override
    public Object resolveContextualObject(String key) {
        return null;
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

    protected abstract BeanStore getBeanStore();

    protected VaadinSession getVaadinSession() {
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
}
