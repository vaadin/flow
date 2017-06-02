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

/**
 * Class that represents a property exposed by the webcomponent.
 * 
 * @see ComponentMetadata
 */
public class ComponentProperty extends ComponentPropertyBase {

    private String documentation;
    private boolean readOnly;

    /**
     * Gets the public documentation of the property, that can be used to
     * generate the corresponding Javadoc at the Java class.
     * 
     * @return The property-level documentation.
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * Sets the public documentation of the property, that can be used to
     * generate the corresponding Javadoc at the Java class.
     * 
     * @param documentation
     *            The property-level documentation.
     */
    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    /**
     * Gets whether the property is read-only or not. Read-only properties
     * doesn't have a corresponding "setter" at the generated Java class.
     * 
     * @return the readOnly <code>true</code> if the property is read-only (and
     *         doesn't contain a setter), <code>false</code> otherwise.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets whether the property is read-only or not. Read-only properties
     * doesn't have a corresponding "setter" at the generated Java class.
     * 
     * @param readOnly
     *            <code>true</code> if the property is read-only (and doesn't
     *            contain a setter), <code>false</code> otherwise.
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

}
