/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceEventBus;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.communication.ServerRpcHandler.InvalidUIDLSecurityKeyException;
import com.vaadin.flow.server.dau.DAUUtils;
import com.vaadin.flow.server.dau.DauEnforcementException;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.pro.licensechecker.dau.EnforcementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ServerRpcHandlerTest {
    private VaadinRequest request;
    private VaadinService service;
    private VaadinSession session;
    private WrappedSession wrappedSession;
    private UI ui;
    private UIInternals uiInternals;
    private DependencyList dependencyList;

    private StateTree uiTree;
    final private String csrfToken = "";

    private ServerRpcHandler serverRpcHandler;
    private DeploymentConfiguration deploymentConfiguration;

    @BeforeEach
    void setup() {
        request = Mockito.mock(VaadinRequest.class);
        service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getEventBus())
                .thenReturn(new VaadinServiceEventBus(service));
        session = Mockito.mock(VaadinSession.class);
        wrappedSession = Mockito.mock(WrappedSession.class);
        ui = Mockito.mock(UI.class);
        uiInternals = Mockito.mock(UIInternals.class);
        dependencyList = Mockito.mock(DependencyList.class);

        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(request.getWrappedSession()).thenReturn(wrappedSession);
        Mockito.when(session.getService()).thenReturn(service);

        Mockito.when(ui.getInternals()).thenReturn(uiInternals);
        Mockito.when(ui.getSession()).thenReturn(session);
        Mockito.when(ui.getCsrfToken()).thenReturn(csrfToken);

        deploymentConfiguration = Mockito.mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);
        Mockito.when(deploymentConfiguration.getMaxRequestBodySize())
                .thenReturn(-1L);

        uiTree = new StateTree(uiInternals);
        Mockito.when(uiInternals.getStateTree()).thenReturn(uiTree);
        Mockito.when(uiInternals.getDependencyList())
                .thenReturn(dependencyList);

        serverRpcHandler = new ServerRpcHandler();
    }

    @Test
    void handleRpc_resynchronize_throwsExceptionAndDirtiesTreeAndClearsDependenciesSent()
            throws IOException,
            ServerRpcHandler.InvalidUIDLSecurityKeyException,
            ServerRpcHandler.MessageIdSyncException {
        // given
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[], \"resynchronize\": true, \"clientId\":1}");
        uiTree.collectChanges(c -> { // clean tree
        });

        assertThrows(ServerRpcHandler.ResynchronizationRequiredException.class,
                () -> serverRpcHandler.handleRpc(ui, reader, request));

        // then there are dirty nodes
        assertTrue(uiTree.hasDirtyNodes());

        // the dependencies-sent cache was cleared
        Mockito.verify(dependencyList).clearPendingSendToClient();
    }

    @Test
    void handleRpc_duplicateMessage_throwsResendPayload()
            throws InvalidUIDLSecurityKeyException,
            ServerRpcHandler.MessageIdSyncException {
        String msg = "{\"" + ApplicationConstants.CLIENT_TO_SERVER_ID + "\":1}";
        ServerRpcHandler handler = new ServerRpcHandler();

        ui = new UI();
        ui.getInternals().setSession(session);
        ui.getInternals().setLastProcessedClientToServerId(1,
                MessageDigestUtil.sha256(msg));

        assertThrows(ServerRpcHandler.ClientResentPayloadException.class,
                () -> handler.handleRpc(ui, msg, request));
    }

    @Test
    void handleRpc_unexpectedMessage_throw()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        String msg = "{\"" + ApplicationConstants.CLIENT_TO_SERVER_ID + "\":1}";
        ServerRpcHandler handler = new ServerRpcHandler();

        ui = new UI();
        ui.getInternals().setSession(session);

        assertThrows(ServerRpcHandler.MessageIdSyncException.class,
                () -> handler.handleRpc(ui, msg, request));
    }

    @Test
    void handleRpc_unexpectedMessage_exceptionContainsCorrectIds()
            throws InvalidUIDLSecurityKeyException, IOException {
        String msg = "{\"" + ApplicationConstants.CLIENT_TO_SERVER_ID + "\":5}";
        ServerRpcHandler handler = new ServerRpcHandler();

        ui = new UI();
        ui.getInternals().setSession(session);
        // Set the last processed ID to 0, so expected is 1
        ui.getInternals().setLastProcessedClientToServerId(0,
                MessageDigestUtil.sha256(""));

        var e = assertThrows(ServerRpcHandler.MessageIdSyncException.class,
                () -> handler.handleRpc(ui, msg, request));
        assertEquals(1, e.getExpectedId());
        assertEquals(5, e.getReceivedId());
        assertTrue(e.getMessage().contains("Expected: 1"));
        assertTrue(e.getMessage().contains("got: 5"));
    }

    @Test
    void handleRpc_dauEnforcement_throws()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"syncId\": 0, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        assertThrows(DauEnforcementException.class,
                () -> handler.handleRpc(ui, reader, request));
    }

    @Test
    void handleRpc_dauEnforcement_pollEvent_doNoThrow()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"ui-poll\" }], \"syncId\": 0, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        try {
            handler.handleRpc(ui, reader, request);
        } catch (DauEnforcementException e) {
            fail("UI Poll request should not be blocked");
        }
    }

    @Test
    void handleRpc_dauEnforcement_pollEventMixedWithOtherEvents_throw()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"ui-poll\" },{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"syncId\": 0, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        assertThrows(DauEnforcementException.class,
                () -> handler.handleRpc(ui, reader, request));
    }

    @Test
    void handleRpc_dauEnforcement_resynchronization_doNoThrow()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"resynchronize\": true, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        assertThrows(ServerRpcHandler.ResynchronizationRequiredException.class,
                () -> handler.handleRpc(ui, reader, request));
    }

    @Test
    void handleRpc_dauEnforcement_unloadBeacon_doNoThrow()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"UNLOAD\": true, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        try {
            handler.handleRpc(ui, reader, request);
        } catch (EnforcementException e) {
            fail("Unload beacon request should not be blocked");
        }
    }

    @Test
    void handleRpc_dauEnforcement_returnChannelMessage_doNoThrow()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"channel\", \"node\" : 1, \"channel\": 0 }], \"syncId\": 0, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        try {
            handler.handleRpc(ui, reader, request);
        } catch (EnforcementException e) {
            fail("UI Poll request should not be blocked");
        }
    }

    @Test
    void handleRpc_dauEnforcement_returnChannelMessageMixedWithOtherEvents_throw()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"channel\", \"node\" : 1, \"channel\": 0 },{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"syncId\": 0, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        assertThrows(DauEnforcementException.class,
                () -> handler.handleRpc(ui, reader, request));
    }

    @Test
    void handleRpc_firesRpcInvocationListener_withTypeNameAndNode()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        List<RpcInvocationStartedEvent> started = new ArrayList<>();
        List<RpcInvocationEndedEvent> ended = new ArrayList<>();
        service.getEventBus().addListener(RpcInvocationStartedEvent.class,
                started::add);
        service.getEventBus().addListener(RpcInvocationEndedEvent.class,
                ended::add);
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"syncId\": 0, \"clientId\":0}");
        ui = new UI();
        ui.getInternals().setSession(session);

        serverRpcHandler.handleRpc(ui, reader, request);

        assertEquals(1, started.size());
        // Ended is always fired (in a finally) so observers can close spans.
        assertEquals(1, ended.size());
        RpcInvocationStartedEvent event = started.get(0);
        assertEquals("event", event.getType());
        assertEquals("click", event.getName());
        assertEquals(1, event.getNodeId());
    }

    @Test
    void handleRpc_mapSync_firesRpcInvocationEvents_withSyncedPropertyAsName()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        // The events must bracket the property change event, so that a listener
        // timing the invocation measures the application code it runs.
        List<String> sequence = new ArrayList<>();
        List<RpcInvocationStartedEvent> started = new ArrayList<>();
        service.getEventBus().addListener(RpcInvocationStartedEvent.class,
                event -> {
                    started.add(event);
                    sequence.add("started");
                });
        service.getEventBus().addListener(RpcInvocationEndedEvent.class,
                event -> sequence.add("ended"));

        ui = new UI();
        ui.getInternals().setSession(session);
        Element input = ElementFactory.createInput();
        input.addPropertyChangeListener("value", "change",
                event -> sequence.add("changeEvent"));
        ui.getElement().appendChild(input);
        int nodeId = input.getNode().getId();

        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"mSync\", \"node\" : " + nodeId
                + ", \"feature\": 1, \"property\": \"value\", \"value\": \"typed\" }], \"syncId\": 0, \"clientId\":0}");

        serverRpcHandler.handleRpc(ui, reader, request);

        assertEquals(1, started.size());
        RpcInvocationStartedEvent event = started.get(0);
        assertEquals(JsonConstants.RPC_TYPE_MAP_SYNC, event.getType());
        assertEquals("value", event.getName());
        assertEquals(nodeId, event.getNodeId());
        assertEquals(List.of("started", "changeEvent", "ended"), sequence);
    }

    @Test
    void handleRpc_mapSyncListenerThrows_firesRpcInvocationFailed_withSyncedPropertyAsName()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        Mockito.when(session.getErrorHandler())
                .thenReturn(Mockito.mock(ErrorHandler.class));
        List<String> sequence = new ArrayList<>();
        List<RpcInvocationFailedEvent> failed = new ArrayList<>();
        service.getEventBus().addListener(RpcInvocationStartedEvent.class,
                event -> sequence.add("started"));
        service.getEventBus().addListener(RpcInvocationFailedEvent.class,
                event -> {
                    failed.add(event);
                    sequence.add("failed");
                });
        service.getEventBus().addListener(RpcInvocationEndedEvent.class,
                event -> sequence.add("ended"));

        ui = new UI();
        ui.getInternals().setSession(session);
        RuntimeException failure = new RuntimeException("in value change");
        Element input = ElementFactory.createInput();
        input.addPropertyChangeListener("value", "change", event -> {
            throw failure;
        });
        ui.getElement().appendChild(input);
        int nodeId = input.getNode().getId();

        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"mSync\", \"node\" : " + nodeId
                + ", \"feature\": 1, \"property\": \"value\", \"value\": \"typed\" }], \"syncId\": 0, \"clientId\":0}");

        serverRpcHandler.handleRpc(ui, reader, request);

        assertEquals(List.of("started", "failed", "ended"), sequence);
        assertEquals("value", failed.get(0).getName());
        assertSame(failure, failed.get(0).getError());
    }

    @Test
    void handleRpc_invocationsForMissingNode_reportsEventButNotPropertySync()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        List<RpcInvocationStartedEvent> started = new ArrayList<>();
        service.getEventBus().addListener(RpcInvocationStartedEvent.class,
                started::add);
        ui = new UI();
        ui.getInternals().setSession(session);

        // Node 99 does not exist, so both invocations are discarded unhandled.
        // The event is still reported because it is observed around the routing
        // to the handler, while the property sync is observed around a change
        // event that is never produced.
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 99, \"event\": \"click\" },"
                + "{\"type\": \"mSync\", \"node\" : 99, \"feature\": 1, \"property\": \"value\", \"value\": \"typed\" }],"
                + " \"syncId\": 0, \"clientId\":0}");

        serverRpcHandler.handleRpc(ui, reader, request);

        assertEquals(1, started.size());
        assertEquals(JsonConstants.RPC_TYPE_EVENT, started.get(0).getType());
    }

    @Test
    void handleRpc_onlyOnePhaseObserved_stillFiresThatPhase()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        // Observing a single phase is enough to be notified of it: a listener
        // that only times completions never registers for the started event.
        List<RpcInvocationEndedEvent> ended = new ArrayList<>();
        service.getEventBus().addListener(RpcInvocationEndedEvent.class,
                ended::add);
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"syncId\": 0, \"clientId\":0}");
        ui = new UI();
        ui.getInternals().setSession(session);

        serverRpcHandler.handleRpc(ui, reader, request);

        assertEquals(1, ended.size());
        assertEquals("click", ended.get(0).getName());
    }

    @Test
    void handleRpc_onlyFailedPhaseObserved_stillFiresThatPhase()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        // Counting failures does not require observing the other two phases.
        Mockito.when(session.getErrorHandler())
                .thenReturn(Mockito.mock(ErrorHandler.class));
        List<RpcInvocationFailedEvent> failed = new ArrayList<>();
        service.getEventBus().addListener(RpcInvocationFailedEvent.class,
                failed::add);

        ui = new UI();
        ui.getInternals().setSession(session);
        Element input = ElementFactory.createInput();
        input.addPropertyChangeListener("value", "change", event -> {
            throw new RuntimeException("in value change");
        });
        ui.getElement().appendChild(input);
        int nodeId = input.getNode().getId();

        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"mSync\", \"node\" : " + nodeId
                + ", \"feature\": 1, \"property\": \"value\", \"value\": \"typed\" }], \"syncId\": 0, \"clientId\":0}");

        serverRpcHandler.handleRpc(ui, reader, request);

        assertEquals(1, failed.size());
        assertEquals("value", failed.get(0).getName());
    }

    @Test
    void handleRpc_mapSyncFailsUnobserved_stillReachesErrorHandler()
            throws InvalidUIDLSecurityKeyException, IOException,
            ServerRpcHandler.MessageIdSyncException {
        // Nothing observes the invocation, which is the default: wrapping the
        // handling in the phase events must not change where a failure goes.
        ErrorHandler errorHandler = Mockito.mock(ErrorHandler.class);
        Mockito.when(session.getErrorHandler()).thenReturn(errorHandler);

        ui = new UI();
        ui.getInternals().setSession(session);
        RuntimeException failure = new RuntimeException("in value change");
        Element input = ElementFactory.createInput();
        input.addPropertyChangeListener("value", "change", event -> {
            throw failure;
        });
        ui.getElement().appendChild(input);
        int nodeId = input.getNode().getId();

        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"mSync\", \"node\" : " + nodeId
                + ", \"feature\": 1, \"property\": \"value\", \"value\": \"typed\" }], \"syncId\": 0, \"clientId\":0}");

        serverRpcHandler.handleRpc(ui, reader, request);

        ArgumentCaptor<ErrorEvent> captor = ArgumentCaptor
                .forClass(ErrorEvent.class);
        Mockito.verify(errorHandler).error(captor.capture());
        assertSame(failure, captor.getValue().getThrowable());
    }

    @Test
    void handleRpc_unsupportedInvocationType_throws() {
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"notAnRpcType\", \"node\" : 1 }], \"syncId\": 0, \"clientId\":0}");
        ui = new UI();
        ui.getInternals().setSession(session);

        assertThrows(IllegalArgumentException.class,
                () -> serverRpcHandler.handleRpc(ui, reader, request));
    }

    private void enableDau() {
        Mockito.when(deploymentConfiguration.isProductionMode())
                .thenReturn(true);
        Mockito.when(deploymentConfiguration.getBooleanProperty(
                ArgumentMatchers.eq(Constants.DAU_TOKEN),
                ArgumentMatchers.anyBoolean())).thenReturn(true);
    }
}
