package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.StringReader;

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
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.communication.ServerRpcHandler.InvalidUIDLSecurityKeyException;
import com.vaadin.flow.server.dau.DAUUtils;
import com.vaadin.flow.server.dau.DauEnforcementException;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.pro.licensechecker.dau.EnforcementException;

public class ServerRpcHandlerTest {
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private DeploymentConfiguration deploymentConfiguration;

    @Before
    public void setup() {
        request = Mockito.mock(VaadinRequest.class);
        service = Mockito.mock(VaadinService.class);
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

    @Test(expected = ServerRpcHandler.ClientResentPayloadException.class)
    public void handleRpc_duplicateMessage_throwsResendPayload()
            throws InvalidUIDLSecurityKeyException {
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

    @Test(expected = DauEnforcementException.class)
    public void handleRpc_dauEnforcement_throws()
            throws InvalidUIDLSecurityKeyException, IOException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"syncId\": 0, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        handler.handleRpc(ui, reader, request);
    }

    @Test
    public void handleRpc_dauEnforcement_pollEvent_doNoThrow()
            throws InvalidUIDLSecurityKeyException, IOException {
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
            Assert.fail("UI Poll request should not be blocked");
        }
    }

    @Test(expected = DauEnforcementException.class)
    public void handleRpc_dauEnforcement_pollEventMixedWithOtherEvents_throw()
            throws InvalidUIDLSecurityKeyException, IOException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"ui-poll\" },{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"syncId\": 0, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        handler.handleRpc(ui, reader, request);
    }

    @Test(expected = ServerRpcHandler.ResynchronizationRequiredException.class)
    public void handleRpc_dauEnforcement_resynchronization_doNoThrow()
            throws InvalidUIDLSecurityKeyException, IOException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"resynchronize\": true, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        try {
            handler.handleRpc(ui, reader, request);
        } catch (EnforcementException e) {
            Assert.fail("UI Poll request should not be blocked");
        }
    }

    @Test
    public void handleRpc_dauEnforcement_unloadBeacon_doNoThrow()
            throws InvalidUIDLSecurityKeyException, IOException {
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
            Assert.fail("Unload beacon request should not be blocked");
        }
    }

    @Test
    public void handleRpc_dauEnforcement_returnChannelMessage_doNoThrow()
            throws InvalidUIDLSecurityKeyException, IOException {
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
            Assert.fail("UI Poll request should not be blocked");
        }
    }

    @Test(expected = DauEnforcementException.class)
    public void handleRpc_dauEnforcement_returnChannelMessageMixedWithOtherEvents_throw()
            throws InvalidUIDLSecurityKeyException, IOException {
        enableDau();
        StringReader reader = new StringReader("{\"csrfToken\": \"" + csrfToken
                + "\", \"rpc\":[{\"type\": \"channel\", \"node\" : 1, \"channel\": 0 },{\"type\": \"event\", \"node\" : 1, \"event\": \"click\" }], \"syncId\": 0, \"clientId\":0}");
        ServerRpcHandler handler = new ServerRpcHandler();
        Mockito.when(request.getAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY))
                .thenReturn(new EnforcementException("Block"));

        ui = new UI();
        ui.getInternals().setSession(session);

        handler.handleRpc(ui, reader, request);
    }

    private void enableDau() {
        Mockito.when(deploymentConfiguration.isProductionMode())
                .thenReturn(true);
        Mockito.when(deploymentConfiguration.getBooleanProperty(
                ArgumentMatchers.eq(Constants.DAU_TOKEN),
                ArgumentMatchers.anyBoolean())).thenReturn(true);
    }
}
