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
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.util.regex.Pattern;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UI.BrowserNavigateEvent;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.dom.NodeVisitor.ElementType;
import com.vaadin.flow.dom.TestNodeVisitor;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletResponse;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.communication.PushMode;

@NotThreadSafe
public class JavaScriptBootstrapHandlerTest {

    private MockServletServiceSessionSetup mocks;

    private TestVaadinServletResponse response;
    private VaadinSession session;
    private JavaScriptBootstrapHandler jsInitHandler;

    @Push
    static public class PushAppShell implements AppShellConfigurator {
    }

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        response = mocks.createResponse();
        session = mocks.getSession();
        jsInitHandler = new JavaScriptBootstrapHandler();
    }

    @After
    public void tearDown() {
        mocks.cleanup();
    }

    @Test
    public void should_handleRequest_when_initTypeRequest() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/", "v-r=init&foo");
        Assert.assertTrue(jsInitHandler.canHandleRequest(request));
    }

    @Test
    public void should_not_handleRequest_when_pathInfo_set() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/foo",
                "v-r=init&foo");
        Assert.assertFalse(jsInitHandler.canHandleRequest(request));
    }

    @Test
    public void should_not_handleRequest_if_not_initTypeRequest()
            throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/", "v-r=bar");
        Assert.assertFalse(jsInitHandler.canHandleRequest(request));
    }

    @Test
    public void should_produceValidJsonResponse() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/",
                "v-r=init&foo&location");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertEquals(200, response.getErrorCode());

        Assert.assertEquals("application/json", response.getContentType());

        JsonNode json = JacksonUtils.readTree(response.getPayload());

        Assert.assertTrue(json.has("stats"));
        Assert.assertTrue(json.has("errors"));
        Assert.assertTrue(json.has("appConfig"));
        Assert.assertTrue(json.get("appConfig").has("appId"));
        Assert.assertTrue(json.get("appConfig").get("uidl").has("changes"));
        Assert.assertTrue(json.get("appConfig").get("debug").booleanValue());
        Assert.assertFalse(json.get("appConfig").has("webComponentMode"));

        Assert.assertEquals("./",
                json.get("appConfig").get("contextRootUrl").asText());
        Assert.assertNull(
                "ServiceUrl should not be set. It will be computed by flow-client",
                json.get("appConfig").get("serviceUrl"));
        Assert.assertEquals("http://localhost:8888/",
                json.get("appConfig").get("requestURL").asText());

        Assert.assertFalse(json.has("pushScript"));
    }

    @Test
    public void should_initialize_UI() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/",
                "v-r=init&foo&location=");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertNotNull(UI.getCurrent());
        Assert.assertEquals(UI.class, UI.getCurrent().getClass());
    }

    @Test
    public void should_attachViewTo_UiContainer() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/",
                "v-r=init&foo&location=");
        jsInitHandler.handleRequest(session, request, response);

        UI ui = UI.getCurrent();
        ui.browserNavigate(new BrowserNavigateEvent(ui, true, "a-route", "", "",
                null, ""));

        TestNodeVisitor visitor = new TestNodeVisitor(true);
        BasicElementStateProvider.get().visit(ui.getElement().getNode(),
                visitor);

        Assert.assertTrue(
                hasNodeTag(visitor, "^<body>.*", ElementType.REGULAR));
        Assert.assertTrue(hasNodeTag(visitor, "^<flow-container-.*>.*",
                ElementType.VIRTUAL_ATTACHED));
        Assert.assertTrue(hasNodeTag(visitor, "^<div>.*", ElementType.REGULAR));
        // There are no routes, so it will get the "no views found" message
        Assert.assertTrue(hasNodeTag(visitor, "^<div>.*No views found.*",
                ElementType.REGULAR));

    }

    @Test
    public void should_respondPushScript_when_enabledInDeploymentConfiguration()
            throws Exception {
        mocks.getDeploymentConfiguration().setPushMode(PushMode.AUTOMATIC);

        VaadinRequest request = mocks.createRequest(mocks, "/",
                "v-r=init&foo&location=");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertEquals(200, response.getErrorCode());
        Assert.assertEquals("application/json", response.getContentType());
        JsonNode json = JacksonUtils.readTree(response.getPayload());

        // Using regex, because version depends on the build
        Assert.assertTrue(json.get("pushScript").asText().matches(
                "^\\./VAADIN/static/push/vaadinPush\\.js\\?v=[\\w\\.\\-]+$"));
    }

    @Test
    public void should_respondPushScript_when_nonRootServletPath()
            throws Exception {
        mocks.getDeploymentConfiguration().setPushMode(PushMode.AUTOMATIC);

        VaadinRequest request = mocks.createRequest(mocks, "/", "/vaadin/",
                "v-r=init&foo&location=");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertEquals(200, response.getErrorCode());
        Assert.assertEquals("application/json", response.getContentType());
        JsonNode json = JacksonUtils.readTree(response.getPayload());

        // Using regex, because version depends on the build
        Assert.assertTrue(json.get("pushScript").asText().matches(
                "^\\./VAADIN/static/push/vaadinPush\\.js\\?v=[\\w\\.\\-]+$"));
    }

    @Test
    public void should_invoke_modifyPushConfiguration() throws Exception {
        AppShellRegistry registry = Mockito.mock(AppShellRegistry.class);
        mocks.setAppShellRegistry(registry);

        VaadinRequest request = mocks.createRequest(mocks, "/",
                "v-r=init&foo&location=");
        jsInitHandler.handleRequest(session, request, response);

        Mockito.verify(registry)
                .modifyPushConfiguration(Mockito.any(PushConfiguration.class));
    }

    @Test
    public void should_respondPushScript_when_annotatedInAppShell()
            throws Exception {
        VaadinServletContext context = new VaadinServletContext(
                mocks.getServletContext());
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(PushAppShell.class);
        mocks.setAppShellRegistry(registry);

        VaadinRequest request = mocks.createRequest(mocks, "/",
                "v-r=init&foo&location");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertEquals(200, response.getErrorCode());
        Assert.assertEquals("application/json", response.getContentType());
        JsonNode json = JacksonUtils.readTree(response.getPayload());

        // Using regex, because version depends on the build
        Assert.assertTrue(json.get("pushScript").asText().matches(
                "^\\./VAADIN/static/push/vaadinPush\\.js\\?v=[\\w\\.\\-]+$"));
    }

    @Test
    public void synchronizedHandleRequest_badLocation_noUiCreated()
            throws IOException {
        final JavaScriptBootstrapHandler bootstrapHandler = new JavaScriptBootstrapHandler();

        VaadinRequest request = mocks.createRequest(mocks, "/",
                "v-r=init&location=..**");

        final MockServletServiceSessionSetup.TestVaadinServletResponse response = mocks
                .createResponse();

        final boolean value = bootstrapHandler.synchronizedHandleRequest(
                mocks.getSession(), request, response);
        Assert.assertTrue("No further request handlers should be called",
                value);

        Assert.assertEquals("Invalid status code reported", 400,
                response.getErrorCode());
        Assert.assertEquals("Invalid message reported",
                "Invalid location: Relative path cannot contain .. segments",
                response.getErrorMessage());
    }

    @Test
    public void synchronizedHandleRequest_noLocationParameter_noUiCreated()
            throws IOException {
        final JavaScriptBootstrapHandler bootstrapHandler = new JavaScriptBootstrapHandler();

        VaadinRequest request = mocks.createRequest(mocks, "/",
                "v-r=ini&foobar");

        final MockServletServiceSessionSetup.TestVaadinServletResponse response = mocks
                .createResponse();

        final boolean value = bootstrapHandler.synchronizedHandleRequest(
                mocks.getSession(), request, response);
        Assert.assertTrue("No further request handlers should be called",
                value);

        Assert.assertEquals("Invalid status code reported", 400,
                response.getErrorCode());
        Assert.assertEquals("Invalid message reported",
                "Invalid location: Location parameter missing from bootstrap request to server.",
                response.getErrorMessage());
    }

    private boolean hasNodeTag(TestNodeVisitor visitor, String htmContent,
            ElementType type) {
        Pattern regex = Pattern.compile(htmContent, Pattern.DOTALL);
        return visitor.getVisited().entrySet().stream().anyMatch(entry -> {
            return entry.getValue().equals(type)
                    && regex.matcher(entry.getKey().toString()).find();
        });
    }

}
