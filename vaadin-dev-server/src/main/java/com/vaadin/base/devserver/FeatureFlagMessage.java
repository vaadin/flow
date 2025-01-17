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
package com.vaadin.base.devserver;

import java.io.Serializable;
import java.util.List;

import com.vaadin.experimental.Feature;

/**
 * Message with feature flags sent to the debug window.
 */
public class FeatureFlagMessage implements Serializable {
    private List<Feature> features;

    /**
     * Creates a new message with the given features.
     *
     * @param features
     *            the features
     */
    public FeatureFlagMessage(List<Feature> features) {
        this.features = features;
    }

    public List<Feature> getFeatures() {
        return features;
    }

}
