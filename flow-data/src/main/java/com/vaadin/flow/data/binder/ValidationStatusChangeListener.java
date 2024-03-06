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

import com.vaadin.flow.function.ValueProvider;

/**
 * The listener interface for receiving {@link ValidationStatusChangeEvent}
 * events. The classes that are interested in processing validation status
 * changed events of field components should register implementation of this
 * interface via
 * {@link HasValidator#addValidationStatusChangeListener(ValidationStatusChangeListener)}
 * which are called whenever such event is fired by the component.
 * <p>
 * This interface is primarily introduced to enable binding instances subscribe
 * for their own associated field's validation status change events and
 * revalidate after that. However, when all the components implementing
 * {@code HasValidator} interface, implement the correct behaviour for adding
 * and notifying listeners of the current type, other usages are also become
 * possible since the {@link ValidationStatusChangeEvent} payload contains the
 * source {@link com.vaadin.flow.component.HasValue} field and the new
 * validation status, thus for instance fields or buttons in a view can
 * subscribe for each other's validation statuses and enable/disable or clear
 * values, etc. respectively.
 *
 * @since 23.2
 *
 * @see HasValidator
 * @see com.vaadin.flow.data.binder.Binder.BindingBuilderImpl#bind(ValueProvider,
 *      Setter)
 */
@FunctionalInterface
public interface ValidationStatusChangeListener<V> extends Serializable {

    /**
     * Invoked when a ValidationStatusChangeEvent occurs.
     *
     * @param event
     *            the event to be processed
     */
    void validationStatusChanged(ValidationStatusChangeEvent<V> event);
}
