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

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.server.VaadinSession;

/**
 * A {@link BeanStore} implementation that locks the related
 * {@link VaadinSession} for all operations that may not be thread safe
 * otherwise.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 */
public class SessionLockingBeanStore extends BeanStore {

    private static final long serialVersionUID = -5244428440915664554L;

    protected final VaadinSession session;

    protected SessionLockingBeanStore(VaadinSession session, String name,
            DestructionCallback destructionCallback) {
        super(name, destructionCallback);
        this.session = session;
    }

    @Override
    public Object get(String s, ObjectFactory<?> objectFactory) {
        session.lock();
        try {
            return super.get(s, objectFactory);
        } finally {
            session.unlock();
        }
    }

    @Override
    public Object remove(String s) {
        session.lock();
        try {
            return super.remove(s);
        } finally {
            session.unlock();
        }
    }

    @Override
    public void destroy() {
        session.lock();
        try {
            super.destroy();
        } finally {
            session.unlock();
        }
    }

    @Override
    public void registerDestructionCallback(String s, Runnable runnable) {
        session.lock();
        try {
            super.registerDestructionCallback(s, runnable);
        } finally {
            session.unlock();
        }
    }
}
