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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.JavaScriptBootstrapUI;
import com.vaadin.flow.dom.NodeVisitor.ElementType;
import com.vaadin.flow.dom.TestNodeVisitor;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletResponse;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;


import elemental.json.Json;
import elemental.json.JsonObject;

@NotThreadSafe
public class JavaScriptBootstrapHandlerTest {

    private MockServletServiceSessionSetup mocks;

    private TestVaadinServletResponse response;
    private VaadinSession session;
    private JavaScriptBootstrapHandler jsInitHandler;

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
        Assert.assertTrue(json.getObject("appConfig").getBoolean("webComponentMode"));

        Assert.assertEquals("./", json.getObject("appConfig").getString("contextRootUrl"));
        Assert.assertEquals("//localhost:8888/foo/", json.getObject("appConfig").getString("serviceUrl"));
        Assert.assertEquals("http://localhost:8888/foo/", json.getObject("appConfig").getString("requestURL"));

        // Using regex, because version depends on the build
        Assert.assertTrue(json.getObject("appConfig").getString("pushScript")
                .matches("^\\./VAADIN/static/push/vaadinPush\\.js\\?v=[\\w\\.\\-]+$"));
    }

    @Test
    public void should_initialize_UI() throws Exception {
        VaadinRequest request = mocks.createRequest(mocks, "/foo/?v-r=init&foo");
        jsInitHandler.handleRequest(session, request, response);

        Assert.assertNotNull(UI.getCurrent());
        Assert.assertEquals(JavaScriptBootstrapUI.class, UI.getCurrent().getClass());
    }


    @Test
    public void should_attachViewToUI() throws Exception {
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
