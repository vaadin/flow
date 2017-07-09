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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
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
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinUriResolverFactory;
import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIInternals.JavaScriptInvocation;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class UidlWriterTest {

    private static final String FRONTEND = "frontend://";

    @JavaScript("UI-parent-JAVASCRIPT")
    private static class ParentUI extends UI {
    }

    @JavaScript("UI-JAVASCRIPT")
    private static class TestUI extends ParentUI {
    }

    @Tag("div")
    @JavaScript("super-JAVASCRIPT")
    @StyleSheet("super-STYLESHEET")
    @HtmlImport("super-HTML_IMPORT")
    public static class SuperComponent extends Component {
    }

    public static class EmptyClassWithInterface extends SuperComponent
            implements AnotherComponentInterface {
    }

    @JavaScript("JAVASCRIPT")
    @StyleSheet("STYLESHEET")
    @HtmlImport("HTML_IMPORT")
    public static class ActualComponent extends EmptyClassWithInterface
            implements ComponentInterface {
    }

    @JavaScript("child1-JAVASCRIPT")
    @JavaScript("child2-JAVASCRIPT")
    @StyleSheet("child1-STYLESHEET")
    @StyleSheet("child2-STYLESHEET")
    @HtmlImport("child1-HTML_IMPORT")
    @HtmlImport("child2-HTML_IMPORT")
    public static class ChildComponent extends ActualComponent
            implements ChildComponentInterface2 {
    }

    @JavaScript("interface-JAVASCRIPT")
    @StyleSheet("interface-STYLESHEET")
    @HtmlImport("interface-HTML_IMPORT")
    public interface ComponentInterface {
    }

    @JavaScript("anotherinterface-JAVASCRIPT")
    @StyleSheet("anotherinterface-STYLESHEET")
    @HtmlImport("anotherinterface-HTML_IMPORT")
    public interface AnotherComponentInterface {
    }

    @JavaScript("childinterface1-JAVASCRIPT")
    @StyleSheet("childinterface1-STYLESHEET")
    @HtmlImport("childinterface1-HTML_IMPORT")
    public interface ChildComponentInterface1 {
    }

    @JavaScript("childinterface2-JAVASCRIPT")
    @StyleSheet("childinterface2-STYLESHEET")
    @HtmlImport("childinterface2-HTML_IMPORT")
    public interface ChildComponentInterface2 extends ChildComponentInterface1 {
    }

    @Tag("test")
    @JavaScript(value = "lazy.js", loadMode = LoadMode.LAZY)
    @StyleSheet(value = "lazy.css", loadMode = LoadMode.LAZY)
    @HtmlImport(value = "lazy.html", loadMode = LoadMode.LAZY)
    @JavaScript(value = "inline.js", loadMode = LoadMode.INLINE)
    @StyleSheet(value = "inline.css", loadMode = LoadMode.INLINE)
    @HtmlImport(value = "inline.html", loadMode = LoadMode.INLINE)
    @JavaScript("eager.js")
    @StyleSheet("eager.css")
    @HtmlImport("eager.html")
    public static class ComponentWithAllDependencyTypes extends Component {
    }

    @Tag("test")
    @JavaScript(value = FRONTEND + "inline.js", loadMode = LoadMode.INLINE)
    @StyleSheet(value = FRONTEND + "inline.css", loadMode = LoadMode.INLINE)
    @HtmlImport(value = FRONTEND + "inline.html", loadMode = LoadMode.INLINE)
    public static class ComponentWithFrontendProtocol extends Component {
    }

    private VaadinUriResolverFactory factory;

    @After
    public void tearDown() {
        VaadinService.setCurrent(null);
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

    @Test
    public void testComponentDependencies() {
        UI ui = initializeUIForDependenciesTest();
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        // no dependencies should be resent in next response
        JsonObject response = uidlWriter.createUidl(ui, false);
        Assert.assertFalse(response.hasKey(LoadMode.EAGER.name()));
        Assert.assertFalse(response.hasKey(LoadMode.INLINE.name()));
        Assert.assertFalse(response.hasKey(LoadMode.LAZY.name()));
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
        assertDependency("childinterface1-", Dependency.Type.HTML_IMPORT.name(),
                dependenciesMap);
        assertDependency("childinterface2-", Dependency.Type.HTML_IMPORT.name(),
                dependenciesMap);
        assertDependency("child1-", Dependency.Type.HTML_IMPORT.name(),
                dependenciesMap);
        assertDependency("child2-", Dependency.Type.HTML_IMPORT.name(),
                dependenciesMap);
        assertDependency("childinterface1-", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);
        assertDependency("childinterface2-", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);
        assertDependency("child1-", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);
        assertDependency("child2-", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);
        assertDependency("childinterface1-", Dependency.Type.STYLESHEET.name(),
                dependenciesMap);
        assertDependency("childinterface2-", Dependency.Type.STYLESHEET.name(),
                dependenciesMap);
        assertDependency("child1-", Dependency.Type.STYLESHEET.name(),
                dependenciesMap);
        assertDependency("child2-", Dependency.Type.STYLESHEET.name(),
                dependenciesMap);
    }

    @Test
    public void checkAllTypesOfDependencies() {
        UI ui = initializeUIForDependenciesTest();
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        ui.add(new ComponentWithAllDependencyTypes());
        JsonObject response = uidlWriter.createUidl(ui, false);
        Map<LoadMode, List<JsonObject>> dependenciesMap = Stream
                .of(LoadMode.values())
                .map(mode -> response.getArray(mode.name()))
                .flatMap(JsonUtils::<JsonObject> stream)
                .collect(Collectors.toMap(
                        jsonObject -> LoadMode.valueOf(
                                jsonObject.getString(Dependency.KEY_LOAD_MODE)),
                        Collections::singletonList, (list1, list2) -> {
                            List<JsonObject> result = new ArrayList<>(list1);
                            result.addAll(list2);
                            return result;
                        }));

        assertThat(
                "Dependencies with all types of load mode should be present in this response",
                dependenciesMap.size(), is(LoadMode.values().length));

        List<JsonObject> eagerDependencies = dependenciesMap
                .get(LoadMode.EAGER);
        assertThat("Should have 3 eager dependencies", eagerDependencies,
                hasSize(3));
        assertThat("Eager dependencies should not have inline contents",
                eagerDependencies.stream()
                        .filter(json -> json.hasKey(Dependency.KEY_CONTENTS))
                        .collect(Collectors.toList()),
                hasSize(0));
        assertThat("Should have 3 different eager urls",
                eagerDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_URL))
                        .collect(Collectors.toList()),
                containsInAnyOrder("eager.js", "eager.html", "eager.css"));
        assertThat("Should have 3 different eager dependency types",
                eagerDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_TYPE))
                        .map(Dependency.Type::valueOf)
                        .collect(Collectors.toList()),
                containsInAnyOrder(Dependency.Type.values()));

        List<JsonObject> lazyDependencies = dependenciesMap.get(LoadMode.LAZY);
        assertThat("Should have 3 lazy dependencies", lazyDependencies,
                hasSize(3));
        assertThat("Lazy dependencies should not have inline contents",
                lazyDependencies.stream()
                        .filter(json -> json.hasKey(Dependency.KEY_CONTENTS))
                        .collect(Collectors.toList()),
                hasSize(0));
        assertThat("Should have 3 different lazy urls",
                lazyDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_URL))
                        .collect(Collectors.toList()),
                containsInAnyOrder("lazy.js", "lazy.html", "lazy.css"));
        assertThat("Should have 3 different lazy dependency types",
                lazyDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_TYPE))
                        .map(Dependency.Type::valueOf)
                        .collect(Collectors.toList()),
                containsInAnyOrder(Dependency.Type.values()));

        List<JsonObject> inlineDependencies = dependenciesMap
                .get(LoadMode.INLINE);
        assertInlineDependencies(inlineDependencies);
    }

    @Test
    public void checkAllTypesOfDependencies_uriResolverResolvesFrontentProtocol() {
        UI ui = initializeUIForDependenciesTest();
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        Mockito.doAnswer(invocation -> {
            String path = (String) invocation.getArguments()[1];
            if (path.startsWith(FRONTEND)) {
                return path.substring(FRONTEND.length());
            }
            return path;
        }).when(factory).toServletContextPath(Mockito.any(),
                Mockito.anyString());

        ui.add(new ComponentWithFrontendProtocol());
        JsonObject response = uidlWriter.createUidl(ui, false);
        List<JsonObject> inlineDependencies = JsonUtils
                .<JsonObject> stream(response.getArray(LoadMode.INLINE.name()))
                .collect(Collectors.toList());

        assertInlineDependencies(inlineDependencies);
    }

    private void assertInlineDependencies(List<JsonObject> inlineDependencies) {
        assertThat("Should have 3 inline dependencies", inlineDependencies,
                hasSize(3));
        assertThat("Eager dependencies should not have urls",
                inlineDependencies.stream()
                        .filter(json -> json.hasKey(Dependency.KEY_URL))
                        .collect(Collectors.toList()),
                hasSize(0));
        assertThat("Should have 3 different inline contents",
                inlineDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_CONTENTS))
                        .collect(Collectors.toList()),
                containsInAnyOrder("inline.js", "inline.html", "inline.css"));
        assertThat("Should have 3 different inline dependency type",
                inlineDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_TYPE))
                        .map(Dependency.Type::valueOf)
                        .collect(Collectors.toList()),
                containsInAnyOrder(Dependency.Type.values()));
    }

    private UI initializeUIForDependenciesTest() {
        UI ui = new TestUI();
        VaadinServletService service = new VaadinServletService(
                new VaadinServlet(), new MockDeploymentConfiguration());
        MockVaadinSession session = new MockVaadinSession(service);
        session.lock();
        ui.getInternals().setSession(session);

        ServletContext servletContextMock = mock(ServletContext.class);
        when(servletContextMock.getResourceAsStream(anyString()))
                .thenAnswer(invocation -> new ByteArrayInputStream(
                        ((String) invocation.getArguments()[0]).getBytes()));

        HttpServletRequest servletRequestMock = mock(HttpServletRequest.class);
        when(servletRequestMock.getServletContext())
                .thenReturn(servletContextMock);

        VaadinServletRequest vaadinRequestMock = mock(
                VaadinServletRequest.class);
        when(vaadinRequestMock.getHttpServletRequest())
                .thenReturn(servletRequestMock);

        service.setCurrentInstances(vaadinRequestMock,
                mock(VaadinResponse.class));
        ui.doInit(vaadinRequestMock, 1);

        factory = Mockito.mock(VaadinUriResolverFactory.class);
        Mockito.doAnswer(invocation -> invocation.getArguments()[1])
                .when(factory)
                .toServletContextPath(Mockito.any(), Mockito.anyString());

        session.setAttribute(VaadinUriResolverFactory.class, factory);

        VaadinSession.setCurrent(session);

        return ui;
    }

    private void addInitialComponentDependencies(UI ui, UidlWriter uidlWriter) {
        ui.add(new ActualComponent());

        JsonObject response = uidlWriter.createUidl(ui, false);
        Map<String, JsonObject> dependenciesMap = getDependenciesMap(response);
        Assert.assertEquals(14, dependenciesMap.size());

        // UI parent first, then UI, then super component's dependencies, then
        // the interfaces and then the component
        assertDependency("UI-parent-", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);
        assertDependency("UI-", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);

        assertDependency("super-", Dependency.Type.HTML_IMPORT.name(),
                dependenciesMap);
        assertDependency("anotherinterface-",
                Dependency.Type.HTML_IMPORT.name(), dependenciesMap);
        assertDependency("interface-", Dependency.Type.HTML_IMPORT.name(),
                dependenciesMap);
        assertDependency("", Dependency.Type.HTML_IMPORT.name(),
                dependenciesMap);

        assertDependency("super-", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);
        assertDependency("anotherinterface-", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);
        assertDependency("interface-", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);
        assertDependency("", Dependency.Type.JAVASCRIPT.name(),
                dependenciesMap);

        assertDependency("super-", Dependency.Type.STYLESHEET.name(),
                dependenciesMap);

        assertDependency("anotherinterface-", Dependency.Type.STYLESHEET.name(),
                dependenciesMap);

        assertDependency("interface-", Dependency.Type.STYLESHEET.name(),
                dependenciesMap);

        assertDependency("", Dependency.Type.STYLESHEET.name(),
                dependenciesMap);
    }

    private Map<String, JsonObject> getDependenciesMap(JsonObject response) {
        return Stream.of(LoadMode.values())
                .map(mode -> response.getArray(mode.name()))
                .flatMap(JsonUtils::<JsonObject> stream)
                .collect(Collectors.toMap(
                        jsonObject -> jsonObject.getString(Dependency.KEY_URL),
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
                jsonValue.get(Dependency.KEY_URL).asString());
        Assert.assertEquals(type,
                jsonValue.get(Dependency.KEY_TYPE).asString());
    }

}
