/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import com.vaadin.flow.component.HasValue;

/**
 * Handler for applying effects to {@link HasValue} based on its
 * {@link ValidationResult}. *
 * {@link Binder#setValidationErrorHandler(BinderValidationErrorHandler)
 * Register} an instance of this class to be able to customize validation error
 * handling.
 *
 * 
 * @see DefaultBinderValidationErrorHandler
 * @see Binder#setValidationErrorHandler(BinderValidationErrorHandler)
 * @author Vaadin Ltd
 * @since
 *
 */
public interface BinderValidationErrorHandler extends Serializable {

    /**
     * Handles a validation error emitted when trying to write the value of the
     * given field.
     *
     * @param field
     *            the field with the invalid value
     * @param result
     *            the validation error result
     */
    void handleError(HasValue<?, ?> field, ValidationResult result);

    /**
     * Clears the error condition of the given field, if any.
     *
     * @param field
     *            the field with an invalid value
     */
    void clearError(HasValue<?, ?> field);
}
