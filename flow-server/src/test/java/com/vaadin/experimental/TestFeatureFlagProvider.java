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
 * Provides test-only feature flags.
 * <p>
 * This provider is only available during test execution and will not be
 * included in production builds.
 *
 * @since 25.0
 */
public class TestFeatureFlagProvider implements FeatureFlagProvider {

    public static final Feature EXAMPLE = new Feature(
            "Example feature. Internally used for testing purposes. Does not have any effect on production applications.",
            "exampleFeatureFlag", "https://github.com/vaadin/flow/pull/12004",
            false,
            "com.vaadin.flow.server.frontend.NodeTestComponents$ExampleExperimentalComponent");

    @Override
    public List<Feature> getFeatures() {
        return List.of(EXAMPLE);
    }

}
