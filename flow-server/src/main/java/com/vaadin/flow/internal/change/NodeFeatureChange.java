/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
