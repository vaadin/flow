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
package com.vaadin.experimental;

import java.util.List;

/**
 * Provides feature flags for Flow Components.
 *
 * @since 25.0
 */
public class FlowComponentsFeatureFlagProvider implements FeatureFlagProvider {

    public static final Feature MASTER_DETAIL_LAYOUT_COMPONENT = new Feature(
            "Master Detail Layout component", "masterDetailLayoutComponent",
            "https://github.com/vaadin/platform/issues/7173", true,
            "com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout");

    public static final Feature LAYOUT_COMPONENT_IMPROVEMENTS = new Feature(
            "HorizontalLayout and VerticalLayout improvements",
            "layoutComponentImprovements",
            "https://github.com/vaadin/flow-components/issues/6998", true,
            null);

    public static final Feature DEFAULT_AUTO_RESPONSIVE_FORM_LAYOUT = new Feature(
            "Form Layout auto-responsive mode enabled by default",
            "defaultAutoResponsiveFormLayout",
            "https://github.com/vaadin/platform/issues/7172", true, null);

    @Override
    public List<Feature> getFeatures() {
        return List.of(MASTER_DETAIL_LAYOUT_COMPONENT,
                LAYOUT_COMPONENT_IMPROVEMENTS,
                DEFAULT_AUTO_RESPONSIVE_FORM_LAYOUT);
    }
}
