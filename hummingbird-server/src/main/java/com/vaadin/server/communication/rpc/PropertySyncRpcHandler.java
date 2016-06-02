/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ElementPropertyMap;
import com.vaadin.hummingbird.processor.annotations.ServiceProvider;
import com.vaadin.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Property synchronization RPC handler.
 * 
 * @see JsonConstants#RPC_TYPE_PROPERTY_SYNC
 * 
 * @author Vaadin Ltd
 *
 */
@ServiceProvider(RpcInvocationHandler.class)
public class PropertySyncRpcHandler extends AbstractRpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_PROPERTY_SYNC;
    }

    @Override
    protected void handleNode(StateNode node, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY_VALUE);

        String property = invocationJson.getString(JsonConstants.RPC_PROPERTY);
        Serializable value = JsonCodec.decodeWithoutTypeInfo(
                invocationJson.get(JsonConstants.RPC_PROPERTY_VALUE));
        node.getFeature(ElementPropertyMap.class).setProperty(property, value,
                false);
    }

}
