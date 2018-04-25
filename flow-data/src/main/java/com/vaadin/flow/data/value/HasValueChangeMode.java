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

package com.vaadin.flow.data.value;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.HasValue;

/**
 * An interface, denoting that the component is able to change the way its value
 * on the client side is synchronized with the server side.
 * <p>
 * A class implementing this interface should typically also implement
 * {@link HasValue} even though this is not required on the API level.
 *
 * @see AbstractSinglePropertyField#setSynchronizedEvent(String)
 *
 * @author Vaadin Ltd.
 */
public interface HasValueChangeMode {

    /**
     * Gets current value change mode of the component.
     *
     * @return current value change mode of the component, or {@code null} if
     *         the value is not synchronized
     */
    ValueChangeMode getValueChangeMode();

    /**
     * Sets new value change mode for the component.
     *
     * @param valueChangeMode
     *            new value change mode, or {@code null} to disable the value
     *            synchronization
     */
    void setValueChangeMode(ValueChangeMode valueChangeMode);
}
