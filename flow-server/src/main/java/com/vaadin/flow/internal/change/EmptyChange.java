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
import com.vaadin.flow.internal.nodefeature.NodeList;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Empty change for the feature to report its presence for the client (send the
 * feature information even though its data is empty).
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
