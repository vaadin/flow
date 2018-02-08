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
 * Mixin interface for fields with {@code autocomplete} attribute.
 */
public interface HasCompletion extends HasElement {

    /**
     * Name of @{code autocomplete} attribute.
     */
    String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";

    /**
     * Sets the {@link Completion} option of the field.
     *
     * @param completion
     *            the completion option
     */
    default void setAutocomplete(Completion completion) {
        getElement().setAttribute(AUTOCOMPLETE_ATTRIBUTE, completion.value);
    }

    /**
     * Gets the {@link Completion} option of the field.
     *
     * @return the completion option
     */
    default Completion getAutocomplete() {
        String autocomplete = getElement().getAttribute(AUTOCOMPLETE_ATTRIBUTE);
        if (autocomplete == null) {
            // Not set, may inherit behavior from parent form.
            return null;
        } else if ("".equals(autocomplete)) {
            // Default behavior for empty attribute.
            return Completion.OFF;
        } else {
            return Completion
                    .valueOf(autocomplete.toUpperCase().replaceAll("-", "_"));
        }
    }
}
