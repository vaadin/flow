package com.vaadin.flow.server.communication.rpc;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.JsonConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import elemental.json.Json;
import elemental.json.JsonObject;

public class NavigationRpcHandlerTest {

    private UI ui;
    private History.HistoryStateChangeHandler historyStateChangeHandler;
    private NavigationRpcHandler rpcHandler;
    private JsonObject invocation;

    @Before
    public void setup() {
        ui = new UI();
        historyStateChangeHandler = Mockito
                .mock(History.HistoryStateChangeHandler.class);
        ui.getPage().getHistory()
                .setHistoryStateChangeHandler(historyStateChangeHandler);

        ui.add(new RouterLink());

        rpcHandler = new NavigationRpcHandler();
        invocation = Json.createObject();
        invocation.put(JsonConstants.RPC_NAVIGATION_LOCATION, "foo");
    }

    @Test
    public void handleRouterLinkClick_navigationTriggered() {
        invocation.put(JsonConstants.RPC_NAVIGATION_ROUTERLINK, true);
        rpcHandler.handle(ui, invocation);

        Mockito.verify(historyStateChangeHandler, Mockito.times(1))
                .onHistoryStateChange(
                        Mockito.any(History.HistoryStateChangeEvent.class));
    }

    @Test
    public void handleRouterLinkClick_uiIsInert_navigationTriggered() {
        ui.addModal(new RouterLink());
        ui.getInternals().getStateTree().collectChanges(nodeChange -> {
        });

        invocation.put(JsonConstants.RPC_NAVIGATION_ROUTERLINK, true);
        rpcHandler.handle(ui, invocation);

        Mockito.verify(historyStateChangeHandler, Mockito.times(1))
                .onHistoryStateChange(
                        Mockito.any(History.HistoryStateChangeEvent.class));
    }

    @Test
    public void handleHistoryChange_uiIsInert_navigationTriggered() {
        ui.addModal(new RouterLink());
        rpcHandler.handle(ui, invocation);

        Mockito.verify(historyStateChangeHandler, Mockito.times(1))
                .onHistoryStateChange(
                        Mockito.any(History.HistoryStateChangeEvent.class));
    }
}
