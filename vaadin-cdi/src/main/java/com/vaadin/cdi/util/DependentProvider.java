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

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.inject.Provider;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A {@link Provider} for &#064;Dependent scoped contextual instances. We need
 * this to be able to properly clean them up when they are not needed anymore
 * via the {@link #destroy()} method.
 *
 * Instances of this class can be retrieved using the {@link BeanProvider}.
 *
 * Instances of this class are Serializable if the wrapped contextual instance
 * is Serializable.
 *
 * @see BeanProvider#getDependent(java.lang.Class,
 *      java.lang.annotation.Annotation...)
 */
public class DependentProvider<T> implements Provider<T>, Serializable {
    private static final long serialVersionUID = 23423413412001L;

    private T instance;
    private CreationalContext<T> creationalContext;
    private transient Bean<T> bean;

    DependentProvider(Bean<T> bean, CreationalContext<T> creationalContext,
            T instance) {
        this.bean = bean;
        this.creationalContext = creationalContext;
        this.instance = instance;
    }

    @Override
    public T get() {
        return instance;
    }

    /**
     * This method will properly destroy the &#064;Dependent scoped instance. It
     * will have no effect if the bean is NormalScoped as those have their own
     * lifecycle which we must not disrupt.
     */
    public void destroy() {
        if (!BeanManagerProvider.getInstance().getBeanManager()
                .isNormalScope(bean.getScope())) {
            bean.destroy(instance, creationalContext);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (!(bean instanceof PassivationCapable)) {
            throw new NotSerializableException(
                    "Bean is not PassivationCapable: " + bean.toString());
        }
        String passivationId = ((PassivationCapable) bean).getId();
        if (passivationId == null) {
            throw new NotSerializableException(bean.toString());
        }

        out.writeLong(serialVersionUID);
        out.writeObject(passivationId);
        out.writeObject(instance);
        out.writeObject(creationalContext);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        long oldSerialId = in.readLong();
        if (oldSerialId != serialVersionUID) {
            throw new NotSerializableException(
                    getClass().getName() + " serialVersion does not match");
        }
        String passivationId = (String) in.readObject();
        bean = (Bean<T>) BeanManagerProvider.getInstance().getBeanManager()
                .getPassivationCapableBean(passivationId);
        instance = (T) in.readObject();
        creationalContext = (CreationalContext<T>) in.readObject();
    }

}
