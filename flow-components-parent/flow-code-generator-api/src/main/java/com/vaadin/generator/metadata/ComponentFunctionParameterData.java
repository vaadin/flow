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

/**
 * Represents a parameter of a exposed function of the webcomponent.
 *
 * @see ComponentMetadata
 * @since 1.0
 */
public class ComponentFunctionParameterData extends ComponentPropertyBaseData {

    private boolean optional;

    /**
     * Gets whether the parameter is optional or not when calling the function.
     *
     * @return <code>true</code> if the parameter is optional,
     *         <code>false</code> otherwise.
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Sets whether the parameter is optional or not when calling the function.
     *
     * @param optional
     *            <code>true</code> if the parameter is optional,
     *            <code>false</code> otherwise.
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

}
