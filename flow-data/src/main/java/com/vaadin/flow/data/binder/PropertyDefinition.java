/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.function.ValueProvider;

/**
 * A property from a {@link PropertySet}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the property set
 * @param <V>
 *            the property type
 */
public interface PropertyDefinition<T, V> extends Serializable {
    /**
     * Gets the value provider that is used for finding the value of this
     * property for a bean.
     *
     * @return the getter, not <code>null</code>
     */
    ValueProvider<T, V> getGetter();

    /**
     * Gets an optional setter for storing a property value in a bean.
     *
     * @return the setter, or an empty optional if this property is read-only
     */
    Optional<Setter<T, V>> getSetter();

    /**
     * Gets the type of this property.
     *
     * @return the property type. not <code>null</code>
     */
    Class<V> getType();

    /**
     * Gets the type of the class containing this property.
     *
     * @return the property type. not <code>null</code>
     */
    Class<?> getPropertyHolderType();

    /**
     * Gets the full name of this property.
     *
     * @return the property name, not <code>null</code>
     */
    String getName();

    /**
     * Gets the top level name of this property.
     *
     * @return the top level property name, not <code>null</code>
     */
    default String getTopLevelName() {
        return getName();
    }

    /**
     * Gets the human readable caption to show for this property.
     *
     * @return the caption to show, not <code>null</code>
     */
    String getCaption();

    /**
     * Gets the {@link PropertySet} that this property belongs to.
     *
     * @return the property set, not <code>null</code>
     */
    PropertySet<T> getPropertySet();

    /**
     * Gets the parent property of this property if this is a sub-property of
     * the property set. If this property belongs directly to the property set,
     * it doesn't have a parent and this method returns {@code null}.
     *
     * @return the parent property, may be {@code null}
     */
    PropertyDefinition<T, ?> getParent();

    /**
     * Gets whether this property belongs to some other property in the property
     * set, or directly to the property set.
     *
     * @return {@code true} if this property is a sub-property of the property
     *         set it belongs to, {@code false} otherwise
     */
    default boolean isSubProperty() {
        return getParent() != null;
    }
}
