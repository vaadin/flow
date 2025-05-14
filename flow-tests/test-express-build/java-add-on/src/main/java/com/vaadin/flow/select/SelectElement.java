/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.select;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

/**
 * Native select element for selecting items.
 */
@Tag("select")
public class SelectElement
        extends AbstractSinglePropertyField<SelectElement, String> {

    public static final String VALUE_PROPERTY = "value";

    /**
     * Init select element with the selections given.
     *
     * @param options
     *            select options to populate select with
     */
    public SelectElement(String... options) {
        super(VALUE_PROPERTY, "", false);
        if (options.length == 0) {
            throw new IllegalArgumentException(
                    "Select should be given at least one option");
        }

        for (String selection : options) {
            Element option = ElementFactory.createOption(selection);
            getElement().appendChild(option);
        }
        setValue(options[0]);
        getElement().setProperty("selectedIndex", 0);
        getElement().addEventListener("change", e -> {
        }).synchronizeProperty(VALUE_PROPERTY);
    }

}
