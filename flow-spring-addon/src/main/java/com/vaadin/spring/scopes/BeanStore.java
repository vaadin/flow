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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.server.VaadinSession;
import com.vaadin.shared.Registration;

/**
 * @author Vaadin Ltd
 *
 */
class BeanStore {

    private static final Logger LOGGER = Logger
            .getLogger(BeanStore.class.getName());

    private final VaadinSession session;

    private final Registration sessionDestroyListenerRegistration;

    private final Map<String, Object> objects = new HashMap<String, Object>();

    private final Map<String, Runnable> destructionCallbacks = new HashMap<String, Runnable>();

    BeanStore(VaadinSession session) {
        assert session.hasLock();
        this.session = session;

        sessionDestroyListenerRegistration = session.getService()
                .addSessionDestroyListener(event -> destroy());
    }

    Object get(String name, ObjectFactory<?> objectFactory) {
        return execute(() -> doGet(name, objectFactory));
    }

    Object remove(String name) {
        return execute(() -> doRemove(name));
    }

    void registerDestructionCallback(String name, Runnable callback) {
        execute(() -> destructionCallbacks.put(name, callback));
    }

    private void destroy() {
        execute(this::doDestroy);
    }

    private Object doRemove(String name) {
        destructionCallbacks.remove(name);
        return objects.remove(name);
    }

    private Object doGet(String name, ObjectFactory<?> objectFactory) {
        Object bean = objects.get(name);
        if (bean == null) {
            bean = objectFactory.getObject();
            objects.put(name, bean);
        }
        return bean;
    }

    private Void doDestroy() {
        assert session.hasLock();
        try {
            session.setAttribute(BeanStore.class, null);
            sessionDestroyListenerRegistration.remove();
        } finally {
            for (Runnable destructionCallback : destructionCallbacks.values()) {
                try {
                    destructionCallback.run();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE,
                            "BeanStore destruction callback failed", e);
                }
            }
            destructionCallbacks.clear();
            objects.clear();
        }
        return null;
    }

    private <T> T execute(Supplier<T> supplier) {
        if (session.hasLock()) {
            return supplier.get();
        } else {
            session.lock();
            try {
                return supplier.get();
            } finally {
                session.unlock();
            }
        }
    }

}
