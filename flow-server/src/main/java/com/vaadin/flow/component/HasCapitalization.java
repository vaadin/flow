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
package com.vaadin.flow.component;

/**
 * Mixin interface for fields with {@code autocapitalize} attribute.
 */
public interface HasCapitalization extends HasElement {

    /**
     * Name of @{code autocapitalize} attribute.
     */
    String AUTOCAPITALIZE_ATTRIBUTE = "autocapitalize";

    /**
     * Sets the {@link Capitalization} option of the field.
     *
     * @param capitalization
     *            the capitalization option
     */
    default void setAutocapitalize(Capitalization capitalization) {
        getElement().setAttribute(AUTOCAPITALIZE_ATTRIBUTE,
                capitalization.value);
    }

    /**
     * Gets the {@link Capitalization} option of the field.
     *
     * @return the capitalization option
     */
    default Capitalization getAutocapitalize() {
        String autocapitalize = getElement()
                .getAttribute(AUTOCAPITALIZE_ATTRIBUTE);
        if (autocapitalize == null) {
            // Not set, may inherit behavior from parent form.
            return null;
        } else if ("".equals(autocapitalize)) {
            // Default behavior for empty attribute.
            return Capitalization.SENTENCES;
        } else {
            return Capitalization.valueOf(autocapitalize.toUpperCase());
        }
    }
}
