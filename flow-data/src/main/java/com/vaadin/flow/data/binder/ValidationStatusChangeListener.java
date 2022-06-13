/*
 * Copyright 2000-2022 Vaadin Ltd.
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

/**
 * The listener interface for receiving {@link ValidationStatusChangeEvent}
 * events. The classes that are interested in processing a Validation status
 * changed event of field components e.g.
 * {@link com.vaadin.flow.data.binder.Binder.BindingBuilderImpl}, register
 * implementation of this interface via
 * {@link HasValidator#addValidationStatusChangeListener(ValidationStatusChangeListener)}
 * which are called whenever such event is fired by the component class, e.g.
 * {@code datePicker}.
 *
 * @since 23.2
 *
 * @see HasValidator
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
