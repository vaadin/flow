/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server.communication;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.Javascript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.hummingbird.util.JsonUtil;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.ui.Component;
import com.vaadin.ui.DependencyList;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIInternals.JavaScriptInvocation;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class UidlWriterTest {
    @Test
    public void testEncodeExecuteJavaScript() {
        Element element = ElementFactory.createDiv();

        JavaScriptInvocation invocation1 = new JavaScriptInvocation(
                "$0.focus()", element);
        JavaScriptInvocation invocation2 = new JavaScriptInvocation(
                "console.log($0, $1)", "Lives remaining:", Integer.valueOf(3));
        List<JavaScriptInvocation> executeJavaScriptList = Arrays
                .asList(invocation1, invocation2);

        JsonArray json = UidlWriter
                .encodeExecuteJavaScriptList(executeJavaScriptList);

        JsonArray expectedJson = JsonUtil.createArray(
                JsonUtil.createArray(
                        // Null since element is not attached
                        Json.createNull(), Json.create("$0.focus()")),
                JsonUtil.createArray(Json.create("Lives remaining:"),
                        Json.create(3), Json.create("console.log($0, $1)")));

        Assert.assertTrue(JsonUtil.jsonEquals(expectedJson, json));
    }

    @Tag("div")
    @Javascript("super-js")
    @StyleSheet("super-css")
    public static class SuperComponent extends Component {

    }

    public static class EmptyClassWithInterface extends SuperComponent
            implements AnotherComponentInterface {

    }

    @Javascript("js")
    @StyleSheet("css")
    public static class ActualComponent extends EmptyClassWithInterface
            implements ComponentInterface {

    }

    @Javascript({ "child1-js", "child2-js" })
    @StyleSheet({ "child1-css", "child2-css" })
    public static class ChildComponent extends ActualComponent {

    }

    @Javascript("interface-js")
    @StyleSheet("interface-css")
    public static interface ComponentInterface {

    }

    @Javascript("anotherinterface-js")
    @StyleSheet("anotherinterface-css")
    public static interface AnotherComponentInterface {

    }

    @Test
    public void testComponentDependencies() {
        UI ui = new UI();
        MockVaadinSession session = new MockVaadinSession(
                new VaadinServletService(new VaadinServlet(),
                        new MockDeploymentConfiguration()));
        session.lock();
        ui.getInternals().setSession(session);

        ui.add(new ActualComponent());

        UidlWriter uidlWriter = new UidlWriter();
        JsonObject response = uidlWriter.createUidl(ui, false);
        JsonArray dependencies = response
                .getArray(DependencyList.DEPENDENCY_KEY);
        Assert.assertEquals(8, dependencies.length());

        // super component's dependencies should be first, then the interfaces
        // and then the component
        assertDependency("super-", dependencies.get(0),
                DependencyList.TYPE_JAVASCRIPT);

        assertDependency("anotherinterface-", dependencies.get(1),
                DependencyList.TYPE_JAVASCRIPT);

        assertDependency("interface-", dependencies.get(2),
                DependencyList.TYPE_JAVASCRIPT);

        assertDependency("", dependencies.get(3),
                DependencyList.TYPE_JAVASCRIPT);

        assertDependency("super-", dependencies.get(4),
                DependencyList.TYPE_STYLESHEET);

        assertDependency("anotherinterface-", dependencies.get(5),
                DependencyList.TYPE_STYLESHEET);

        assertDependency("interface-", dependencies.get(6),
                DependencyList.TYPE_STYLESHEET);

        assertDependency("", dependencies.get(7),
                DependencyList.TYPE_STYLESHEET);

        // no dependencies should be send again
        ui.add(new ActualComponent(), new SuperComponent(),
                new ChildComponent());

        response = uidlWriter.createUidl(ui, false);
        dependencies = response.getArray(DependencyList.DEPENDENCY_KEY);
        Assert.assertEquals(4, dependencies.length());
        assertDependency("child1-", dependencies.getObject(0),
                DependencyList.TYPE_JAVASCRIPT);
        assertDependency("child2-", dependencies.getObject(1),
                DependencyList.TYPE_JAVASCRIPT);
        assertDependency("child1-", dependencies.getObject(2),
                DependencyList.TYPE_STYLESHEET);
        assertDependency("child2-", dependencies.getObject(3),
                DependencyList.TYPE_STYLESHEET);
    }

    private void assertDependency(String level, JsonObject jsonValue,
            String type) {
        Assert.assertEquals(level + type,
                jsonValue.get(DependencyList.KEY_URL).asString());
        Assert.assertEquals(type,
                jsonValue.get(DependencyList.KEY_TYPE).asString());
    }

}
