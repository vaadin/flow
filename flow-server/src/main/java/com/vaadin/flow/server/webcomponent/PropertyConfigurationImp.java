/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.function.SerializableBiConsumer;

// TODO: this bad-boy replaces PropertyData
public class PropertyConfigurationImp<C extends Component, P> implements PropertyConfiguration<C, P> {
    // property
    private Class<C> componentClass;
    private String propertyName;
    private Class<P> propertyType;
    private P defaultValue;

    // configuration
    private boolean readOnly = false;
    private SerializableBiConsumer<C, P> onChangeHandler = null;

    public PropertyConfigurationImp(Class<C> componentType, String propertyName,
                                    Class<P> propertyType, P defaultValue) {
        this.componentClass = componentType;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.defaultValue = defaultValue;
    }

    @Override
    public PropertyConfiguration<C, P> onChange(SerializableBiConsumer<C, P> onChangeHandler) {
        Objects.requireNonNull(onChangeHandler, "Parameter 'onChangeHandler' " +
                "cannot be null!");
        this.onChangeHandler = onChangeHandler;
        return null;
    }

    PropertyData2<P> getPropertyData() {
        return new PropertyData2<>(propertyName, propertyType, defaultValue);
    }

    @Override
    public PropertyConfiguration<C, P> readOnly() {
        readOnly = true;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropertyConfigurationImp) {
            PropertyConfigurationImp other = (PropertyConfigurationImp) obj;
            return propertyName.equals(other.propertyName)
                    && componentClass.equals(other.componentClass)
                    && propertyType.equals(other.propertyType);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentClass, propertyName, propertyType);
    }

    public SerializableBiConsumer<C, Object> getOnChangeHandler() {
        return (c, o) -> onChangeHandler.accept(c, (P)o);
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public Class<P> getPropertyType() {
        return this.propertyType;
    }
}
