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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinSession;

class BeanManagerProxy implements InvocationHandler {

    private BeanManager delegate;

    private final Bean<?> fakeBean = Mockito.mock(Bean.class);

    private final CreationalContext<?> fakeContext = Mockito
            .mock(CreationalContext.class);

    private final Class<?> mockingBeanType;

    BeanManagerProxy(BeanManager delegate, Class<?> mockingBeanType) {
        this.delegate = delegate;
        this.mockingBeanType = mockingBeanType;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        if (method.getName().equals("getBeans")
                && args[0].equals(mockingBeanType)) {
            Set<Bean<?>> set = Collections.singleton(fakeBean);
            return set;
        }
        if (method.getName().equals("resolve")
                && args[0].equals(Collections.singleton(fakeBean))) {
            return fakeBean;
        }
        if (method.getName().equals("createCreationalContext")
                && args[0].equals(fakeBean)) {
            return fakeContext;
        }
        if (method.getName().equals("getReference") && args[0].equals(fakeBean)
                && args[1].equals(mockingBeanType)
                && args[2].equals(fakeContext)) {
            VaadinSession session = VaadinSession.getCurrent();
            String attribute = getAttributeName(mockingBeanType);
            Object value = session.getAttribute(attribute);
            if (value == null) {
                value = BeanProvider.getContextualReference(delegate,
                        mockingBeanType, false);
                session.setAttribute(attribute, value);
            }
            return value;
        }

        return delegate(method, args);
    }

    private Object delegate(Method method, Object[] args) throws Throwable {
        Method[] methods = delegate.getClass().getDeclaredMethods();
        List<Method> filtered = Stream.of(methods).filter(
                origMethod -> origMethod.getName().equals(method.getName()))
                .collect(Collectors.toList());

        Method found = null;
        if (filtered.size() == 1) {
            found = filtered.get(0);
        } else {
            for (Method overloaded : filtered) {
                if (overloaded.getParameterCount() == method
                        .getParameterCount()) {
                    found = overloaded;
                    break;
                }
            }
        }
        return found.invoke(delegate, args);
    }

    static String getAttributeName(Class<?> mockingBeanType) {
        return "test-" + mockingBeanType;
    }

}
