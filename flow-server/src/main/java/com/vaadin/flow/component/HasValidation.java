/*
 * Copyright 2000-2023 Vaadin Ltd.
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

package com.vaadin.flow.component;

import java.io.Serializable;

/**
 * A component that supports input validation.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public interface HasValidation extends Serializable {
    /**
     * Sets whether the component's internal validation is disabled.
     * <p>
     * When disabled, the component doesn't automatically validate values
     * against constraints such as required and so on, which also means that
     * the invalid state isn't affected. However, it's still possible to use
     * Binder or implement custom validation logic.
     *
     * @param disabled whether the validation should be disabled.
     */
    void setInternalValidationDisabled(boolean disabled);

    /**
     * Returns {@code true} if the component's internal validation is disabled,
     * {@code false} otherwise.
     *
     * @return whether the validation is disabled.
     */
    boolean isInternalValidationDisabled();

    /**
     * Sets an error message to the component.
     * <p>
     * The Web Component is responsible for deciding when to show the error
     * message to the user, and this is usually triggered by triggering the
     * invalid state for the Web Component. Which means that there is no need to
     * clean up the message when component becomes valid (otherwise it may lead
     * to undesired visual effects).
     *
     * @param errorMessage
     *            a new error message
     */
    void setErrorMessage(String errorMessage);

    /**
     * Gets current error message from the component.
     *
     * @return current error message
     */
    String getErrorMessage();

    /**
     * Sets the validity of the component input.
     * <p>
     * When component becomes valid it hides the error message by itself, so
     * there is no need to clean up the error message via the
     * {@link #setErrorMessage(String)} call.
     *
     * @param invalid
     *            new value for component input validity
     */
    void setInvalid(boolean invalid);

    /**
     * Returns {@code true} if component input is invalid, {@code false}
     * otherwise.
     *
     * @return whether the component input is valid
     */
    boolean isInvalid();
}
