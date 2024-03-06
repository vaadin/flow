/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.util.EventObject;

import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.function.ValueProvider;

/**
 * Binder status change event.
 * <p>
 * The {@link Binder} status is changed whenever any of the following happens:
 * <ul>
 * <li>if any of its bound fields or selects have been changed
 * <li>{@link Binder#writeBean(Object)} or
 * {@link Binder#writeBeanIfValid(Object)} is called
 * <li>{@link Binder#readBean(Object)} is called
 * <li>{@link Binder#setBean(Object)} is called
 * <li>{@link Binder#removeBean()} is called
 * <li>{@link BindingBuilder#bind(ValueProvider, Setter)} is called
 * <li>{@link Binder#validate()} or {@link Binding#validate()} is called
 * </ul>
 *
 * @see StatusChangeListener#statusChange(StatusChangeEvent)
 * @see Binder#addStatusChangeListener(StatusChangeListener)
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class StatusChangeEvent extends EventObject {

    private final boolean hasValidationErrors;

    /**
     * Create a new status change event for given {@code binder}, storing
     * information of whether the change that triggered this event caused
     * validation errors.
     *
     * @param binder
     *            the event source binder
     * @param hasValidationErrors
     *            the validation status associated with this event
     */
    public StatusChangeEvent(Binder<?> binder, boolean hasValidationErrors) {
        super(binder);
        this.hasValidationErrors = hasValidationErrors;
    }

    /**
     * Gets the associated validation status.
     *
     * @return {@code true} if the change that triggered this event caused
     *         validation errors, {@code false} otherwise
     */
    public boolean hasValidationErrors() {
        return hasValidationErrors;
    }

    @Override
    public Binder<?> getSource() {
        return (Binder<?>) super.getSource();
    }

    /**
     * Gets the binder.
     *
     * @return the binder
     */
    public Binder<?> getBinder() {
        return getSource();
    }

}
