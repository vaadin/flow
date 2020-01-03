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

/**
 * Represents an event the webcomponent can trigger.
 *
 * @see ComponentMetadata
 * @since 1.0
 */
public class ComponentEventData {

    private String name;
    private String description;
    private List<ComponentPropertyBaseData> properties;

    /**
     * Gets the name of the event, such as "click".
     *
     * @return The name of the event.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the event, such as "click".
     *
     * @param name
     *            The name of the event.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the public description of the event, that can be used to generate
     * the corresponding Javadoc at the Java class.
     *
     * @return The event-level description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the public description of the event, that can be used to generate
     * the corresponding Javadoc at the Java class.
     *
     * @param description
     *            The event-level description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the list of properties that are part of the event. Each property is
     * represented by the {@link ComponentPropertyBaseData} object.
     *
     * @return The list of properties that are part of the event.
     */
    public List<ComponentPropertyBaseData> getProperties() {
        return properties == null ? new ArrayList<>(0) : properties;
    }

    /**
     * Sets the list of properties that are part of the event. Each property is
     * represented by the {@link ComponentPropertyBaseData} object.
     *
     * @param properties
     *            The list of properties that are part of the event.
     */
    public void setProperties(List<ComponentPropertyBaseData> properties) {
        this.properties = properties;
    }

}
