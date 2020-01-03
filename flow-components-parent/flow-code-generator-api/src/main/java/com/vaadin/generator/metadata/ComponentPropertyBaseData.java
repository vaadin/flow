/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.generator.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base class of properties exposed by the webcomponent, that can be properties
 * themselves, event properties or function parameters.
 *
 * @see ComponentMetadata
 * @since 1.0
 */
public class ComponentPropertyBaseData {

    private String name;
    private Set<ComponentBasicType> type;
    private String description;
    private List<ComponentObjectType> objectType = new ArrayList<>();

    /**
     * Gets the object type properties.
     *
     * @return a list of the object type properties
     */
    public List<ComponentObjectType> getObjectType() {
        return objectType;
    }

    /**
     * Sets the object type properties.
     *
     * @param objectType
     *            the
     */
    public void setObjectType(List<ComponentObjectType> objectType) {
        this.objectType = objectType;
    }

    /**
     * Gets the name of the property.
     *
     * @return the name The name of the property.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the property.
     *
     * @param name
     *            The name of the property.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the type of the property, or if property allows varying types, a
     * list of those.
     *
     * @return the type the type in a list or the a list of allowed types
     */
    public Set<ComponentBasicType> getType() {
        return type;
    }

    /**
     * Sets the type of the property, or if property allows varying types, a
     * list of those.
     *
     * @param type
     *            The type of the property in a list or a list of allowed types
     */
    public void setType(Set<ComponentBasicType> type) {
        this.type = type;
    }

    /**
     * Gets the public description of the function parameter, that can be used
     * to generate the corresponding Javadoc at the Java class.
     *
     * @return The function parameter description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the public description of the function parameter, that can be used
     * to generate the corresponding Javadoc at the Java class.
     *
     * @param description
     *            The function parameter description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
