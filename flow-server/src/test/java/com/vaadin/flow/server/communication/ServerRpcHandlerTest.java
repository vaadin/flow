/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.CompletableFuture;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.ServerRpcHandler.InvalidUIDLSecurityKeyException;
import com.vaadin.flow.shared.ApplicationConstants;

public class ServerRpcHandlerTest {
    private VaadinRequest request;
    private VaadinService service;
    private VaadinSession session;
    private UI ui;
    private UIInternals uiInternals;
    private DependencyList dependencyList;

    private StateTree uiTree;
    final private String csrfToken = "";

    private ServerRpcHandler serverRpcHandler;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        request = Mockito.mock(VaadinRequest.class);
        service = Mockito.mock(VaadinService.class);
        session = Mockito.mock(VaadinSession.class);
        ui = Mockito.mock(UI.class);
        uiInternals = Mockito.mock(UIInternals.class);
        dependencyList = Mockito.mock(DependencyList.class);

        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(session.getService()).thenReturn(service);

        Mockito.when(ui.getInternals()).thenReturn(uiInternals);
        Mockito.when(ui.getSession()).thenReturn(session);
        Mockito.when(ui.getCsrfToken()).thenReturn(csrfToken);

        DeploymentConfiguration deploymentConfiguration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);

        uiTree = new StateTree(uiInternals);
        Mockito.when(uiInternals.getStateTree()).thenReturn(uiTree);
        Mockito.when(uiInternals.getDependencyList())
                .thenReturn(dependencyList);

        serverRpcHandler = new ServerRpcHandler();
    }

    @Test
    public void handleRpc_resynchronize_throwsExceptionAndDirtiesTreeAndClearsDependenciesSent()
            throws IOException,
            ServerRpcHandler.InvalidUIDLSecurityKeyException {
        // given
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[], \"resynchronize\": true, \"clientId\":1}");
        uiTree.collectChanges(c -> { // clean tree
        });
        thrown.expect(
                ServerRpcHandler.ResynchronizationRequiredException.class);

        // when
        serverRpcHandler.handleRpc(ui, reader, request);

        // then there are dirty nodes
        Assert.assertTrue(uiTree.hasDirtyNodes());

        // the dependencies-sent cache was cleared
        Mockito.verify(dependencyList).clearPendingSendToClient();
    }

    @Test
    public void handleRpc_duplicateMessage_doNotThrow()
            throws InvalidUIDLSecurityKeyException, IOException {
        String msg = "{\"" + ApplicationConstants.CLIENT_TO_SERVER_ID + "\":1}";
        ServerRpcHandler handler = new ServerRpcHandler();

        ui = new UI();
        ui.getInternals().setSession(session);
        ui.getInternals().setLastProcessedClientToServerId(1,
                MessageDigestUtil.sha256(msg));

        // This invocation shouldn't throw. No other checks
        handler.handleRpc(ui, msg, request);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void handleRpc_unexpectedMessage_throw()
            throws InvalidUIDLSecurityKeyException, IOException {
        String msg = "{\"" + ApplicationConstants.CLIENT_TO_SERVER_ID + "\":1}";
        ServerRpcHandler handler = new ServerRpcHandler();

        ui = new UI();
        ui.getInternals().setSession(session);

        handler.handleRpc(ui, msg, request);
    }

    @Test
    public void handleRpc_pushEnabled_unloadBeacon_doNoGeneratePayload()
            throws InvalidUIDLSecurityKeyException, IOException {

        Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);

        Mockito.doAnswer(i -> {
            // Introduce a small delay to hold the lock during disconnect
            Thread.sleep(30);
            return null;
        }).when(resource).close();
        Mockito.doAnswer(i -> {
            // Introduce a small delay to hold the lock during message push
            Thread.sleep(30);
            return CompletableFuture.completedFuture(null);
        }).when(broadcaster).broadcast(ArgumentMatchers.any(),
                ArgumentMatchers.any(AtmosphereResource.class));

        AtmospherePushConnection pushConnection = new AtmospherePushConnection(
                ui);
        pushConnection.connect(resource);
        Mockito.when(uiInternals.getPushConnection())
                .thenReturn(pushConnection);

        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"UNLOAD\": true, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();

        Assert.assertEquals("Push should be connected",
                AtmospherePushConnection.State.CONNECTED,
                pushConnection.getState());

        handler.handleRpc(ui, reader, request);

        Assert.assertEquals("Push should be disconnected after beacon unload",
                AtmospherePushConnection.State.DISCONNECTED,
                pushConnection.getState());
        Mockito.verifyNoInteractions(broadcaster);
    }
}
