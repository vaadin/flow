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
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.History.HistoryJs;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceEventBus;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.UidlRequestHandler.MprPushStateJs;
import com.vaadin.flow.server.dau.DAUUtils;
import com.vaadin.flow.server.dau.DauEnforcementException;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.pro.licensechecker.dau.EnforcementException;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class UidlRequestHandlerTest {

    private VaadinRequest request;
    private VaadinResponse response;
    private OutputStream outputStream;

    private UidlRequestHandler handler;

    @BeforeEach
    void setup() throws IOException {
        request = Mockito.mock(VaadinRequest.class);
        response = Mockito.mock(VaadinResponse.class);
        outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);

        handler = new UidlRequestHandler();
    }

    @Test
    void writeSessionExpired() throws Exception {
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(config.getBuildFolder()).thenReturn(".");
        VaadinContext context = new MockVaadinContext();
        Mockito.when(config.getContext()).thenReturn(context);
        VaadinService service = new VaadinServletService(null,
                new DefaultDeploymentConfiguration(config, getClass(),
                        new Properties()));
        when(request.getService()).thenReturn(service);

        when(request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(RequestType.UIDL.getIdentifier());

        boolean result = handler.handleSessionExpired(request, response);
        assertTrue(result, "Result should be true");

        String responseContent = CommunicationUtil
                .getStringWhenWriteBytesOffsetLength(outputStream);

        // response shouldn't contain async
        assertEquals("{\"meta\":{\"sessionExpired\":true}}", responseContent,
                "Invalid response");
    }

    @Test
    void writeSessionExpired_whenUINotFound() throws IOException {

        VaadinService service = mock(VaadinService.class);
        VaadinSession session = mock(VaadinSession.class);
        when(session.getService()).thenReturn(service);

        when(service.findUI(request)).thenReturn(null);

        Optional<SynchronizedRequestHandler.ResponseWriter> result = handler
                .synchronizedHandleRequest(session, request, response, null);
        assertTrue(result.isPresent(), "ResponseWriter should be present");
        result.get().writeResponse();
        String responseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // response shouldn't contain async
        assertEquals("{\"meta\":{\"sessionExpired\":true}}", responseContent,
                "Invalid response");
    }

    @Test
    void clientRequestsPreviousIdAndPayload_resendPreviousResponse()
            throws IOException {

        UI ui = getUi();
        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();
        DeploymentConfiguration conf = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(conf);
        Mockito.when(conf.isRequestTiming()).thenReturn(false);

        String requestBody = """
                {
                   "csrfToken": "d1f44a6f-bbe5-4493-a8a9-3f5f234a2a93",
                   "rpc": [
                     {
                       "type": "mSync",
                       "node": 12,
                       "feature": 1,
                       "property": "value",
                       "value": "a"
                     },
                     {
                       "type": "event",
                       "node": 12,
                       "event": "change",
                       "data": {}
                     }
                   ],
                   "syncId": 0,
                   "clientId": 0
                 }
                """;
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(conf.isSyncIdCheckEnabled()).thenReturn(true);

        Optional<SynchronizedRequestHandler.ResponseWriter> result = handler
                .synchronizedHandleRequest(session, request, response,
                        requestBody);
        assertTrue(result.isPresent(), "ResponseWriter should be present");
        result.get().writeResponse();
        String responseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // Init clean response
        response = Mockito.mock(VaadinResponse.class);
        outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);

        result = handler.synchronizedHandleRequest(session, request, response,
                requestBody);
        assertTrue(result.isPresent(), "ResponseWriter should be present");
        result.get().writeResponse();
        String resendResponseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // response shouldn't contain async
        assertEquals(responseContent, resendResponseContent,
                "Server should send same content again");
    }

    @Test
    void clientRequestsPreviousIdAndPayload_nothingRecorded_writesANewResponse()
            throws IOException {

        UI ui = getUi();
        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();
        DeploymentConfiguration conf = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(conf);
        Mockito.when(conf.isRequestTiming()).thenReturn(false);
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(conf.isSyncIdCheckEnabled()).thenReturn(true);

        String requestBody = """
                {
                   "csrfToken": "d1f44a6f-bbe5-4493-a8a9-3f5f234a2a93",
                   "rpc": [],
                   "syncId": 0,
                   "clientId": 0
                 }
                """;

        handler.synchronizedHandleRequest(session, request, response,
                requestBody).orElseThrow().writeResponse();
        ObjectNode firstResponse = JacksonUtils.readTree(
                CommunicationUtil.getStringWhenWriteString(outputStream));

        // The answer to the message the client is about to re-send was never
        // produced, so there is nothing recorded to send again. This is the
        // state left behind when creating a response fails, and the state the
        // push connection leaves when it cannot create one.
        ui.getInternals().setLastRequestResponse(null);

        response = Mockito.mock(VaadinResponse.class);
        outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);

        handler.synchronizedHandleRequest(session, request, response,
                requestBody).orElseThrow().writeResponse();
        String resendResponseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);
        assertTrue(resendResponseContent.startsWith("{"),
                "The client should get a JSON response, was: "
                        + resendResponseContent);
        ObjectNode resendResponse = JacksonUtils
                .readTree(resendResponseContent);

        // A sync id means a UIDL was written, which rules out the refresh and
        // critical-notification responses this handler can also produce, and a
        // sync id that moved on means it is a new one rather than the recorded
        // response being sent again.
        assertTrue(resendResponse.has(ApplicationConstants.SERVER_SYNC_ID),
                "The client should get a UIDL response, was: "
                        + resendResponseContent);
        assertTrue(resendResponse.get(ApplicationConstants.SERVER_SYNC_ID)
                .intValue() > firstResponse
                        .get(ApplicationConstants.SERVER_SYNC_ID).intValue(),
                "The response should be a newly created one, was: "
                        + resendResponseContent);
    }

    @Test
    void should_modifyUidl_when_MPR() throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = generateUidl(true, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        uidl = JacksonUtils.readTree(out);

        String v7Uidl = uidl.get("execute").get(2).get(1).textValue();
        assertFalse(v7Uidl.contains("http://localhost:9998/#!away"));
        assertTrue(v7Uidl.contains("http://localhost:9998/"));
        assertFalse(v7Uidl.contains("window.location.hash = '!away';"));
    }

    @Test
    void should_changeURL_when_v7LocationProvided() throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = generateUidl(true, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        uidl = JacksonUtils.readTree(out);

        assertEquals(
                UidlRequestHandler.functionId(MprPushStateJs.class,
                        "pushLocation", 1),
                functionRunBy(uidl, 1),
                "the push state of the corrected location should replace the one the response carried: "
                        + uidl);

        // The client applies the function to the parameter that follows the
        // arguments of the call, and refuses to run one whose parameters do
        // not add up, so the whole invocation is pinned here
        ArrayNode invocation = (ArrayNode) uidl.get("execute").get(1);
        assertEquals(3, invocation.size(),
                "the invocation should carry the argument, the element and what it runs: "
                        + invocation);
        assertEquals("http://localhost:9998/#!away",
                invocation.get(0).asString(),
                "the corrected location should be the argument of the call");
        assertTrue(invocation.get(1).isNull(),
                "the call has nothing to run on, so nothing should follow the argument");
    }

    @Test
    void should_updateHash_when_v7LocationNotProvided() throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = generateUidl(false, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        uidl = JacksonUtils.readTree(out);

        assertEquals(
                UidlRequestHandler.functionId(MprPushStateJs.class, "pushHash",
                        1),
                functionRunBy(uidl, 1),
                "the push state of the corrected hash should replace the one the response carried: "
                        + uidl);
        assertEquals("!away",
                ((ArrayNode) uidl.get("execute").get(1)).get(0).asString(),
                "the corrected hash should be the argument of the call: "
                        + uidl);
    }

    @Test
    void should_sendTheCorrectedLocation_as_aParameter() throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        // A location is application data. Whatever it holds has to reach the
        // browser unchanged, and must not become part of what the browser
        // runs, which an apostrophe in it otherwise ends up being.
        ObjectNode uidl = generateUidl(true, false, "!it's");
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        ObjectNode written = JacksonUtils.readTree(writer.toString());
        ArrayNode invocation = (ArrayNode) written.get("execute").get(1);

        assertTrue(hasParameter(invocation, "http://localhost:9998/#!it's"),
                "the corrected location should be a parameter of the push state, was: "
                        + invocation);
        assertFalse(whatRuns(written, 1).toString().contains("it's"),
                "the corrected location should not be part of what the push state runs, was: "
                        + whatRuns(written, 1));
    }

    @Test
    void should_runTheSameThing_when_theCorrectedLocationDiffers()
            throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());

        // The constants a session has sent are remembered for good, so a push
        // state that is its own constant per location makes a session grow
        // with every hash the user navigates to.
        String first = pushStateOf(ui, "!away");
        String second = pushStateOf(ui, "!elsewhere");

        assertEquals(first, second,
                "two corrected locations should run the same thing, differing in the parameter");
    }

    @Test
    void should_keepAnApplicationInvocation_that_mentionsPushState()
            throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = generateUidl(true, true);

        // An application may run the browser function that the router happens
        // to use. Only what the router scheduled may be corrected.
        String applicationScript = "history.pushState(null, '', '/tracked')";
        ((ObjectNode) uidl.get("constants")).put("applicationScript",
                applicationScript);
        ArrayNode invocation = JacksonUtils.createArrayNode();
        invocation.add("");
        invocation.add("applicationScript");
        int index = ((ArrayNode) uidl.get("execute")).size();
        ((ArrayNode) uidl.get("execute")).add(invocation);

        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        ObjectNode written = JacksonUtils.readTree(writer.toString());
        assertEquals(applicationScript, whatRuns(written, index).asString(),
                "what the application scheduled should still be there: "
                        + written);
    }

    @Test
    void should_replaceThePushState_that_theWriterActuallyEncodes()
            throws Exception {
        UI ui = getUi();
        DeploymentConfiguration configuration = mock(
                DeploymentConfiguration.class);
        when(ui.getSession().getService().getDeploymentConfiguration())
                .thenReturn(configuration);
        when(configuration.isReactEnabled()).thenReturn(false);

        // The fix-up recognizes the router's push state by what names it among
        // the constants of the response, which is a contract between it and
        // the writer: this schedules a real location change and encodes it the
        // way a response does, so that a change to either side that stops the
        // two from naming the same thing fails here rather than silently
        // leaving the wrong location pushed.
        ui.getPage().getHistory().pushState(null, "away");
        ArrayNode encoded = UidlWriter.encodeExecuteJavaScriptList(
                ui.getInternals().dumpPendingJavaScriptInvocations(),
                ui.getInternals().getConstantPool());
        assertEquals(1, encoded.size(),
                "the router should have scheduled one invocation, got: "
                        + encoded);

        ObjectNode uidl = generateUidl(true, true);
        ((ArrayNode) uidl.get("execute")).set(1, encoded.get(0));
        ((ObjectNode) uidl.get("constants"))
                .setAll(ui.getInternals().getConstantPool().dumpConstants());
        int invocations = uidl.get("execute").size();

        handler = spy(new UidlRequestHandler());
        doReturn(uidl).when(handler).createUidl(ui, false);
        StringWriter writer = new StringWriter();

        handler.writeUidl(ui, writer, false);

        ObjectNode written = JacksonUtils.readTree(writer.toString());
        assertEquals(invocations, written.get("execute").size(),
                "the corrected push state should replace the one the router scheduled rather than be added next to it: "
                        + written);
        assertEquals(
                UidlRequestHandler.functionId(MprPushStateJs.class,
                        "pushLocation", 1),
                functionRunBy(written, 1),
                "and it should be what that invocation now runs: " + written);
    }

    @Test
    void should_not_modify_non_MPR_Uidl() throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = generateUidl(true, true);
        ((ArrayNode) uidl.get("execute").get(2)).remove(1);

        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String expected = uidl.toString();

        String out = writer.toString();
        uidl = JacksonUtils.readTree(out);

        String actual = uidl.toString();

        assertEquals(expected, actual);
    }

    @Test
    void should_not_update_browser_history_if_no_hash_in_location()
            throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = getUidlWithNoHashInLocation();

        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        ObjectNode written = JacksonUtils.readTree(writer.toString());
        assertEquals(1, written.get("execute").size(),
                "nothing should be pushed when the v7 location carries no hash: "
                        + written);
        assertFalse(written.has("constants"),
                "nothing should be pushed when the v7 location carries no hash: "
                        + written);
    }

    @Test
    void synchronizedHandleRequest_DauEnforcementException_setsStatusCode503()
            throws IOException {
        VaadinService service = mock(VaadinService.class);
        VaadinSession session = mock(VaadinSession.class);
        when(session.getService()).thenReturn(service);
        UI ui = Mockito.mock(UI.class);

        when(service.findUI(request)).thenReturn(ui);

        ServerRpcHandler serverRpcHandler = new ServerRpcHandler() {
            @Override
            public void handleRpc(UI ui, String requestBody,
                    VaadinRequest request) {
                throw new DauEnforcementException(
                        new EnforcementException("test"));
            }
        };

        handler = new UidlRequestHandler() {
            @Override
            protected ServerRpcHandler createRpcHandler() {
                return serverRpcHandler;
            }
        };

        handler.synchronizedHandleRequest(session, request, response, "");

        Mockito.verify(response).setHeader(DAUUtils.STATUS_CODE_KEY, "503");
    }

    @Test
    void synchronizedHandleRequest_MessageIdSyncException_returnsSyncErrorResponse()
            throws IOException {
        VaadinService service = mock(VaadinService.class);
        VaadinSession session = mock(VaadinSession.class);
        when(session.getService()).thenReturn(service);
        UI ui = Mockito.mock(UI.class);

        when(service.findUI(request)).thenReturn(ui);
        when(request.getService()).thenReturn(service);

        SystemMessages systemMessages = new CustomizedSystemMessages();
        when(service.getSystemMessages(Mockito.any(), Mockito.any()))
                .thenReturn(systemMessages);

        ServerRpcHandler serverRpcHandler = new ServerRpcHandler() {
            @Override
            public void handleRpc(UI ui, String requestBody,
                    VaadinRequest request)
                    throws ServerRpcHandler.MessageIdSyncException {
                throw new ServerRpcHandler.MessageIdSyncException(1, 5);
            }
        };

        handler = new UidlRequestHandler() {
            @Override
            protected ServerRpcHandler createRpcHandler() {
                return serverRpcHandler;
            }
        };

        Optional<SynchronizedRequestHandler.ResponseWriter> result = handler
                .synchronizedHandleRequest(session, request, response, "");
        assertTrue(result.isPresent(), "ResponseWriter should be present");
        result.get().writeResponse();
        String responseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // Verify sync error notification is returned
        assertTrue(responseContent.contains("Synchronization Error"),
                "Response should contain caption");
        assertTrue(
                responseContent.contains("Your session needs to be refreshed"),
                "Response should contain message");
    }

    @Test
    void synchronizedHandleRequest_MessageIdSyncException_usesCustomMessages()
            throws IOException {
        VaadinService service = mock(VaadinService.class);
        VaadinSession session = mock(VaadinSession.class);
        when(session.getService()).thenReturn(service);
        UI ui = Mockito.mock(UI.class);

        when(service.findUI(request)).thenReturn(ui);
        when(request.getService()).thenReturn(service);

        CustomizedSystemMessages customMessages = new CustomizedSystemMessages();
        customMessages.setSyncErrorCaption("Custom Sync Caption");
        customMessages.setSyncErrorMessage("Custom sync error message");
        customMessages.setSyncErrorURL("/custom-redirect");
        when(service.getSystemMessages(Mockito.any(), Mockito.any()))
                .thenReturn(customMessages);

        ServerRpcHandler serverRpcHandler = new ServerRpcHandler() {
            @Override
            public void handleRpc(UI ui, String requestBody,
                    VaadinRequest request) throws MessageIdSyncException {
                throw new ServerRpcHandler.MessageIdSyncException(1, 5);
            }
        };

        handler = new UidlRequestHandler() {
            @Override
            protected ServerRpcHandler createRpcHandler() {
                return serverRpcHandler;
            }
        };

        Optional<SynchronizedRequestHandler.ResponseWriter> result = handler
                .synchronizedHandleRequest(session, request, response, "");
        assertTrue(result.isPresent(), "ResponseWriter should be present");
        result.get().writeResponse();
        String responseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // Verify custom sync error notification is returned
        assertTrue(responseContent.contains("Custom Sync Caption"),
                "Response should contain custom caption");
        assertTrue(responseContent.contains("Custom sync error message"),
                "Response should contain custom message");
        assertTrue(responseContent.contains("/custom-redirect"),
                "Response should contain custom URL");
    }

    @Test
    void synchronizedHandleRequest_MessageIdSyncException_notificationDisabled_silentRefresh()
            throws IOException {
        VaadinService service = mock(VaadinService.class);
        VaadinSession session = mock(VaadinSession.class);
        when(session.getService()).thenReturn(service);
        UI ui = Mockito.mock(UI.class);

        when(service.findUI(request)).thenReturn(ui);
        when(request.getService()).thenReturn(service);

        CustomizedSystemMessages customMessages = new CustomizedSystemMessages();
        customMessages.setSyncErrorNotificationEnabled(false);
        when(service.getSystemMessages(Mockito.any(), Mockito.any()))
                .thenReturn(customMessages);

        ServerRpcHandler serverRpcHandler = new ServerRpcHandler() {
            @Override
            public void handleRpc(UI ui, String requestBody,
                    VaadinRequest request) throws MessageIdSyncException {
                throw new ServerRpcHandler.MessageIdSyncException(1, 5);
            }
        };

        handler = new UidlRequestHandler() {
            @Override
            protected ServerRpcHandler createRpcHandler() {
                return serverRpcHandler;
            }
        };

        Optional<SynchronizedRequestHandler.ResponseWriter> result = handler
                .synchronizedHandleRequest(session, request, response, "");
        assertTrue(result.isPresent(), "ResponseWriter should be present");
        result.get().writeResponse();
        String responseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // Verify caption and message are null (triggers silent refresh)
        assertTrue(responseContent.contains("\"caption\":null"),
                "Response should have null caption");
        assertTrue(responseContent.contains("\"message\":null"),
                "Response should have null message");
    }

    /**
     * What the invocation at the given index of the given response runs, which
     * the invocation names among the constants of the response: the expression
     * for one that runs an expression, and the function of the bundle for one
     * that runs declared JavaScript.
     */
    private static JsonNode whatRuns(ObjectNode uidl, int index) {
        ArrayNode invocation = (ArrayNode) uidl.get("execute").get(index);
        String name = invocation.get(invocation.size() - 1).asString();
        return uidl.get("constants").get(name);
    }

    /**
     * The identifier of the function that the invocation at the given index of
     * the given response runs.
     */
    private static String functionRunBy(ObjectNode uidl, int index) {
        return whatRuns(uidl, index).get(JsonConstants.UIDL_KEY_JS_FUNCTION)
                .asString();
    }

    /**
     * Whether the given invocation carries the given value as a parameter,
     * which is any element but the last, that one naming what it runs.
     */
    private static boolean hasParameter(ArrayNode invocation, String value) {
        for (int i = 0; i < invocation.size() - 1; i++) {
            JsonNode parameter = invocation.get(i);
            if (parameter.getNodeType().equals(JsonNodeType.STRING)
                    && value.equals(parameter.asString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a response that corrects the given hash and answers with what
     * names the push state it runs. A constant belongs to the session rather
     * than to the response, so a second response that runs the same thing names
     * it and carries it no more.
     */
    private String pushStateOf(UI ui, String hash) throws IOException {
        StringWriter writer = new StringWriter();
        ObjectNode uidl = generateUidl(false, true, hash);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        ArrayNode invocation = (ArrayNode) JacksonUtils
                .readTree(writer.toString()).get("execute").get(1);
        return invocation.get(invocation.size() - 1).asString();
    }

    private ObjectNode generateUidl(boolean withLocation, boolean withHash) {
        return generateUidl(withLocation, withHash, "!away");
    }

    private ObjectNode generateUidl(boolean withLocation, boolean withHash,
            String hash) {

        // @formatter:off
        ObjectNode uidl = JacksonUtils.readTree(
                "{" +
                "  \"syncId\": 3," +
                "  \"clientId\": 3," +
                "  \"changes\": []," +
                "  \"execute\": [" +
                "   [\"\", \"title\"]," +
                "   [\"\", \"pushState\"]," +
                "   [[0, 16], \"___PLACE_FOR_V7_UIDL___\", \"setResponse\"]," +
                "   [1,null,[0, 16], \"callServer\"]" +
                "  ]," +
                "  \"constants\": {" +
                "   \"title\": \"document.title = $0\"," +
                "   \"pushState\": \"setTimeout(() => window.history.pushState(null, '', $0))\"," +
                "   \"setResponse\": \"$0.firstElementChild.setResponse($1)\"," +
                "   \"callServer\": \"return (function() { this.$server['}p']($0, true, $1)}).apply($2)\"" +
                "  }," +
                "  \"timings\": []" +
                "}");

        String v7String =
            "\"syncId\": 2," +
            "\"clientId\": 2," +
            "\"changes\": [" +
            "  [],[\"___PLACE_FOR_LOCATION_CHANGE___\"]" +
            "]," +
            "\"state\": {" +
            "}," +
            "\"types\": {" +
            "}," +
            "\"hierarchy\": {" +
            "}," +
            "\"rpc\": [" +
            " [],[" +
            "  \"11\"," +
            "  \"com.vaadin.shared.extension.javascriptmanager.ExecuteJavaScriptRpc\"," +
            "  \"executeJavaScript\", [ \"___PLACE_FOR_HASH_RPC___\" ]" +
            " ],[" +
            "  \"12\"," +
            "  \"com.example.FooRpc\"," +
            "  \"barMethod\", [{}, {}]" +
            " ],[]" +
            "]," +
            "\"meta\": {}, \"resources\": {},\"typeMappings\": {},\"typeInheritanceMap\": {}, \"timings\": []";

        String locationChange =
            "\"change\", {\"pid\": \"0\"}, [\"0\", {\"id\": \"0\", \"location\": \"http://localhost:9998/#" + hash + "\"}]";

        String hashRpc = "window.location.hash = '" + hash + "';";

        // @formatter:on

        if (withLocation) {
            v7String = v7String.replace("\"___PLACE_FOR_LOCATION_CHANGE___\"",
                    locationChange);
        }
        if (withHash) {
            v7String = v7String.replace("___PLACE_FOR_HASH_RPC___", hashRpc);
        }

        ((ArrayNode) uidl.get("execute").get(2)).set(1, v7String);

        // The push state the router scheduled, as UidlWriter sends it: the
        // invocation names the constant of the function the build generated
        // for History.HistoryJs.pushState, and that is what the fix-up
        // corrects.
        ObjectNode routerPushState = UidlWriter.functionConstant(
                UidlRequestHandler.functionId(HistoryJs.class, "pushState", 2));
        String name = new ConstantPoolKey(routerPushState).getId();
        ((ArrayNode) uidl.get("execute").get(1)).set(1, name);
        ((ObjectNode) uidl.get("constants")).remove("pushState");
        ((ObjectNode) uidl.get("constants")).set(name, routerPushState);

        return uidl;
    }

    private ObjectNode getUidlWithNoHashInLocation() {
        // @formatter:off
        return JacksonUtils.readTree(
                "{" +
                "  \"syncId\": 3," +
                "  \"clientId\": 3," +
                "  \"changes\": []," +
                "  \"execute\": [" +
                "    [" +
                "      [" +
                "        0," +
                "        9" +
                "      ]," +
                "      \"'syncId': 1, 'clientId': 0, 'changes' : [['change',{'pid':'0'},['0',{'id':'0','location':'http://localhost:8080/','v':{'action':''}},['actions',{}]]]], 'state':{'1':{'componentSettings':[]}}, 'types':{'0':'0','1':'2'}, 'hierarchy':{'0':['1']}, 'rpc' : [], 'meta' : {'async':true}, 'resources' : {}, 'timings':[113, 113]\"," +
                "      \"ROOT\"" +
                "    ]" +
                "  ]," +
                "  \"timings\": [" +
                "    20880," +
                "    18181" +
                "  ]" +
                "}"
        );
        // @formatter:on
    }

    /**
     * Mock ui with session.
     *
     * @return
     */
    private static UI getUi() {
        VaadinService service = mock(VaadinService.class);
        when(service.getEventBus())
                .thenReturn(new VaadinServiceEventBus(service));
        VaadinSession session = new VaadinSession(service) {
            @Override
            public boolean hasLock() {
                return true;
            }

            @Override
            public VaadinService getService() {
                return service;
            }
        };

        UI ui = new MockUI(session);

        when(service.findUI(Mockito.any())).thenReturn(ui);

        return ui;
    }
}
