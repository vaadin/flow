/*
 * Copyright 2000-2018 Vaadin Ltd.
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
 * {@link Binder#setStatusLabel(HasText) status label}, if one has
 * been set.
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
