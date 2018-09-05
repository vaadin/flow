/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.orderedlayout;

import javax.annotation.Generated;

/**
 * Set of theme variants applicable for {@code vaadin-vertical-layout}
 * component.
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.1-SNAPSHOT",
        "WebComponent: Vaadin.VerticalLayoutElement#1.1.0-alpha3",
        "Flow#1.1-SNAPSHOT" })
public enum VerticalLayoutVariant {
    LUMO_SPACING_XS("spacing-xs"), LUMO_SPACING_S("spacing-s"), LUMO_SPACING(
            "spacing"), LUMO_SPACING_L("spacing-l"), LUMO_SPACING_XL(
                    "spacing-xl"), MATERIAL_SPACING_XS(
                            "spacing-xs"), MATERIAL_SPACING_S(
                                    "spacing-s"), MATERIAL_SPACING(
                                            "spacing"), MATERIAL_SPACING_L(
                                                    "spacing-l"), MATERIAL_SPACING_XL(
                                                            "spacing-xl");

    private final String variant;

    VerticalLayoutVariant(String variant) {
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