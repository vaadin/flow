/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import com.vaadin.flow.component.HasText;
import com.vaadin.flow.function.SerializableEventListener;

/**
 * Handler for {@link BinderValidationStatus} changes.
 * <p>
 * {@link Binder#setValidationStatusHandler(BinderValidationStatusHandler)
 * Register} an instance of this class to be able to customize validation status
 * handling.
 * <p>
 * error message} for failed field validations. For bean level validation errors
 * the default handler will display the first error message in
 * {@link Binder#setStatusLabel(HasText) status label}, if one has been set.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @see BinderValidationStatus
 * @see Binder#validate()
 * @see BindingValidationStatus
 *
 * @param <BEAN>
 *            the bean type of binder
 *
 */
@FunctionalInterface
public interface BinderValidationStatusHandler<BEAN>
        extends SerializableEventListener {

    /**
     * Invoked when the validation status has changed in binder.
     *
     * @param statusChange
     *            the changed status
     */
    void statusChange(BinderValidationStatus<BEAN> statusChange);

}
