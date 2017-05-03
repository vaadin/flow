/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.util.JsonUtils;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinRequest;
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
    @JavaScript("UI-parent-js")
    private static class ParentUI extends UI {

    }

    @JavaScript("UI-js")
    private static class TestUI extends ParentUI {

    }

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

        JsonArray expectedJson = JsonUtils.createArray(
                JsonUtils.createArray(
                        // Null since element is not attached
                        Json.createNull(), Json.create("$0.focus()")),
                JsonUtils.createArray(Json.create("Lives remaining:"),
                        Json.create(3), Json.create("console.log($0, $1)")));

        Assert.assertTrue(JsonUtils.jsonEquals(expectedJson, json));
    }

    @Tag("div")
    @JavaScript("super-js")
    @StyleSheet("super-css")
    @HtmlImport("super-html")
    public static class SuperComponent extends Component {

    }

    public static class EmptyClassWithInterface extends SuperComponent
            implements AnotherComponentInterface {

    }

    @JavaScript("js")
    @StyleSheet("css")
    @HtmlImport("html")
    public static class ActualComponent extends EmptyClassWithInterface
            implements ComponentInterface {

    }

    @JavaScript("child1-js")
    @JavaScript("child2-js")
    @StyleSheet("child1-css")
    @StyleSheet("child2-css")
    @HtmlImport("child1-html")
    @HtmlImport("child2-html")
    public static class ChildComponent extends ActualComponent
            implements ChildComponentInterface2 {

    }

    @JavaScript("interface-js")
    @StyleSheet("interface-css")
    @HtmlImport("interface-html")
    public interface ComponentInterface {

    }

    @JavaScript("anotherinterface-js")
    @StyleSheet("anotherinterface-css")
    @HtmlImport("anotherinterface-html")
    public interface AnotherComponentInterface {

    }

    @JavaScript("childinterface1-js")
    @StyleSheet("childinterface1-css")
    @HtmlImport("childinterface1-html")
    public interface ChildComponentInterface1 {

    }

    @JavaScript("childinterface2-js")
    @StyleSheet("childinterface2-css")
    @HtmlImport("childinterface2-html")
    public interface ChildComponentInterface2
            extends ChildComponentInterface1 {

    }

    @Test
    public void testComponentDependencies() {
        UI ui = initializeUIForDependenciesTest();
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        // no dependencies should be resent in next response
        JsonObject response = uidlWriter.createUidl(ui, false);
        Assert.assertFalse(response.hasKey(DependencyList.DEPENDENCY_KEY));
    }

    private UI initializeUIForDependenciesTest() {
        UI ui = new TestUI();
        MockVaadinSession session = new MockVaadinSession(
                new VaadinServletService(new VaadinServlet(),
                        new MockDeploymentConfiguration()));
        session.lock();
        ui.getInternals().setSession(session);

        ui.doInit(Mockito.mock(VaadinRequest.class), 1);
        return ui;
    }

    private void addInitialComponentDependencies(UI ui, UidlWriter uidlWriter) {
        ui.add(new ActualComponent());

        JsonObject response = uidlWriter.createUidl(ui, false);
        Map<String, JsonObject> dependenciesMap = getDependenciesMap(response);
        Assert.assertEquals(14, dependenciesMap.size());

        // UI parent first, then UI, then super component's dependencies, then
        // the interfaces and then the component
        assertDependency("UI-parent-", DependencyList.TYPE_JAVASCRIPT,
                dependenciesMap);
        assertDependency("UI-", DependencyList.TYPE_JAVASCRIPT,
                dependenciesMap);

        assertDependency("super-", DependencyList.TYPE_HTML_IMPORT,
                dependenciesMap);
        assertDependency("anotherinterface-", DependencyList.TYPE_HTML_IMPORT,
                dependenciesMap);
        assertDependency("interface-", DependencyList.TYPE_HTML_IMPORT,
                dependenciesMap);
        assertDependency("", DependencyList.TYPE_HTML_IMPORT, dependenciesMap);

        assertDependency("super-", DependencyList.TYPE_JAVASCRIPT,
                dependenciesMap);
        assertDependency("anotherinterface-", DependencyList.TYPE_JAVASCRIPT,
                dependenciesMap);
        assertDependency("interface-", DependencyList.TYPE_JAVASCRIPT,
                dependenciesMap);
        assertDependency("", DependencyList.TYPE_JAVASCRIPT, dependenciesMap);

        assertDependency("super-", DependencyList.TYPE_STYLESHEET,
                dependenciesMap);

        assertDependency("anotherinterface-", DependencyList.TYPE_STYLESHEET,
                dependenciesMap);

        assertDependency("interface-", DependencyList.TYPE_STYLESHEET,
                dependenciesMap);

        assertDependency("", DependencyList.TYPE_STYLESHEET, dependenciesMap);
    }

    @Test
    public void testComponentInterfaceDependencies() {
        UI ui = initializeUIForDependenciesTest();
        UidlWriter uidlWriter = new UidlWriter();

        addInitialComponentDependencies(ui, uidlWriter);

        // test that dependencies only from new child interfaces are added
        ui.add(new ActualComponent(), new SuperComponent(),
                new ChildComponent());

        JsonObject response = uidlWriter.createUidl(ui, false);
        Map<String, JsonObject> dependenciesMap = getDependenciesMap(response);

        Assert.assertEquals(12, dependenciesMap.size());
        assertDependency("childinterface1-", DependencyList.TYPE_HTML_IMPORT,
                dependenciesMap);
        assertDependency("childinterface2-", DependencyList.TYPE_HTML_IMPORT,
                dependenciesMap);
        assertDependency("child1-", DependencyList.TYPE_HTML_IMPORT,
                dependenciesMap);
        assertDependency("child2-", DependencyList.TYPE_HTML_IMPORT,
                dependenciesMap);
        assertDependency("childinterface1-", DependencyList.TYPE_JAVASCRIPT,
                dependenciesMap);
        assertDependency("childinterface2-", DependencyList.TYPE_JAVASCRIPT,
                dependenciesMap);
        assertDependency("child1-", DependencyList.TYPE_JAVASCRIPT,
                dependenciesMap);
        assertDependency("child2-", DependencyList.TYPE_JAVASCRIPT,
                dependenciesMap);
        assertDependency("childinterface1-", DependencyList.TYPE_STYLESHEET,
                dependenciesMap);
        assertDependency("childinterface2-", DependencyList.TYPE_STYLESHEET,
                dependenciesMap);
        assertDependency("child1-", DependencyList.TYPE_STYLESHEET,
                dependenciesMap);
        assertDependency("child2-", DependencyList.TYPE_STYLESHEET,
                dependenciesMap);
    }

    private Map<String, JsonObject> getDependenciesMap(JsonObject response) {
        JsonArray dependencies = response
                .getArray(DependencyList.DEPENDENCY_KEY);
        return IntStream.range(0, dependencies.length())
                .mapToObj(dependencies::getObject)
                .collect(Collectors.toMap(
                        jsonObject -> jsonObject.getString(DependencyList.KEY_URL),
                        Function.identity()));
    }

    private void assertDependency(String level, String type,
            Map<String, JsonObject> dependenciesMap) {
        String url = level + type;
        JsonObject jsonValue = dependenciesMap.get(url);
        Assert.assertNotNull(
                "Expected dependencies map to have dependency with key=" + url,
                jsonValue);
        Assert.assertEquals(level + type,
                jsonValue.get(DependencyList.KEY_URL).asString());
        Assert.assertEquals(type,
                jsonValue.get(DependencyList.KEY_TYPE).asString());
    }

}
