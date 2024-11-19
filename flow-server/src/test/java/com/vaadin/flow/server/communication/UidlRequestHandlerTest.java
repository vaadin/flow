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
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.internal.JavaScriptBootstrapUI;
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
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

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
    public void should_not_modifyUidl_when_MPR_nonJavaScriptBootstrapUI()
            throws Exception {
        JavaScriptBootstrapUI ui = null;

        UidlRequestHandler handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        JsonObject uidl = generateUidl(true, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();

        assertTrue(out.startsWith("for(;;);[{"));
        assertTrue(out.endsWith("}]"));

        uidl = JsonUtil.parse(out.substring(9, out.length() - 1));
        String v7Uidl = uidl.getArray("execute").getArray(2).getString(1);

        assertTrue(v7Uidl.contains("http://localhost:9998/#!away"));
        assertTrue(v7Uidl.contains("window.location.hash = '!away';"));

        assertEquals("setTimeout(() => window.history.pushState(null, '', $0))",
                uidl.getArray("execute").getArray(1).getString(1));
    }

    @Test
    public void should_modifyUidl_when_MPR_JavaScriptBootstrapUI()
            throws Exception {
        JavaScriptBootstrapUI ui = mock(JavaScriptBootstrapUI.class);

        UidlRequestHandler handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        JsonObject uidl = generateUidl(true, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        uidl = JsonUtil.parse(out.substring(9, out.length() - 1));

        String v7Uidl = uidl.getArray("execute").getArray(2).getString(1);
        assertFalse(v7Uidl.contains("http://localhost:9998/#!away"));
        assertTrue(v7Uidl.contains("http://localhost:9998/"));
        assertFalse(v7Uidl.contains("window.location.hash = '!away';"));
    }

    @Test
    public void should_changeURL_when_v7LocationProvided() throws Exception {
        JavaScriptBootstrapUI ui = mock(JavaScriptBootstrapUI.class);

        UidlRequestHandler handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        JsonObject uidl = generateUidl(true, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        uidl = JsonUtil.parse(out.substring(9, out.length() - 1));

        assertEquals(
                "setTimeout(() => history.pushState(null, null, 'http://localhost:9998/#!away'));",
                uidl.getArray("execute").getArray(1).getString(1));
    }

    @Test
    public void should_updateHash_when_v7LocationNotProvided()
            throws Exception {
        JavaScriptBootstrapUI ui = mock(JavaScriptBootstrapUI.class);

        UidlRequestHandler handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        JsonObject uidl = generateUidl(false, true);
        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        uidl = JsonUtil.parse(out.substring(9, out.length() - 1));

        assertEquals(
                "setTimeout(() => history.pushState(null, null, location.pathname + location.search + '#!away'));",
                uidl.getArray("execute").getArray(1).getString(1));
    }

    @Test
    public void should_not_modify_non_MPR_Uidl() throws Exception {
        JavaScriptBootstrapUI ui = mock(JavaScriptBootstrapUI.class);

        UidlRequestHandler handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        JsonObject uidl = generateUidl(true, true);
        uidl.getArray("execute").getArray(2).remove(1);

        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String expected = uidl.toJson();

        String out = writer.toString();
        uidl = JsonUtil.parse(out.substring(9, out.length() - 1));

        String actual = uidl.toJson();

        assertEquals(expected, actual);
    }

    @Test
    public void should_not_update_browser_history_if_no_hash_in_location()
            throws Exception {
        JavaScriptBootstrapUI ui = mock(JavaScriptBootstrapUI.class);

        UidlRequestHandler handler = spy(new UidlRequestHandler());
        StringWriter writer = new StringWriter();

        JsonObject uidl = getUidlWithNoHashInLocation();

        doReturn(uidl).when(handler).createUidl(ui, false);

        handler.writeUidl(ui, writer, false);

        String out = writer.toString();
        Assert.assertFalse(out.contains("history.pushState"));
    }

    private JsonObject generateUidl(boolean withLocation, boolean withHash) {

        // @formatter:off
        JsonObject uidl = JsonUtil.parse(
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

        uidl.getArray("execute").getArray(2).set(1, v7String);
        return uidl;
    }

    private JsonObject getUidlWithNoHashInLocation() {
        // @formatter:off
        return JsonUtil.parse(
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
                "      '\"syncId\": 1, \"clientId\": 0, \"changes\" : [[\"change\",{\"pid\":\"0\"},[\"0\",{\"id\":\"0\",\"location\":\"http://localhost:8080/\",\"v\":{\"action\":\"\"}},[\"actions\",{}]]]], \"state\":{\"1\":{\"componentSettings\":[]}}, \"types\":{\"0\":\"0\",\"1\":\"2\"}, \"hierarchy\":{\"0\":[\"1\"]}, \"rpc\" : [], \"meta\" : {\"async\":true}, \"resources\" : {}, \"timings\":[113, 113]'," +
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

}
