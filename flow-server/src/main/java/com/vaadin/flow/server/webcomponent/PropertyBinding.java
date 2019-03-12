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

package com.vaadin.flow.server.webcomponent;

import java.util.Objects;

import com.vaadin.flow.function.SerializableConsumer;

public class PropertyBinding<P> {
    private PropertyData2<P> data;
    private SerializableConsumer<P> listener;
    private P value;

    public PropertyBinding(PropertyData2<P> data,
                           SerializableConsumer<P> listener) {
        Objects.requireNonNull(data, "Parameter 'data' must not be null!");
        this.data = data;
        this.listener = listener;
        this.value = data.getDefaultValue();
    }

    public void updateValue(Object value) {
        if (value != null && value.getClass() != getType()) {
            // TODO: throw a specific exception here
            throw new RuntimeException(String.format("Parameter 'value' is of" +
                            " the wrong type: onChangeHandler of the property " +
                            "expected to receive %s but found %s instead.",
                    getType().getCanonicalName(),
                    value.getClass().getCanonicalName()));
        }
        P newValue = (P)value;

        boolean updated = false;
        if (this.value != null && !this.value.equals(newValue)) {
            updated = true;
        }
        else if (newValue != null && !newValue.equals(this.value)) {
            updated = true;
        }
        this.value = newValue;

        if (updated) {
            notifyValueChange();
        }
    }

    public Class<P> getType() {
        return data.getType();
    }

    public String getName() {
        return data.getName();
    }

    public P getValue() {
        return value;
    }

    public boolean isReadOnly() {
        return data.isReadOnly();
    }

    void notifyValueChange() {
        if (listener != null) {
            listener.accept(this.value);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropertyBinding) {
            PropertyBinding other = (PropertyBinding) obj;
            return data.equals(other.data) && (
                    (value == null && other.value == null)
                            || (value != null && value.equals(other.value)));
        }
        return false;
    }
}
