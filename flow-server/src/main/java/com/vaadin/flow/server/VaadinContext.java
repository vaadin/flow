package com.vaadin.flow.server;

import java.util.function.Supplier;

/**
 * Context in which {@link VaadinService} is running.
 *
 * It is used to store service-scoped attributes.
 *
 * @author miki
 * @since 14.0.0
 */
public interface VaadinContext {

    /**
     * Returns value of the specified attribute, creating a default value if not
     * present.
     *
     * @param type
     *            Type of the attribute.
     * @param defaultValueSupplier
     *            {@link Supplier} of the default value, called when there is no
     *            value already present. May be {@code null}.
     * @return Value of the specified attribute.
     */
    <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier);

    /**
     * Returns value of the specified attribute.
     *
     * @param type
     *            Type of the attribute.
     * @return Value of the specified attribute.
     */
    default <T> T getAttribute(Class<T> type) {
        return getAttribute(type, null);
    }

    /**
     * Sets the attribute value, overriding previously existing one. Values are
     * based on exact type, meaning only one attribute of given type is possible
     * at any given time.
     *
     * @param value
     *            Value of the attribute. May not be {@code null}.
     */
    <T> void setAttribute(T value);

}
