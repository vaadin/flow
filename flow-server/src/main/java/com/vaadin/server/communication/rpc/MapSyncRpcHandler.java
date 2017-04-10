/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server.communication.rpc;

import java.io.Serializable;

import com.vaadin.flow.JsonCodec;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.nodefeature.NodeFeature;
import com.vaadin.flow.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.nodefeature.NodeMap;
import com.vaadin.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Model map synchronization RPC handler.
 *
 * @see JsonConstants#RPC_TYPE_MAP_SYNC
 *
 * @author Vaadin Ltd
 *
 */
public class MapSyncRpcHandler extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_MAP_SYNC;
    }

    @Override
    protected void handleNode(StateNode node, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_FEATURE);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY_VALUE);

        int featureId = (int) invocationJson
                .getNumber(JsonConstants.RPC_FEATURE);
        Class<? extends NodeFeature> feature = NodeFeatureRegistry
                .getFeature(featureId);
        assert NodeMap.class.isAssignableFrom(feature);

        String property = invocationJson.getString(JsonConstants.RPC_PROPERTY);
        Serializable value = JsonCodec.decodeWithoutTypeInfo(
                invocationJson.get(JsonConstants.RPC_PROPERTY_VALUE));

        ((NodeMap) node.getFeature(feature)).updateFromClient(property, value);
    }

}
