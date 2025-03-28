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
package com.vaadin.flow.data.binder;

import java.beans.PropertyDescriptor;
import java.lang.reflect.TypeVariable;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.shared.util.SharedUtil;

/**
 * Abstract base class for PropertyDefinition implementations for beans.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the property set
 * @param <V>
 *            the property type
 */
public abstract class AbstractBeanPropertyDefinition<T, V>
        implements PropertyDefinition<T, V> {

    private final PropertyDescriptor descriptor;
    private final BeanPropertySet<T> propertySet;
    private final Class<?> propertyHolderType;

    /**
     * Constructor for setting the immutable descriptor, property set and
     * property holder type used by this instance.
     *
     * @param propertySet
     *            property set this property belongs to
     * @param propertyHolderType
     *            property holder type
     * @param descriptor
     *            property descriptor
     */
    public AbstractBeanPropertyDefinition(BeanPropertySet<T> propertySet,
            Class<?> propertyHolderType, PropertyDescriptor descriptor) {
        this.propertySet = propertySet;
        this.propertyHolderType = propertyHolderType;
        this.descriptor = descriptor;
        if (descriptor.getReadMethod() == null) {
            throw new IllegalArgumentException(
                    "Bean property has no accessible getter: "
                            + propertySet.getBeanType() + "."
                            + descriptor.getName());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<V> getType() {
        return (Class<V>) ReflectTools
                .convertPrimitiveType(descriptor.getPropertyType());
    }

    @Override
    public boolean isGenericType() {
        return descriptor.getReadMethod()
                .getGenericReturnType() instanceof TypeVariable<?>;
    }

    @Override
    public String getName() {
        return descriptor.getName();
    }

    @Override
    public String getCaption() {
        return SharedUtil.propertyIdToHumanFriendly(getName());
    }

    @Override
    public BeanPropertySet<T> getPropertySet() {
        return propertySet;
    }

    /**
     * Gets the property descriptor of this instance.
     *
     * @return the property descriptor
     */
    protected PropertyDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Class<?> getPropertyHolderType() {
        return propertyHolderType;
    }
}
