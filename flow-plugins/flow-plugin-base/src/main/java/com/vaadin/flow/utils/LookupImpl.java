/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.flow.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * An implementation of Lookup, which could be used to find service(s) of a
 * given type.
 */
public class LookupImpl implements Lookup {

    private ClassFinder classFinder;

    /**
     * Creates an implementation of Lookup.
     *
     * @param classFinder
     *            for searching service implementations.
     */
    public LookupImpl(ClassFinder classFinder) {
        this.classFinder = classFinder;
    }

    @Override
    public <T> T lookup(Class<T> serviceClass) {
        if (ResourceProvider.class.isAssignableFrom(serviceClass)) {
            return serviceClass.cast(new ResourceProviderImpl(classFinder));
        }
        return lookupAll(serviceClass).stream().findFirst().orElse(null);
    }

    @Override
    public <T> List<T> lookupAll(Class<T> serviceClass) {
        Set<Class<? extends T>> subTypes = classFinder
                .getSubTypesOf(serviceClass);
        List<T> result = new ArrayList<>(subTypes.size());
        for (Class<? extends T> clazz : subTypes) {
            if (!ReflectTools.isInstantiableService(clazz)) {
                continue;
            }
            result.add(ReflectTools.createInstance(clazz));
        }
        return result;
    }
}
