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
            return serviceClass.cast(new ResourceProviderImpl());
        }
        return lookupAll(serviceClass).stream().findFirst().orElse(null);
    }

    @Override
    public <T> List<T> lookupAll(Class<T> serviceClass) {
        Set<?> subTypes = classFinder
                .getSubTypesOf(loadClassFromClassFindler(serviceClass));
        List<T> result = new ArrayList<>(subTypes.size());
        try {
            for (Object clazz : subTypes) {
                if (!ReflectTools.isInstantiableService((Class<?>) clazz)) {
                    continue;
                }
                Class<?> serviceType = serviceClass.getClassLoader()
                        .loadClass(((Class<?>) clazz).getName());
                result.add(serviceClass
                        .cast(ReflectTools.createInstance(serviceType)));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not find service class", e);
        }
        return result;
    }

    private Class<?> loadClassFromClassFindler(Class<?> clz) {
        try {
            return classFinder.loadClass(clz.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Could not load " + clz.getName() + " class", e);
        }
    }

}
