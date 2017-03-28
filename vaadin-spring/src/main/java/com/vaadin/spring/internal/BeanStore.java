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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

/**
 * Class for storing beans in the different Vaadin scopes. For internal use
 * only.
 *
 * @author Petter Holmström (petter@vaadin.com)
 */
public class BeanStore implements Serializable {

    private static final long serialVersionUID = 7625347916717427098L;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BeanStore.class);

    private final Map<String, Object> objectMap = new ConcurrentHashMap<String, Object>();

    private final Map<String, Runnable> destructionCallbacks = new ConcurrentHashMap<String, Runnable>();

    private final String name;

    private final DestructionCallback destructionCallback;

    private boolean destroyed = false;

    public BeanStore(String name, DestructionCallback destructionCallback) {
        this.name = name;
        this.destructionCallback = destructionCallback;
    }

    public BeanStore(String name) {
        this(name, null);
    }

    public Object get(String s, ObjectFactory<?> objectFactory) {
        LOGGER.trace("Getting bean with name [{}] from [{}]", s, this);
        Object bean = objectMap.get(s);
        if (bean == null) {
            bean = create(s, objectFactory);
            LOGGER.trace("Added bean [{}] with name [{}] to [{}]", bean, s,
                    this);
            objectMap.put(s, bean);
        }
        return bean;
    }

    protected Object create(String s, ObjectFactory<?> objectFactory) {
        final Object bean = objectFactory.getObject();
        if (!(bean instanceof Serializable)) {
            LOGGER.warn(
                    "Storing non-serializable bean [{}] with name [{}] in [{}]",
                    bean, s, this);
        }
        return bean;
    }

    public Object remove(String s) {
        destructionCallbacks.remove(s);
        return objectMap.remove(s);
    }

    public void registerDestructionCallback(String s, Runnable runnable) {
        LOGGER.trace(
                "Registering destruction callback for bean with name [{}] in [{}]",
                s, this);
        destructionCallbacks.put(s, runnable);
    }

    public void destroy() {
        if (destroyed) {
            LOGGER.trace("[{}] has already been destroyed, ignoring", this);
            return;
        }
        try {
            LOGGER.debug("Destroying [{}]", this);
            for (Runnable destructionCallback : destructionCallbacks.values()) {
                try {
                    destructionCallback.run();
                } catch (Exception e) {
                    LOGGER.error("BeanStore destruction callback failed", e);
                }
            }
            destructionCallbacks.clear();
            objectMap.clear();
            if (destructionCallback != null) {
                try {
                    destructionCallback.beanStoreDestroyed(this);
                } catch (Exception e) {
                    LOGGER.error("BeanStore final destruction callback failed",
                            e);
                }
            }
        } finally {
            destroyed = true;
        }
    }

    @Override
    public String toString() {
        return String.format("%s[id=%x, name=%s]", getClass().getSimpleName(),
                System.identityHashCode(this), name);
    }

    /**
     * Callback interface for receiving notifications about a
     * {@link com.vaadin.spring.internal.BeanStore} being destroyed.
     */
    public static interface DestructionCallback extends Serializable {

        void beanStoreDestroyed(BeanStore beanStore);

    }
}
