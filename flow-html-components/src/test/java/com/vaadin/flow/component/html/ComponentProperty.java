/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.shared.util.SharedUtil;

public class ComponentProperty {
    public String name;
    public Object defaultValue, otherValue;
    public boolean optional;
    public boolean removeDefault;
    public Class<?> type;
    private Class<? extends Component> componentType;

    public <T> ComponentProperty(Class<? extends Component> componentType,
            String name, Class<T> type, T defaultValue, T otherValue,
            boolean optional, boolean removeDefault) {
        this.componentType = componentType;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.optional = optional;
        this.removeDefault = removeDefault;
        this.otherValue = otherValue == null ? name + name : otherValue;
    }

    public boolean isOptional() {
        return optional;
    }

    public Object getUsingGetter(Component component)
            throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {

        return getGetter().invoke(component);
    }

    public Method getGetter() throws NoSuchMethodException, SecurityException {
        String getterName;
        if (boolean.class.equals(type)) {
            getterName = "is" + SharedUtil.capitalize(name);
        } else {
            getterName = "get" + SharedUtil.capitalize(name);
        }
        Method m = componentType.getMethod(getterName, (Class<?>[]) null);
        return m;
    }

    public Method getSetter() throws NoSuchMethodException, SecurityException {
        String setterName = "set" + SharedUtil.capitalize(name);
        Method m = componentType.getMethod(setterName, type);
        return m;
    }

    public void setToOtherValueUsingSetter(Component component) {
        try {
            setUsingSetter(component, otherValue);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            throw new AssertionError(e);
        }
    }

    public void setUsingSetter(Component component, Object someValue)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException,
            SecurityException {
        getSetter().invoke(component, someValue);
    }

}
