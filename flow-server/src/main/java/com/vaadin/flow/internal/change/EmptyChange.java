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
import com.vaadin.flow.internal.nodefeature.NodeList;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Empty change for the feature to report its presence for the client (send the
 * feature information even though its data is empty).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class EmptyChange extends NodeFeatureChange {

    /**
     * Creates a new empty change.
     *
     * @param feature
     *            the feature to populate on the client
     */
    public EmptyChange(NodeFeature feature) {
        super(feature);
    }

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_NOOP);
        if (NodeList.class.isAssignableFrom(getFeature())) {
            json.put(JsonConstants.CHANGE_FEATURE_TYPE, true);
        } else {
            json.put(JsonConstants.CHANGE_FEATURE_TYPE, false);
        }
        super.populateJson(json, constantPool);
    }

}
