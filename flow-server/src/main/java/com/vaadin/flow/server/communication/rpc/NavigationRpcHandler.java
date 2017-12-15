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
package com.vaadin.flow.server.communication.rpc;

import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.router.Location;
import com.vaadin.router.NavigationTrigger;
import com.vaadin.ui.History;
import com.vaadin.ui.History.HistoryStateChangeEvent;
import com.vaadin.ui.History.HistoryStateChangeHandler;
import com.vaadin.ui.UI;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * RPC handler for Navigation.
 * 
 * @see JsonConstants#RPC_TYPE_NAVIGATION
 * @author Vaadin Ltd
 *
 */
public class NavigationRpcHandler implements RpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_NAVIGATION;
    }

    @Override
    public void handle(UI ui, JsonObject invocationJson) {
        History history = ui.getPage().getHistory();

        HistoryStateChangeHandler historyStateChangeHandler = history
                .getHistoryStateChangeHandler();
        if (historyStateChangeHandler != null) {
            JsonValue state = invocationJson
                    .get(JsonConstants.RPC_NAVIGATION_STATE);
            String location = invocationJson
                    .getString(JsonConstants.RPC_NAVIGATION_LOCATION);

            boolean triggeredByLink = invocationJson
                    .hasKey(JsonConstants.RPC_NAVIGATION_ROUTERLINK);
            NavigationTrigger trigger = triggeredByLink
                    ? NavigationTrigger.ROUTER_LINK : NavigationTrigger.HISTORY;

            HistoryStateChangeEvent event = new HistoryStateChangeEvent(history,
                    state, new Location(location), trigger);
            historyStateChangeHandler.onHistoryStateChange(event);
        }
    }

}
