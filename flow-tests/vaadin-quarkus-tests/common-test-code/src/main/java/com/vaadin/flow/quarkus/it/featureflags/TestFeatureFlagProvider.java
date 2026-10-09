/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.featureflags;

import java.util.List;

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlagProvider;

/**
 * Declares a feature flag of the test application, registered in
 * {@code META-INF/services} the way an add-on declares its own flags.
 */
public class TestFeatureFlagProvider implements FeatureFlagProvider {

    public static final String FEATURE_ID = "quarkusTestFeature";

    @Override
    public List<Feature> getFeatures() {
        return List.of(new Feature("Quarkus test feature", FEATURE_ID, null,
                false, null));
    }
}
