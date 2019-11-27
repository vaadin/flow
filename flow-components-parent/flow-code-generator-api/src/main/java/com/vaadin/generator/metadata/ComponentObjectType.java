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
package com.vaadin.generator.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a webcomponent's property / method parameter of type object.
 * <p>
 * An example output from Analyzer for the type is:
 * <code>{inputElement: (Element|undefined), value: (string|undefined), invalid: boolean}
 * </code>
 *
 * @see ComponentPropertyBaseData
 * @see ComponentFunctionParameterData
 * @since 1.0
 */
public class ComponentObjectType implements ComponentType {

    /**
     * Holds the list of actual types in a ComponentObjectType.
     */
    public static class ComponentObjectTypeInnerType {

        private String name;
        private List<ComponentBasicType> type = new ArrayList<>();
        private boolean optional;

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
        public List<ComponentBasicType> getType() {
            return type;
        }

        /**
         * Sets the type of the property.
         *
         * @param type
         *            The type of the property.
         */
        public void setType(List<ComponentBasicType> type) {
            this.type = type;
        }

        /**
         * Returns whether the this property is optional or not.
         *
         * @return {@code true} if optional and doesn't have to be defined,
         *         {@code false} if this property should be defined always
         */
        public boolean isOptional() {
            return optional;
        }

        /**
         * Set this property as optional or not.
         *
         * @param optional
         *            whether the property optional or not
         */
        public void setOptional(boolean optional) {
            this.optional = optional;
        }
    }

    private List<ComponentObjectTypeInnerType> innerTypes = new ArrayList<>();

    /**
     * Get the list of actual types contained in this ComponentObjectType.
     *
     * @see ComponentObjectTypeInnerType
     *
     * @return the list of inner types within this ComponentObjectType
     */
    public List<ComponentObjectTypeInnerType> getInnerTypes() {
        return innerTypes;
    }

    /**
     * Sets the list of actual types contained in this ComponentObjectType.
     *
     * @see ComponentObjectTypeInnerType
     *
     * @param innerTypes
     *            the list of inner types to set
     */
    public void setInnerTypes(List<ComponentObjectTypeInnerType> innerTypes) {
        this.innerTypes = innerTypes;
    }
}
