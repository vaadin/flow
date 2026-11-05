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
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * A property of a bean, a record or an interface, found by its accessor
 * methods, see {@link BeanUtil#getBeanProperties(Class)}.
 * <p>
 * Takes the place of {@code java.beans.PropertyDescriptor}, so that Flow does
 * not need the {@code java.desktop} module.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param name
 *            the name of the property
 * @param readMethod
 *            the getter, or {@code null} for a write-only property
 * @param writeMethod
 *            the setter, or {@code null} for a read-only property
 */
public record BeanProperty(String name, Method readMethod,
        Method writeMethod) implements Serializable {

    /**
     * Gets the type of the property: the return type of the getter, or the
     * parameter type of the setter of a write-only property.
     *
     * @return the type of the property
     */
    public Class<?> type() {
        if (readMethod != null) {
            return readMethod.getReturnType();
        }
        return writeMethod.getParameterTypes()[0];
    }
}
