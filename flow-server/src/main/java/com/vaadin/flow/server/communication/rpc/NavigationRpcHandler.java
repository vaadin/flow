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
package com.vaadin.flow.server.communication.rpc;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.component.page.History.HistoryStateChangeHandler;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;

/**
 * RPC handler for Navigation.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see JsonConstants#RPC_TYPE_NAVIGATION
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class NavigationRpcHandler implements RpcInvocationHandler {

    @Override
    public String getRpcType() {
        return JsonConstants.RPC_TYPE_NAVIGATION;
    }

    @Override
    public Optional<Runnable> handle(UI ui, JsonNode invocationJson) {
        History history = ui.getPage().getHistory();

        HistoryStateChangeHandler historyStateChangeHandler = history
                .getHistoryStateChangeHandler();
        if (historyStateChangeHandler != null) {
            JsonNode state = invocationJson
                    .get(JsonConstants.RPC_NAVIGATION_STATE);
            String location = invocationJson
                    .get(JsonConstants.RPC_NAVIGATION_LOCATION).textValue();
            boolean triggeredByLink = invocationJson
                    .has(JsonConstants.RPC_NAVIGATION_ROUTERLINK);
            NavigationTrigger trigger = triggeredByLink
                    ? NavigationTrigger.ROUTER_LINK
                    : NavigationTrigger.HISTORY;
            // TODO: remove Json when history updated
            HistoryStateChangeEvent event = new HistoryStateChangeEvent(history,
                    state == null ? null : Json.create(state.toString()),
                    new Location(location), trigger);
            historyStateChangeHandler.onHistoryStateChange(event);
        }

        return Optional.empty();
    }

}
