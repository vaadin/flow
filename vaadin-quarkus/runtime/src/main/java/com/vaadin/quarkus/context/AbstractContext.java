/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

/**
 * A modified copy of org.apache.deltaspike.core.util.context.AbstractContext.
 *
 * A skeleton containing the most important parts of a custom CDI Context. An
 * implementing Context needs to implement the missing methods from the
 * {@link AlterableContext} interface and
 * {@link #getContextualStorage(Contextual, boolean)}.
 */
public abstract class AbstractContext implements InjectableContext {

    /**
     * An implementation has to return the underlying storage which contains the
     * items held in the Context.
     *
     * @param contextual
     *            the contextual type
     * @param createIfNotExist
     *            whether a ContextualStorage shall get created if it doesn't
     *            yet exist.
     * @return the underlying storage
     */
    protected abstract ContextualStorage getContextualStorage(
            Contextual<?> contextual, boolean createIfNotExist);

    /**
     * Gets all active contextual storages.
     *
     * @return a list of contextual storages.
     */
    protected List<ContextualStorage> getActiveContextualStorages() {
        List<ContextualStorage> result = new ArrayList<ContextualStorage>();
        result.add(getContextualStorage(null, false));
        return result;
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
     */
    @Override
    public void destroy(Contextual<?> bean) {
        ContextualStorage storage = getContextualStorage(bean, false);
        if (storage == null) {
            return;
        }

        ContextualInstanceInfo<?> contextualInstanceInfo = storage.getStorage()
                .remove(storage.getBeanKey(bean));

        if (contextualInstanceInfo == null) {
            return;
        }

        destroyBean(bean, contextualInstanceInfo);
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
     *
     * @param storage
     *            a contextual storage
     * @return a storage map of destroyed objects
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
            Contextual<?> bean = storage.getBean(entry.getKey());

            ContextualInstanceInfo<?> contextualInstanceInfo = entry.getValue();
            destroyBean(bean, contextualInstanceInfo);
        }
        return contextMap;
    }

    @Override
    public void destroy() {
        destroyAllActive();
    }

    @Override
    public ContextState getState() {
        return this::getContextualInstances;
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

    private Map<InjectableBean<?>, Object> getContextualInstances() {
        List<ContextualStorage> storages = getActiveContextualStorages();
        if (storages == null) {
            return Collections.emptyMap();
        }
        Map<InjectableBean<?>, Object> state = new HashMap<>();
        for (ContextualStorage storage : storages) {
            for (Map.Entry<Object, ContextualInstanceInfo<?>> entry : storage
                    .getStorage().entrySet()) {
                state.put((InjectableBean<?>) storage.getBean(entry.getKey()),
                        entry.getValue().getContextualInstance());
            }
        }
        return state;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void destroyBean(Contextual bean,
            ContextualInstanceInfo<?> contextualInstanceInfo) {
        bean.destroy(contextualInstanceInfo.getContextualInstance(),
                contextualInstanceInfo.getCreationalContext());
    }

}
