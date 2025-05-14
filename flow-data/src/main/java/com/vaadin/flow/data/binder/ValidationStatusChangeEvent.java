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

import com.vaadin.flow.component.HasValue;

import java.io.Serializable;

/**
 * The event to be processed when
 * {@link ValidationStatusChangeListener#validationStatusChanged(ValidationStatusChangeEvent)}
 * invoked.
 *
 * @since 23.2
 *
 * @param <V>
 *            the value type
 */
public class ValidationStatusChangeEvent<V> implements Serializable {

    private final HasValue<?, V> source;
    private final boolean newStatus;

    public ValidationStatusChangeEvent(HasValue<?, V> source,
            boolean newStatus) {
        this.source = source;
        this.newStatus = newStatus;
    }

    public HasValue<?, V> getSource() {
        return source;
    }

    public boolean getNewStatus() {
        return newStatus;
    }
}
