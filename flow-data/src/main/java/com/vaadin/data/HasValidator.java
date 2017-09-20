/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.data;

/**
 * A generic interface for field components and other user interface objects
 * that have a user-editable value. Emits change events whenever the value is
 * changed, either by the user or programmatically.
 *
 * @author Vaadin Ltd.
 *
 * @param <V>
 *            the value type
 */
public interface HasValidator<V> {

    /**
     * Returns a validator that checks the internal state of the HasValue. This
     * should be overridden for components with internal value conversion or
     * validation, eg. when the user is providing a string that has to be parsed
     * into a date. An invalid input from user will be exposed to a
     * {@code Binder} and can be seen as a validation failure.
     *
     * @since 8.1
     * @return internal state validator
     */
    default Validator<V> getDefaultValidator() {
        return Validator.alwaysPass();
    }
}
