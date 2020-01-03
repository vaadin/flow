/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.regex.Pattern;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.JavaScriptBootstrapUI;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.dom.NodeVisitor.ElementType;
import com.vaadin.flow.dom.TestNodeVisitor;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletResponse;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.communication.PushMode;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.component.internal.JavaScriptBootstrapUI.SERVER_ROUTING;

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
        VaadinRequest request = mocks.createRequest(mocks, "/foo/?v-r=init&foo");
        Assert.assertTrue(jsInitHandler.canHandleRequest(request));
    }

    @Test
    public void should_not_handleRequest_if_not_initTypeRequest() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/foo/?v-r=bar");
        Assert.assertFalse(jsInitHandler.canHandleRequest(request));
    }

    @Test
    public void should_produceValidJsonResponse() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/foo/?v-r=init&foo");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertEquals(200, response.getErrorCode());

        Assert.assertEquals("application/json", response.getContentType());

        JsonObject json = Json.parse(response.getPayload());

        Assert.assertTrue(json.hasKey("stats"));
        Assert.assertTrue(json.hasKey("errors"));
        Assert.assertTrue(json.hasKey("appConfig"));
        Assert.assertTrue(json.getObject("appConfig").hasKey("appId"));
        Assert.assertTrue(json.getObject("appConfig").getObject("uidl").hasKey("changes"));
        Assert.assertTrue(json.getObject("appConfig").getBoolean("debug"));
        Assert.assertFalse(json.getObject("appConfig").hasKey("webComponentMode"));

        Assert.assertEquals("./", json.getObject("appConfig").getString("contextRootUrl"));
        Assert.assertNull(
                "ServiceUrl should not be set. It will be computed by flow-client",
                json.getObject("appConfig").get("serviceUrl"));
        Assert.assertEquals("http://localhost:8888/foo/", json.getObject("appConfig").getString("requestURL"));

        Assert.assertFalse(json.hasKey("pushScript"));
    }

    @Test
    public void should_initialize_UI() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/foo/?v-r=init&foo");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertNotNull(UI.getCurrent());
        Assert.assertEquals(JavaScriptBootstrapUI.class, UI.getCurrent().getClass());

        Mockito.verify(session, Mockito.times(0)).setAttribute(SERVER_ROUTING, Boolean.TRUE);

    }

    @Test
    public void should_attachViewTo_UiContainer() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/foo/?v-r=init&foo");
        jsInitHandler.handleRequest(session, request, response);

        JavaScriptBootstrapUI ui = (JavaScriptBootstrapUI) UI.getCurrent();
        ui.connectClient("a-tag", "an-id", "a-route");

        TestNodeVisitor visitor = new TestNodeVisitor(true);
        BasicElementStateProvider.get().visit(ui.getElement().getNode(), visitor);

        Assert.assertTrue(hasNodeTag(visitor, "^<body>.*", ElementType.REGULAR));
        Assert.assertTrue(hasNodeTag(visitor, "^<a-tag>.*", ElementType.VIRTUAL_ATTACHED));
        Assert.assertTrue(hasNodeTag(visitor, "^<div>.*", ElementType.REGULAR));
        Assert.assertTrue(
                hasNodeTag(visitor, "^<div>.*Could not navigate to 'a-route'.*",
                        ElementType.REGULAR));

    }

    @Test
    public void should_attachViewTo_Body_when_location() throws Exception {

        VaadinRequest request = mocks.createRequest(mocks, "/foo/?v-r=init&location=%2Fbar%3Fpar1%26par2");

        jsInitHandler.handleRequest(session, request, response);

        JavaScriptBootstrapUI ui = (JavaScriptBootstrapUI) UI.getCurrent();

        TestNodeVisitor visitor = new TestNodeVisitor(true);
        BasicElementStateProvider.get().visit(ui.getElement().getNode(), visitor);

        Assert.assertTrue(hasNodeTag(visitor, "^<body>.*", ElementType.REGULAR));
        Assert.assertTrue(hasNodeTag(visitor, "^<div>.*", ElementType.REGULAR));
        Assert.assertTrue(
                hasNodeTag(visitor, "^<div>.*Could not navigate to 'bar'.*",
                        ElementType.REGULAR));

        Mockito.verify(session, Mockito.times(1)).setAttribute(SERVER_ROUTING, Boolean.TRUE);
    }

    @Test
    public void should_respondPushScript_when_enabledInDeploymentConfiguration()
            throws Exception {
        mocks.getDeploymentConfiguration().setPushMode(PushMode.AUTOMATIC);

        VaadinRequest request = mocks.createRequest(mocks,
                "/foo/?v-r=init&foo");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertEquals(200, response.getErrorCode());
        Assert.assertEquals("application/json", response.getContentType());
        JsonObject json = Json.parse(response.getPayload());

        // Using regex, because version depends on the build
        Assert.assertTrue(json.getString("pushScript").matches(
                "^\\./VAADIN/static/push/vaadinPush\\.js\\?v=[\\w\\.\\-]+$"));
    }

    @Test
    public void should_invoke_modifyPushConfiguration() throws Exception {
        AppShellRegistry registry = Mockito.mock(AppShellRegistry.class);
        mocks.setAppShellRegistry(registry);

        VaadinRequest request = mocks.createRequest(mocks,
                "/foo/?v-r=init&foo");
        jsInitHandler.handleRequest(session, request, response);

        Mockito.verify(registry)
                .modifyPushConfiguration(Mockito.any(PushConfiguration.class));
    }

    @Test
    public void should_respondPushScript_when_annotatedInAppShell()
            throws Exception {
        AppShellRegistry registry = new AppShellRegistry();
        registry.setShell(PushAppShell.class);
        mocks.setAppShellRegistry(registry);

        VaadinRequest request = mocks.createRequest(mocks, "/foo/?v-r=init&foo");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertEquals(200, response.getErrorCode());
        Assert.assertEquals("application/json", response.getContentType());
        JsonObject json = Json.parse(response.getPayload());

        // Using regex, because version depends on the build
        Assert.assertTrue(json.getString("pushScript").matches(
                "^\\./VAADIN/static/push/vaadinPush\\.js\\?v=[\\w\\.\\-]+$"));
    }

    private boolean hasNodeTag(TestNodeVisitor visitor, String htmContent, ElementType type) {
        Pattern regex = Pattern.compile(htmContent, Pattern.DOTALL);
        return visitor
                .getVisited()
                .entrySet()
                .stream()
                .anyMatch(entry -> {
                    return entry.getValue().equals(type) && regex.matcher(entry.getKey().toString()).find();
                });
    }

}
