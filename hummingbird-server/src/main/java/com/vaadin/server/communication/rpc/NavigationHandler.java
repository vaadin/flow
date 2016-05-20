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

import com.vaadin.shared.JsonConstants;
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
public class NavigationHandler extends AbstractInvocationHandler {

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

            HistoryStateChangeEvent event = new HistoryStateChangeEvent(history,
                    state, location);
            historyStateChangeHandler.onHistoryStateChange(event);
        }
    }

}
