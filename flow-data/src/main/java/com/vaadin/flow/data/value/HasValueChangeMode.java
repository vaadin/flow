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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.dom.Element;

/**
 * An interface, denoting that the component is able to change the way its value
 * on the client side is synchronized with the server side.
 * <p>
 * The value which mode is changed is defined by
 * {@link HasValue#getClientValuePropertyName()}.
 * <p>
 * Any component implementing this interface should take care of setting the
 * default value for its value change mode.
 *
 * @param <C>
 *            the component type
 * @param <V>
 *            the value type
 * 
 * @author Vaadin Ltd.
 */
public interface HasValueChangeMode<C extends Component, V>
        extends HasValue<C, V>, HasElement {

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
    default void setValueChangeMode(ValueChangeMode valueChangeMode) {
        Element element = get().getElement();

        element.removeSynchronizedPropertyEvent(
                getClientPropertyChangeEventName());
        element.removeSynchronizedPropertyEvent("blur");
        element.removeSynchronizedPropertyEvent("change");

        if (valueChangeMode == null) {
            element.removeSynchronizedProperty(getClientValuePropertyName());
            return;
        }

        switch (valueChangeMode) {
        case EAGER:
            element.synchronizeProperty(getClientValuePropertyName(),
                    getClientPropertyChangeEventName());
            break;
        case ON_BLUR:
            element.synchronizeProperty(getClientValuePropertyName(), "blur");
            break;
        case ON_CHANGE:
            element.synchronizeProperty(getClientValuePropertyName(), "change");
            break;
        default:
            throw new IllegalArgumentException(
                    "Unexpected value change mode: " + valueChangeMode);
        }
    }
}
