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
package com.vaadin.flow.component.tabs;

/**
 * Set of variants applicable for 'Tabs' component
 */
public enum TabsVariants {
    LUMO_SMALL("small"), LUMO_MINIMAL("minimal"), LUMO_HIDE_SCROLL_BUTTONS(
            "hide-scroll-buttons"), LUMO_EQUAL_WIDTH_TABS("equal-width-tabs");

    private final String variant;

    TabsVariants(java.lang.String variant) {
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