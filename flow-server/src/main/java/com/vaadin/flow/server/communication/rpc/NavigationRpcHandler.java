/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.rpc;

import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.component.page.History.HistoryStateChangeHandler;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

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
    public Optional<Runnable> handle(UI ui, JsonObject invocationJson) {
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
                    ? NavigationTrigger.ROUTER_LINK
                    : NavigationTrigger.HISTORY;
            HistoryStateChangeEvent event = new HistoryStateChangeEvent(history,
                    state, new Location(location), trigger);
            historyStateChangeHandler.onHistoryStateChange(event);
        }

        return Optional.empty();
    }

}
