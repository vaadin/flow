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

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

/**
 * Shows whether the feature flag that {@link TestFeatureFlagProvider} declares
 * and vaadin-featureflags.properties enables is enabled at runtime.
 */
@Route("feature-flag")
public class FeatureFlagView extends Span {

    public static final String FEATURE_ID = "feature";

    public FeatureFlagView() {
        setId(FEATURE_ID);
        setText("Feature flag " + TestFeatureFlagProvider.FEATURE_ID
                + " enabled: "
                + FeatureFlags.get(VaadinService.getCurrent().getContext())
                        .isEnabled(TestFeatureFlagProvider.FEATURE_ID));
    }
}
