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
     * Enables the implementing components to notify changes in their validation
     * status to the observers.
     * <p>
     * <strong>Note:</strong> This method can be overridden by the implementing
     * classes e.g. components, to enable the associated {@link Binder.Binding}
     * instance subscribing for their validation change events and revalidate
     * itself.
     * <p>
     * This method primarily designed for notifying the Binding about the
     * validation status changes of a bound component at the client-side.
     * WebComponents such as <code>&lt;vaadin-date-picker&gt;</code> or any
     * other component that accept a formatted text as input should be able to
     * communicate their invalid status to their server-side instance, and a
     * bound server-side component instance must notify its binding about this
     * validation status change as well. When the binding instance revalidates,
     * a chain of validators and convertors get executed one of which is the
     * default validator provided by {@link HasValidator#getDefaultValidator()}.
     * Thus, In order for the binding to be able to show/clear errors for its
     * associated bound field, it is important that implementing components take
     * that validation status into account while implementing any validator and
     * converter including {@link HasValidator#getDefaultValidator()}. Here is
     * an example:
     *
     * <pre>
     * &#64;Tag("date-picker-demo")
     * public class DatePickerDemo implements HasValidator&lt;LocalDate&gt; {
     *
     *     // Each web component has a way to communicate its validation status
     *     // to its server-side component instance. The following
     *     // clientSideValid state is introduced here just for the sake of
     *     // simplicity of this code snippet:
     *     boolean clientSideValid = true;
     *
     *     /**
     *      * Note how <code>clientSideValid</code> engaged in the definition of
     *      * this method. It is important to reflect this status either in the
     *      * returning validation result of this method or any other validation
     *      * that is associated with this component.
     *     *&#47;
     *     &#64;Override
     *     public Validator getDefaultValidator() {
     *         return (value, valueContext) -&gt; clientSideValid
     *                 ? ValidationResult.ok()
     *                 : ValidationResult.error("Invalid date format");
     *     }
     *
     *     private final Collection&lt;ValidationStatusChangeListener&lt;LocalDate&gt;&gt; validationStatusListeners = new ArrayList&lt;&gt;();
     *
     *     /**
     *      * This enables the binding to subscribe for the validation status
     *      * change events that are fired by this component and revalidate
     *      * itself respectively.
     *     *&#47;
     *     &#64;Override
     *     public Registration addValidationStatusChangeListener(
     *             ValidationStatusChangeListener&lt;LocalDate&gt; listener) {
     *         validationStatusListeners.add(listener);
     *         return () -&gt; validationStatusListeners.remove(listener);
     *     }
     *
     *     private void fireValidationStatusChangeEvent(
     *             boolean newValidationStatus) {
     *         if (this.clientSideValid != newValidationStatus) {
     *             this.clientSideValid = newValidationStatus;
     *             var event = new ValidationStatusChangeEvent&lt;&gt;(this,
     *                     newValidationStatus);
     *             validationStatusListeners.forEach(
     *                     listener -&gt; listener.validationStatusChanged(event));
     *         }
     *     }
     * }
     * </pre>
     *
     * @see com.vaadin.flow.data.binder.Binder.BindingBuilderImpl#bind(ValueProvider,
     *      Setter)
     * @since 23.2
     *
     * @return Registration of the added listener.
     */
    default Registration addValidationStatusChangeListener(
            ValidationStatusChangeListener<V> listener) {
        return null;
    }
}
