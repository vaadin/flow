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
import java.util.Objects;

/**
 * Value object containing information of a WebComponent property field.
 */
public class PropertyData implements Serializable {

    private final String name;
    private final Class<?> type;
    private final String initialValue;

    /**
     * Public constructor.
     *
     * @param name
     *         name of property
     * @param type
     *         property value class type
     * @param initialValue
     *         initial value as a String
     */
    public PropertyData(String name, Class<?> type, String initialValue) {
        Objects.requireNonNull(name, "Property needs to have a name");
        Objects.requireNonNull(type, "Property needs to expose type");
        this.name = name;
        this.type = type;
        this.initialValue = initialValue;
    }

    /**
     * Getter for the property name.
     *
     * @return property name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the property value class type.
     *
     * @return value class type
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Getter for the initial value if given.
     *
     * @return initial string value or {@code null} if none given
     */
    public String getInitialValue() {
        return initialValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropertyData) {
            PropertyData other = (PropertyData) obj;
            return getName().equals(other.getName()) && getType()
                    .equals(other.getType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
