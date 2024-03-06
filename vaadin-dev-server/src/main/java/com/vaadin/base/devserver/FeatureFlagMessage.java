/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
