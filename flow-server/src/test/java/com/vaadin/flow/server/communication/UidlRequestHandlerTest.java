
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
 *
 */

package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.dau.DAUUtils;
import com.vaadin.flow.server.dau.DauEnforcementException;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.pro.licensechecker.dau.EnforcementException;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class UidlRequestHandlerTest {

    private VaadinRequest request;
    private VaadinResponse response;
    private OutputStream outputStream;

    private UidlRequestHandler handler;

    @Before
    public void setup() throws IOException {
        request = Mockito.mock(VaadinRequest.class);
        response = Mockito.mock(VaadinResponse.class);
        outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);

        handler = new UidlRequestHandler();
    }

    @Test
    public void writeSessionExpired() throws Exception {
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
        Assert.assertTrue("Result should be true", result);

        String responseContent = CommunicationUtil
                .getStringWhenWriteBytesOffsetLength(outputStream);

        // response shouldn't contain async
        Assert.assertEquals("Invalid response",
                "for(;;);[{\"meta\":{\"sessionExpired\":true}}]",
                responseContent);
    }

    @Test
    public void writeSessionExpired_whenUINotFound() throws IOException {

        VaadinService service = mock(VaadinService.class);
        VaadinSession session = mock(VaadinSession.class);
        when(session.getService()).thenReturn(service);

        when(service.findUI(request)).thenReturn(null);

        Optional<SynchronizedRequestHandler.ResponseWriter> result = handler
                .synchronizedHandleRequest(session, request, response, null);
        Assert.assertTrue("ResponseWriter should be present",
                result.isPresent());
        result.get().writeResponse();
        String responseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // response shouldn't contain async
        Assert.assertEquals("Invalid response",
                "for(;;);[{\"meta\":{\"sessionExpired\":true}}]",
                responseContent);
    }

    @Test
    public void clientRequestsPreviousIdAndPayload_resendPreviousResponse()
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
        Assert.assertTrue("ResponseWriter should be present",
                result.isPresent());
        result.get().writeResponse();
        String responseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // Init clean response
        response = Mockito.mock(VaadinResponse.class);
        outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);

        result = handler.synchronizedHandleRequest(session, request, response,
                requestBody);
        Assert.assertTrue("ResponseWriter should be present",
                result.isPresent());
        result.get().writeResponse();
        String resendResponseContent = CommunicationUtil
                .getStringWhenWriteString(outputStream);

        // response shouldn't contain async
        Assert.assertEquals("Server should send same content again",
                responseContent, resendResponseContent);
    }

    @Test
    public void should_modifyUidl_when_MPR() throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = generateUidl(true, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        uidl = JacksonUtils.readTree(out.substring(9, out.length() - 1));

        String v7Uidl = uidl.get("execute").get(2).get(1).textValue();
        assertFalse(v7Uidl.contains("http://localhost:9998/#!away"));
        assertTrue(v7Uidl.contains("http://localhost:9998/"));
        assertFalse(v7Uidl.contains("window.location.hash = '!away';"));
    }

    @Test
    public void should_changeURL_when_v7LocationProvided() throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = generateUidl(true, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        uidl = JacksonUtils.readTree(out.substring(9, out.length() - 1));

        assertEquals(
                "setTimeout(() => history.pushState(null, null, 'http://localhost:9998/#!away'));",
                uidl.get("execute").get(1).get(1).textValue());
    }

    @Test
    public void should_updateHash_when_v7LocationNotProvided()
            throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = generateUidl(false, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        uidl = JacksonUtils.readTree(out.substring(9, out.length() - 1));

        assertEquals(
                "setTimeout(() => history.pushState(null, null, location.pathname + location.search + '#!away'));",
                uidl.get("execute").get(1).get(1).textValue());
    }

    @Test
    public void should_not_modify_non_MPR_Uidl() throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = generateUidl(true, true);
        ((ArrayNode) uidl.get("execute").get(2)).remove(1);

        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String expected = uidl.toString();

        String out = writer.toString();
        uidl = JacksonUtils.readTree(out.substring(9, out.length() - 1));

        String actual = uidl.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void should_not_update_browser_history_if_no_hash_in_location()
            throws Exception {
        UI ui = getUi();

        handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        ObjectNode uidl = getUidlWithNoHashInLocation();

        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        Assert.assertFalse(out.contains("history.pushState"));
    }

    @Test
    public void synchronizedHandleRequest_DauEnforcementException_setsStatusCode503()
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

    private ObjectNode generateUidl(boolean withLocation, boolean withHash) {

        // @formatter:off
        ObjectNode uidl = JacksonUtils.readTree(
                "{" +
                "  \"syncId\": 3," +
                "  \"clientId\": 3," +
                "  \"changes\": []," +
                "  \"execute\": [" +
                "   [\"\", \"document.title = $0\"]," +
                "   [\"\", \"setTimeout(() => window.history.pushState(null, '', $0))\"]," +
                "   [[0, 16], \"___PLACE_FOR_V7_UIDL___\", \"$0.firstElementChild.setResponse($1)\"]," +
                "   [1,null,[0, 16], \"return (function() { this.$server['}p']($0, true, $1)}).apply($2)\"]" +
                "  ]," +
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
            "\"change\", {\"pid\": \"0\"}, [\"0\", {\"id\": \"0\", \"location\": \"http://localhost:9998/#!away\"}]";

        String hashRpc =
             "window.location.hash = '!away';";

        // @formatter:on

        if (withLocation) {
            v7String = v7String.replace("\"___PLACE_FOR_LOCATION_CHANGE___\"",
                    locationChange);
        }
        if (withHash) {
            v7String = v7String.replace("___PLACE_FOR_HASH_RPC___", hashRpc);
        }

        ((ArrayNode) uidl.get("execute").get(2)).set(1, v7String);
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
