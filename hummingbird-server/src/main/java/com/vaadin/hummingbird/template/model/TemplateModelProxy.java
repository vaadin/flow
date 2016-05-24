/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.vaadin.hummingbird.StateNode;

/**
 * Proxy object between {@link TemplateModel} and the {@link StateNode}s that
 * actually store the information.
 *
 * @author Vaadin Ltd
 */
public class TemplateModelProxy implements InvocationHandler {

    static <T> T createProxy(Class<T> modelType) {
        return modelType.cast(Proxy.newProxyInstance(modelType.getClassLoader(),
                new Class[] { modelType }, new TemplateModelProxy()));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        // NOOP for now
        return null;
    }

}
