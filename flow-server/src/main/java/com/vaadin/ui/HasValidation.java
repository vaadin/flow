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

package com.vaadin.ui;

/**
 * A component that supports input validation.
 *
 * @author Vaadin Ltd.
 */
public interface HasValidation {

    /**
     * Sets an error message to the component.
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
