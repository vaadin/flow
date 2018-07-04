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
package com.vaadin.flow.component;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;

/**
 * A component that has a value.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <E>
 *            the value change event type
 * @param <V>
 *            the value type
 */
public interface HasValueAndElement<E extends ValueChangeEvent<V>, V>
        extends HasValue<E, V>, HasElement, HasEnabled {

    @Override
    default void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        getElement().setProperty("required", requiredIndicatorVisible);
    }

    @Override
    default boolean isRequiredIndicatorVisible() {
        return getElement().getProperty("required", false);
    }

    @Override
    default void setReadOnly(boolean readOnly) {
        getElement().setProperty("readonly", readOnly);
    }

    @Override
    default boolean isReadOnly() {
        return getElement().getProperty("readonly", false);
    }
}
