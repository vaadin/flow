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
package com.vaadin.flow.spring.scopes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.flow.server.VaadinSession;

/**
 * Spring bean store class to keep scope objects.
 *
 * @author Vaadin Ltd
 *
 */
class BeanStore {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BeanStore.class.getName());

    private final VaadinSession session;

    private final Map<String, Object> objects = new HashMap<>();

    private final Map<String, Runnable> destructionCallbacks = new HashMap<>();

    /**
     * Creates a new instance for the given {@code session}.
     *
     * @param session
     *            a vaadin session
     */
    BeanStore(VaadinSession session) {
        session.checkHasLock();
        this.session = session;
    }

    /**
     * Return the object with the given name from the underlying scop.,
     *
     * @param name
     *            the name of the object to retrieve
     * @param objectFactory
     *            the {@link ObjectFactory} to use to create the scoped object
     *            if it is not present in the underlying storage mechanism
     * @return the desired object (never {@code null})
     *
     * @see Scope#get(String, ObjectFactory)
     */
    Object get(String name, ObjectFactory<?> objectFactory) {
        return execute(() -> doGet(name, objectFactory));
    }

    /**
     * Remove the object with the given {@code name} from the underlying scope.
     *
     * @param name
     *            the name of the object to remove
     * @see Scope#remove(String)
     */
    Object remove(String name) {
        return execute(() -> doRemove(name));
    }

    /**
     * Register a callback to be executed on destruction of the specified object
     * in the scope (or at destruction of the entire scope, if the scope does
     * not destroy individual objects but rather only terminates in its
     * entirety).
     *
     * @see Scope#registerDestructionCallback(String, Runnable)
     */
    void registerDestructionCallback(String name, Runnable callback) {
        execute(() -> destructionCallbacks.put(name, callback));
    }

    void destroy() {
        execute(this::doDestroy);
    }

    VaadinSession getVaadinSession() {
        return session;
    }

    Void doDestroy() {
        session.checkHasLock();
        for (Runnable destructionCallback : destructionCallbacks.values()) {
            try {
                destructionCallback.run();
            } catch (Exception e) {
                LOGGER.error(
                        "BeanStore destruction callback failed", e);
            }
        }
        destructionCallbacks.clear();
        objects.clear();
        return null;
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
