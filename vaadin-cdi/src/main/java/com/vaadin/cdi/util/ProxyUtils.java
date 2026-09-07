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

import jakarta.enterprise.inject.Typed;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for proxies
 */
@Typed()
public abstract class ProxyUtils {
    private ProxyUtils() {
        // prevent instantiation
    }

    /**
     * @param currentClass
     *            current class
     * @return class of the real implementation
     */
    public static Class getUnproxiedClass(Class currentClass) {
        Class unproxiedClass = currentClass;

        while (isProxiedClass(unproxiedClass)) {
            unproxiedClass = unproxiedClass.getSuperclass();
        }

        return unproxiedClass;
    }

    /**
     * Analyses if the given class is a generated proxy class
     * 
     * @param currentClass
     *            current class
     * @return true if the given class is a known proxy class, false otherwise
     */
    public static boolean isProxiedClass(Class currentClass) {
        if (currentClass == null || currentClass.getSuperclass() == null) {
            return false;
        }

        String name = currentClass.getName();
        return name.startsWith(currentClass.getSuperclass().getName())
                && (name.contains("$$") // CDI
                        || name.contains("_ClientProxy") // Quarkus
                        || name.contains("$HibernateProxy$")); // Hibernate
    }

    public static List<Class<?>> getProxyAndBaseTypes(Class<?> proxyClass) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        result.add(proxyClass);

        if (isInterfaceProxy(proxyClass)) {
            for (Class<?> currentInterface : proxyClass.getInterfaces()) {
                if (proxyClass.getName()
                        .startsWith(currentInterface.getName())) {
                    result.add(currentInterface);
                }
            }
        } else {
            Class unproxiedClass = proxyClass.getSuperclass();
            result.add(unproxiedClass);

            while (isProxiedClass(unproxiedClass)) {
                unproxiedClass = unproxiedClass.getSuperclass();
                result.add(unproxiedClass);
            }
        }
        return result;
    }

    public static boolean isInterfaceProxy(Class<?> proxyClass) {
        Class<?>[] interfaces = proxyClass.getInterfaces();
        if (Proxy.class.equals(proxyClass.getSuperclass()) && interfaces != null
                && interfaces.length > 0) {
            return true;
        }

        if (proxyClass.getSuperclass() != null
                && !proxyClass.getSuperclass().equals(Object.class)) {
            return false;
        }

        if (proxyClass.getName().contains("$$")) {
            for (Class<?> currentInterface : interfaces) {
                if (proxyClass.getName()
                        .startsWith(currentInterface.getName())) {
                    return true;
                }
            }
        }

        return false;
    }
}
