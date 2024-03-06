/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import com.vaadin.flow.component.HasValue;

import java.io.Serializable;

/**
 * The event to be processed when
 * {@link ValidationStatusChangeListener#validationStatusChanged(ValidationStatusChangeEvent)}
 * invoked.
 *
 * @since 23.2
 *
 * @param <V>
 *            the value type
 */
public class ValidationStatusChangeEvent<V> implements Serializable {

    private final HasValue<?, V> source;
    private final boolean newStatus;

    public ValidationStatusChangeEvent(HasValue<?, V> source,
            boolean newStatus) {
        this.source = source;
        this.newStatus = newStatus;
    }

    public HasValue<?, V> getSource() {
        return source;
    }

    public boolean getNewStatus() {
        return newStatus;
    }
}
