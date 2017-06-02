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
package com.vaadin.generator.metadata;

import java.io.Serializable;

/**
 * Base class of properties exposed by the webcomponent, that can be properties
 * themselves, event properties or function parameters.
 *
 * @see ComponentMetadata
 */
public class ComponentPropertyBase implements Serializable {

    private String name;
    private ComponentObjectType type;

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
     * Gets the type of the property.
     * 
     * @return the type The type of the property.
     */
    public ComponentObjectType getType() {
        return type;
    }

    /**
     * Sets the type of the property.
     * 
     * @param type
     *            The type of the property.
     */
    public void setType(ComponentObjectType type) {
        this.type = type;
    }

}
