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
package com.vaadin.flow.component.radiobutton;

import javax.annotation.Generated;

/**
 * Set of theme variants applicable for {@code vaadin-radio-group} component.
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.RadioGroupElement#1.1.2", "Flow#1.2-SNAPSHOT" })
public enum RadioGroupVariant {
    LUMO_VERTICAL("vertical"), MATERIAL_VERTICAL("vertical");

    private final String variant;

    RadioGroupVariant(String variant) {
        this.variant = variant;
    }

    /**
     * Gets the variant name.
     * 
     * @return variant name
     */
    public String getVariantName() {
        return variant;
    }
}