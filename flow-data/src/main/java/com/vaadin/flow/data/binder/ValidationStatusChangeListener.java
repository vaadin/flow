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
 * @param <V>
 *            the value type
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
