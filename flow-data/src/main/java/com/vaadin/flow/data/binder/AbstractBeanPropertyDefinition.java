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
package com.vaadin.flow.data.binder;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
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

    private final String name;
    private final Method readMethod;
    private final Method writeMethod;
    private final BeanPropertySet<T> propertySet;
    private final Class<?> propertyHolderType;

    /**
     * Constructor for setting the immutable property, property set and property
     * holder type used by this instance.
     *
     * @param propertySet
     *            property set this property belongs to
     * @param propertyHolderType
     *            property holder type
     * @param name
     *            the name of the property
     * @param readMethod
     *            the getter of the property, not {@code null}
     * @param writeMethod
     *            the setter of the property, or {@code null} for a read-only
     *            property
     */
    public AbstractBeanPropertyDefinition(BeanPropertySet<T> propertySet,
            Class<?> propertyHolderType, String name, Method readMethod,
            Method writeMethod) {
        this.propertySet = propertySet;
        this.propertyHolderType = propertyHolderType;
        this.name = name;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        if (readMethod == null) {
            throw new IllegalArgumentException(
                    "Bean property has no accessible getter: "
                            + propertySet.getBeanType() + "." + name);
        }
    }

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
     * @deprecated Use
     *             {@link #AbstractBeanPropertyDefinition(BeanPropertySet, Class, String, Method, Method)},
     *             which needs no {@code java.desktop} module
     */
    @Deprecated(since = "25.4", forRemoval = true)
    public AbstractBeanPropertyDefinition(BeanPropertySet<T> propertySet,
            Class<?> propertyHolderType, PropertyDescriptor descriptor) {
        this(propertySet, propertyHolderType, descriptor.getName(),
                descriptor.getReadMethod(), descriptor.getWriteMethod());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<V> getType() {
        return (Class<V>) ReflectTools
                .convertPrimitiveType(readMethod.getReturnType());
    }

    @Override
    public boolean isGenericType() {
        return readMethod.getGenericReturnType() instanceof TypeVariable<?>;
    }

    @Override
    public String getName() {
        return name;
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
     * Gets the getter of this property.
     *
     * @return the getter, not {@code null}
     */
    protected Method getReadMethod() {
        return readMethod;
    }

    /**
     * Gets the setter of this property.
     *
     * @return the setter, or {@code null} for a read-only property
     */
    protected Method getWriteMethod() {
        return writeMethod;
    }

    /**
     * Gets the property descriptor of this instance.
     *
     * @return the property descriptor
     * @deprecated Use {@link #getName()}, {@link #getReadMethod()} and
     *             {@link #getWriteMethod()}, which need no {@code java.desktop}
     *             module
     */
    @Deprecated(since = "25.4", forRemoval = true)
    protected PropertyDescriptor getDescriptor() {
        // A catch clause of IntrospectionException would make the JVM load
        // java.desktop when it verifies this class
        try {
            return new PropertyDescriptor(name, readMethod, writeMethod);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Class<?> getPropertyHolderType() {
        return propertyHolderType;
    }
}
