/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.value;

import java.io.Serializable;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.dom.DomListenerRegistration;

/**
 * Denotes that the component is able to change the way its value on the client
 * side is synchronized with the server side.
 * <p>
 * A class implementing this interface should typically also implement
 * {@link HasValue} even though this is not required on the API level.
 *
 * @see AbstractSinglePropertyField#setSynchronizedEvent(String)
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public interface HasValueChangeMode extends Serializable {

    /**
     * Default value change timeout for textual inputs in milliseconds.
     */
    int DEFAULT_CHANGE_TIMEOUT = 400;

    /**
     * Gets current value change mode of the component.
     *
     * @return current value change mode of the component, or {@code null} if
     *         the value is not synchronized
     */
    ValueChangeMode getValueChangeMode();

    /**
     * Sets new value change mode for the component.
     *
     * @param valueChangeMode
     *            new value change mode, or {@code null} to disable the value
     *            synchronization
     */
    void setValueChangeMode(ValueChangeMode valueChangeMode);

    /**
     * Sets how often {@link ValueChangeEvent}s are triggered when the
     * ValueChangeMode is set to {@link ValueChangeMode#LAZY}, or
     * {@link ValueChangeMode#TIMEOUT}.
     * <p>
     * Implementations should use
     * {@link ValueChangeMode#applyChangeTimeout(ValueChangeMode, int, DomListenerRegistration)}.
     *
     * @param valueChangeTimeout
     *            the timeout in milliseconds of how often
     *            {@link ValueChangeEvent}s are triggered.
     * @throws UnsupportedOperationException
     *             if neither {@link ValueChangeMode#LAZY}, nor
     *             {@link ValueChangeMode#TIMEOUT} is supported
     */
    default void setValueChangeTimeout(int valueChangeTimeout) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the currently set timeout, for how often
     * {@link ValueChangeEvent}s are triggered when the ValueChangeMode is set
     * to {@link ValueChangeMode#LAZY}, or {@link ValueChangeMode#TIMEOUT}.
     *
     * @return the timeout in milliseconds of how often
     *         {@link ValueChangeEvent}s are triggered.
     * @throws UnsupportedOperationException
     *             if neither {@link ValueChangeMode#LAZY}, nor
     *             {@link ValueChangeMode#TIMEOUT} is supported
     */
    default int getValueChangeTimeout() {
        throw new UnsupportedOperationException();
    }

}
