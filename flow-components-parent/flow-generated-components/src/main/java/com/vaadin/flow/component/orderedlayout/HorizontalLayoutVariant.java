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
package com.vaadin.flow.component.orderedlayout;

/**
 * Set of theme variants applicable for {@code vaadin-horizontal-layout}
 * component.
 */
public enum HorizontalLayoutVariant {
    LUMO_SPACING_XS("spacing-xs"), LUMO_SPACING_S("spacing-s"), LUMO_SPACING(
            "spacing"), LUMO_SPACING_L(
                    "spacing-l"), LUMO_SPACING_XL("spacing-xl");

    private final String variant;

    HorizontalLayoutVariant(String variant) {
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