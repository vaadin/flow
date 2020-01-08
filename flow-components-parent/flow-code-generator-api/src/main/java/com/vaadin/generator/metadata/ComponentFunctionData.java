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

import java.util.List;

/**
 * Represents a exposed function of the webcomponent, that can be called on the
 * server-side by the corresponding Java class.
 *
 * @see ComponentMetadata
 * @since 1.0
 */
public class ComponentFunctionData {

    private String name;
    private ComponentBasicType returns;
    private String description;
    private List<ComponentFunctionParameterData> parameters;

    /**
     * Gets the name of the function on the webcomponent.
     *
     * @return The name of the function.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the function on the webcomponent.
     *
     * @param name
     *            The name of the function.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the return type of the function.
     *
     * @return The type of the return, or <code>null</code> if void.
     */
    public ComponentBasicType getReturns() {
        return returns;
    }

    /**
     * Sets the return type of the function.
     *
     * @param returns
     *            The type of the return, or <code>null</code> if void.
     */
    public void setReturns(ComponentBasicType returns) {
        this.returns = returns;
    }

    /**
     * Gets the public description of the function, that can be used to generate
     * the corresponding Javadoc at the Java class.
     *
     * @return The function-level description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the public description of the function, that can be used to generate
     * the corresponding Javadoc at the Java class.
     *
     * @param description
     *            The function-level description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the list of parameters of the function. Each parameter is
     * represented by the {@link ComponentFunctionParameterData} object.
     *
     * @return The list of parameters of the function.
     */
    public List<ComponentFunctionParameterData> getParameters() {
        return parameters;
    }

    /**
     * Sets the list of parameters of the function. Each parameter is
     * represented by the {@link ComponentFunctionParameterData} object.
     *
     * @param parameters
     *            The list of parameters of the function.
     */
    public void setParameters(List<ComponentFunctionParameterData> parameters) {
        this.parameters = parameters;
    }

}
