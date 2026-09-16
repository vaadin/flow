/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.cdi.util;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.PassivationCapable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A skeleton containing the most important parts of a custom CDI Context. An
 * implementing Context needs to implement the missing methods from the
 * {@link Context} interface and
 * {@link #getContextualStorage(Contextual, boolean)}.
 */
public abstract class AbstractContext implements Context {
    /**
     * Whether the Context is for a passivating scope.
     */
    private final boolean passivatingScope;

    protected AbstractContext(BeanManager beanManager) {
        passivatingScope = beanManager.isPassivatingScope(getScope());
    }

    /**
     * An implementation has to return the underlying storage which contains the
     * items held in the Context.
     * 
     * @param createIfNotExist
     *            whether a ContextualStorage shall get created if it doesn't
     *            yet exist.
     * @return the underlying storage
     */
    protected abstract ContextualStorage getContextualStorage(
            Contextual<?> contextual, boolean createIfNotExist);

    protected List<ContextualStorage> getActiveContextualStorages() {
        List<ContextualStorage> result = new ArrayList<ContextualStorage>();
        result.add(getContextualStorage(null, false));
        return result;
    }

    /**
     * @return whether the served scope is a passivating scope
     */
    public boolean isPassivatingScope() {
        return passivatingScope;
    }

    @Override
    public <T> T get(Contextual<T> bean) {
        checkActive();

        ContextualStorage storage = getContextualStorage(bean, false);
        if (storage == null) {
            return null;
        }

        Map<Object, ContextualInstanceInfo<?>> contextMap = storage
                .getStorage();
        ContextualInstanceInfo<?> contextualInstanceInfo = contextMap
                .get(storage.getBeanKey(bean));
        if (contextualInstanceInfo == null) {
            return null;
        }

        return (T) contextualInstanceInfo.getContextualInstance();
    }

    @Override
    public <T> T get(Contextual<T> bean,
            CreationalContext<T> creationalContext) {
        if (creationalContext == null) {
            return get(bean);
        }

        checkActive();

        if (passivatingScope) {
            if (!(bean instanceof PassivationCapable)) {
                throw new IllegalStateException(
                        bean.toString() + " doesn't implement "
                                + PassivationCapable.class.getName());
            }
        }

        ContextualStorage storage = getContextualStorage(bean, true);

        Map<Object, ContextualInstanceInfo<?>> contextMap = storage
                .getStorage();
        ContextualInstanceInfo<?> contextualInstanceInfo = contextMap
                .get(storage.getBeanKey(bean));

        if (contextualInstanceInfo != null) {
            @SuppressWarnings("unchecked")
            final T instance = (T) contextualInstanceInfo
                    .getContextualInstance();

            if (instance != null) {
                return instance;
            }
        }

        return storage.createContextualInstance(bean, creationalContext);
    }

    /**
     * Destroy the Contextual Instance of the given Bean.
     * 
     * @param bean
     *            dictates which bean shall get cleaned up
     * @return <code>true</code> if the bean was destroyed, <code>false</code>
     *         if there was no such bean.
     */
    public boolean destroy(Contextual bean) {
        ContextualStorage storage = getContextualStorage(bean, false);
        if (storage == null) {
            return false;
        }

        ContextualInstanceInfo<?> contextualInstanceInfo = storage.getStorage()
                .remove(storage.getBeanKey(bean));

        if (contextualInstanceInfo == null) {
            return false;
        }

        destroyBean(bean, contextualInstanceInfo);

        return true;
    }

    /**
     * destroys all the Contextual Instances in the Storage returned by
     * {@link #getContextualStorage(Contextual, boolean)}.
     */
    public void destroyAllActive() {
        List<ContextualStorage> storages = getActiveContextualStorages();
        if (storages == null) {
            return;
        }

        for (ContextualStorage storage : storages) {
            if (storage != null) {
                destroyAllActive(storage);
            }
        }
    }

    /**
     * Destroys all the Contextual Instances in the specified ContextualStorage.
     * This is a static method to allow various holder objects to cleanup
     * properly in &#064;PreDestroy.
     */
    public static Map<Object, ContextualInstanceInfo<?>> destroyAllActive(
            ContextualStorage storage) {
        // drop all entries in the storage before starting with destroying the
        // original entries
        Map<Object, ContextualInstanceInfo<?>> contextMap = new HashMap<Object, ContextualInstanceInfo<?>>(
                storage.getStorage());
        storage.getStorage().clear();

        for (Map.Entry<Object, ContextualInstanceInfo<?>> entry : contextMap
                .entrySet()) {
            Contextual bean = storage.getBean(entry.getKey());

            ContextualInstanceInfo<?> contextualInstanceInfo = entry.getValue();
            destroyBean(bean, contextualInstanceInfo);
        }
        return contextMap;
    }

    public static void destroyBean(Contextual bean,
            ContextualInstanceInfo<?> contextualInstanceInfo) {
        bean.destroy(contextualInstanceInfo.getContextualInstance(),
                contextualInstanceInfo.getCreationalContext());
    }

    /**
     * Make sure that the Context is really active.
     * 
     * @throws ContextNotActiveException
     *             if there is no active Context for the current Thread.
     */
    protected void checkActive() {
        if (!isActive()) {
            throw new ContextNotActiveException(
                    "CDI context with scope annotation @" + getScope().getName()
                            + " is not active with respect to the current thread");
        }
    }

}
