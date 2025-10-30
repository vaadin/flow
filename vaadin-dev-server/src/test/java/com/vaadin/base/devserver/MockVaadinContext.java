/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.vaadin.flow.server.VaadinContext;

public class MockVaadinContext implements VaadinContext {

    Map<String, Object> attributes = new HashMap<>();

    @Override
    public <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier) {
        Object result = attributes.get(type.getName());
        if (result == null && defaultValueSupplier != null) {
            result = defaultValueSupplier.get();
            attributes.put(type.getName(), result);
        }
        return type.cast(result);
    }

    @Override
    public <T> void setAttribute(Class<T> clazz, T value) {
        attributes.put(clazz.getName(), value);
    }

    @Override
    public void removeAttribute(Class<?> clazz) {
        attributes.remove(clazz.getName());
    }

    @Override
    public Enumeration<String> getContextParameterNames() {
        return Collections.enumeration(Collections.emptyList());
    }

    @Override
    public String getContextParameter(String name) {
        return null;
    }
};
