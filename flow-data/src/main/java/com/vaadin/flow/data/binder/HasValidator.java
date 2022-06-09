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

import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

/**
 * A generic interface for field components and other user interface objects
 * that have a user-editable value that should be validated.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <V>
 *            the value type
 */
public interface HasValidator<V> extends Serializable {

    /**
     * Returns a validator that checks the state of the Value. This should be
     * overridden for components with internal value conversion or validation,
     * e.g. when the user is providing a string that has to be parsed into a
     * date. An invalid input from user will be exposed to a {@code Binder} and
     * can be seen as a validation failure.
     *
     * @return state validator
     */
    default Validator<V> getDefaultValidator() {
        return Validator.alwaysPass();
    }

    /**
     * Enables the implementing components to announce changes in their
     * validation status to the observers.
     *
     * @see com.vaadin.flow.data.binder.Binder.BindingBuilderImpl#bind(ValueProvider,
     *      Setter)
     * @since 23.2 ??
     *
     * @return Registration of the added listener.
     */
    default Registration addValidationStatusListener(
            ValidationStatusListener<V> listener) {
        throw new RuntimeException(
                "The default implementation should be overridden.");
    }
}
