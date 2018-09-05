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

package com.vaadin.flow.internal.change;

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Base class for all node changes related to a feature.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class NodeFeatureChange extends NodeChange {

    private final Class<? extends NodeFeature> feature;

    /**
     * Creates a new change for the given feature.
     *
     * @param feature
     *            the feature affected by the change
     */
    public NodeFeatureChange(NodeFeature feature) {
        super(feature.getNode());

        this.feature = feature.getClass();
    }

    /**
     * Gets the feature affected by the change.
     *
     * @return the feature
     */
    public Class<? extends NodeFeature> getFeature() {
        return feature;
    }

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        json.put(JsonConstants.CHANGE_FEATURE,
                Json.create(NodeFeatureRegistry.getId(feature)));
    }
}
