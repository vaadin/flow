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

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Objects;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.SerializableConsumer;

public class PropertyBinding<P extends Serializable> implements Serializable {
    private PropertyData<P> data;
    private SerializableConsumer<P> listener;
    private P value;

    public PropertyBinding(PropertyData<P> data,
                           SerializableConsumer<P> listener) {
        Objects.requireNonNull(data, "Parameter 'data' must not be null!");
        this.data = data;
        this.listener = listener;
        this.value = data.getDefaultValue();
    }

    public void updateValue(Serializable newValue) {
        if (isReadOnly()) {
            LoggerFactory.getLogger(WebComponentBindingImpl.class)
                    .warn(String.format("An attempt was made to write to " +
                                    "a read-only property '%s' owned by exported " +
                                    "component %s", getName(),
                            getType().getCanonicalName()));
            return;
        }

        if (newValue != null && newValue.getClass() != getType()) {
            throw new InvalidParameterException(String.format("Parameter " +
                            "'newValue' is of the wrong type: onChangeHandler" +
                            " of the property expected to receive %s but " +
                            "found %s instead.",
                    getType().getCanonicalName(),
                    newValue.getClass().getCanonicalName()));
        }

        P newTypedValue = (P)newValue;

        // null values are always set to default value (which might still be
        // null for some types)
        if (newTypedValue == null) {
            newTypedValue = data.getDefaultValue();
        }

        boolean updated = false;
        if (this.value != null && !this.value.equals(newTypedValue)) {
            updated = true;
        }
        else if (newValue != null && !newValue.equals(this.value)) {
            updated = true;
        }

        if (updated) {
            this.value = newTypedValue;
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
        return Objects.hash(data, value);
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
