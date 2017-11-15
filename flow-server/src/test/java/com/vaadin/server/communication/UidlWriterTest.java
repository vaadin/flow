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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
import org.junit.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.View;
import com.vaadin.flow.util.JsonUtils;
import com.vaadin.router.RouterInterface;
import com.vaadin.server.DependencyFilter;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinUriResolverFactory;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIInternals.JavaScriptInvocation;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class UidlWriterTest {
    private static final String JS_TYPE_NAME = Dependency.Type.JAVASCRIPT
            .name();
    private static final String HTML_TYPE_NAME = Dependency.Type.HTML_IMPORT
            .name();
    private static final String CSS_STYLE_NAME = Dependency.Type.STYLESHEET
            .name();

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
    @JavaScript(value = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
            + "inline.js", loadMode = LoadMode.INLINE)
    @StyleSheet(value = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
            + "inline.css", loadMode = LoadMode.INLINE)
    @HtmlImport(value = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
            + "inline.html", loadMode = LoadMode.INLINE)
    public static class ComponentWithFrontendProtocol extends Component {
    }

    @Tag("base")
    @HtmlImport("2.html")
    public static class BaseClass extends Component implements View {
    }

    @Tag("parent")
    @HtmlImport("1.html")
    public static class ParentClass extends Component implements HasChildView {
        @Override
        public void setChildView(View childView) {
        }
    }

    @Tag("super-parent")
    @HtmlImport("0.html")
    public static class SuperParentClass extends Component
            implements HasChildView {
        @Override
        public void setChildView(View childView) {
        }
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

        JsonArray expectedJson = JsonUtils.createArray(JsonUtils.createArray(
                // Null since element is not attached
                Json.createNull(), Json.create("$0.focus()")),
                JsonUtils.createArray(Json.create("Lives remaining:"),
                        Json.create(3), Json.create("console.log($0, $1)")));

        assertTrue(JsonUtils.jsonEquals(expectedJson, json));
    }

    @Test
    public void testComponentDependencies() {
        UI ui = initializeUIForDependenciesTest(new TestUI());
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        // no dependencies should be resent in next response
        JsonObject response = uidlWriter.createUidl(ui, false);
        assertFalse(response.hasKey(LoadMode.EAGER.name()));
        assertFalse(response.hasKey(LoadMode.INLINE.name()));
        assertFalse(response.hasKey(LoadMode.LAZY.name()));
    }

    @Test
    public void testComponentInterfaceDependencies() {
        UI ui = initializeUIForDependenciesTest(new TestUI());
        UidlWriter uidlWriter = new UidlWriter();

        addInitialComponentDependencies(ui, uidlWriter);

        // test that dependencies only from new child interfaces are added
        ui.add(new ActualComponent(), new SuperComponent(),
                new ChildComponent());

        JsonObject response = uidlWriter.createUidl(ui, false);
        Map<String, JsonObject> dependenciesMap = getDependenciesMap(response);

        assertEquals(12, dependenciesMap.size());
        assertDependency("childinterface1-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                dependenciesMap);
        assertDependency("childinterface2-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                dependenciesMap);
        assertDependency("child1-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                dependenciesMap);
        assertDependency("child2-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                dependenciesMap);
        assertDependency("childinterface1-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("childinterface2-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("child1-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("child2-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("childinterface1-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);
        assertDependency("childinterface2-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);
        assertDependency("child1-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);
        assertDependency("child2-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);
    }

    @Test
    public void checkAllTypesOfDependencies() {
        UI ui = initializeUIForDependenciesTest(new TestUI());
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
        assertThat("Should have 3 different eager urls", eagerDependencies
                .stream().map(json -> json.getString(Dependency.KEY_URL))
                .map(url -> url.substring(
                        ApplicationConstants.FRONTEND_PROTOCOL_PREFIX.length()))
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
        assertThat("Should have 3 different lazy urls", lazyDependencies
                .stream().map(json -> json.getString(Dependency.KEY_URL))
                .map(url -> url.substring(
                        ApplicationConstants.FRONTEND_PROTOCOL_PREFIX.length()))
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
        assertInlineDependencies(inlineDependencies,
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);
    }

    @Test
    public void checkAllTypesOfDependencies_uriResolverResolvesFrontendProtocol() {
        UI ui = initializeUIForDependenciesTest(new TestUI());
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        doAnswer(invocation -> {
            String path = (String) invocation.getArguments()[1];
            if (path.startsWith(
                    ApplicationConstants.FRONTEND_PROTOCOL_PREFIX)) {
                return path.substring(
                        ApplicationConstants.FRONTEND_PROTOCOL_PREFIX.length());
            }
            return path;
        }).when(factory).toServletContextPath(any(), anyString());

        ui.add(new ComponentWithFrontendProtocol());
        JsonObject response = uidlWriter.createUidl(ui, false);
        List<JsonObject> inlineDependencies = JsonUtils
                .<JsonObject> stream(response.getArray(LoadMode.INLINE.name()))
                .collect(Collectors.toList());

        assertInlineDependencies(inlineDependencies, "");
    }

    @Test
    public void parentViewDependenciesAreAddedFirst() {
        UI ui = initializeUIForDependenciesTest(new UI());
        UidlWriter uidlWriter = new UidlWriter();
        ui.add(new BaseClass());

        JsonObject response = uidlWriter.createUidl(ui, false);

        assertFalse("Did not expect to have lazy dependencies in uidl",
                response.hasKey(LoadMode.LAZY.name()));
        assertFalse("Did not expect to have inline dependencies in uidl",
                response.hasKey(LoadMode.INLINE.name()));
        assertTrue("Expected to have eager dependencies in uidl",
                response.hasKey(LoadMode.EAGER.name()));

        JsonArray eagerDependencies = response.getArray(LoadMode.EAGER.name());
        assertEquals(
                "Expected to have exactly 3 eager dependencies in uidl, actual: %d",
                eagerDependencies.length(), 3);

        List<Class<?>> expectedClassOrder = Arrays.asList(
                SuperParentClass.class, ParentClass.class, BaseClass.class);

        for (int i = 0; i < expectedClassOrder.size(); i++) {
            Class<?> expectedClass = expectedClassOrder.get(i);
            HtmlImport htmlImport = expectedClass
                    .getAnnotation(HtmlImport.class);

            JsonValue actualDependency = eagerDependencies.get(i);
            JsonObject expectedDependency = new Dependency(
                    Dependency.Type.HTML_IMPORT, htmlImport.value(),
                    htmlImport.loadMode()).toJson();
            assertTrue(String.format(
                    "Unexpected dependency. Expected: '%s', actual: '%s', class: '%s'",
                    expectedDependency, actualDependency, expectedClass),
                    expectedDependency.jsEquals(actualDependency));
        }
    }

    private void assertInlineDependencies(List<JsonObject> inlineDependencies,
            String expectedPrefix) {
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
                        .map(url -> {
                            if (!url.startsWith(expectedPrefix)) {
                                throw new AssertionError(
                                        url + " should have the prefix "
                                                + expectedPrefix);
                            } else {
                                return url.substring(expectedPrefix.length());
                            }
                        }).collect(Collectors.toList()),
                containsInAnyOrder("inline.js", "inline.html", "inline.css"));
        assertThat("Should have 3 different inline dependency type",
                inlineDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_TYPE))
                        .map(Dependency.Type::valueOf)
                        .collect(Collectors.toList()),
                containsInAnyOrder(Dependency.Type.values()));
    }

    private UI initializeUIForDependenciesTest(UI ui) {
        VaadinServletService service = new VaadinServletService(
                new VaadinServlet(), new MockDeploymentConfiguration()) {
            RouterInterface router = new com.vaadin.flow.router.Router();

            @Override
            public RouterInterface getRouter() {
                return router;
            }

            @Override
            public Iterable<DependencyFilter> getDependencyFilters() {
                return Collections.emptyList();
            }
        };

        service.getRouter().reconfigure(conf -> {
            conf.setRoute("", BaseClass.class);
            conf.setParentView(BaseClass.class, ParentClass.class);
            conf.setParentView(ParentClass.class, SuperParentClass.class);
        });

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

        factory = mock(VaadinUriResolverFactory.class);
        doAnswer(invocation -> invocation.getArguments()[1]).when(factory)
                .toServletContextPath(any(), anyString());

        session.setAttribute(VaadinUriResolverFactory.class, factory);

        VaadinSession.setCurrent(session);

        return ui;
    }

    private void addInitialComponentDependencies(UI ui, UidlWriter uidlWriter) {
        ui.add(new ActualComponent());

        JsonObject response = uidlWriter.createUidl(ui, false);
        Map<String, JsonObject> dependenciesMap = getDependenciesMap(response);
        assertEquals(15, dependenciesMap.size());

        // UI parent first, then UI, then super component's dependencies, then
        // the interfaces and then the component
        assertDependency("UI-parent-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("UI-" + JS_TYPE_NAME, JS_TYPE_NAME, dependenciesMap);

        assertDependency("super-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                dependenciesMap);
        assertDependency("anotherinterface-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                dependenciesMap);
        assertDependency("interface-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                dependenciesMap);
        assertDependency(HTML_TYPE_NAME, HTML_TYPE_NAME, dependenciesMap);

        assertDependency("super-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("anotherinterface-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("interface-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency(JS_TYPE_NAME, JS_TYPE_NAME, dependenciesMap);

        assertDependency("super-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);

        assertDependency("anotherinterface-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);

        assertDependency("interface-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);

        assertDependency(CSS_STYLE_NAME, CSS_STYLE_NAME, dependenciesMap);

        assertDependency("0.html", HTML_TYPE_NAME, dependenciesMap);
    }

    private Map<String, JsonObject> getDependenciesMap(JsonObject response) {
        return Stream.of(LoadMode.values())
                .map(mode -> response.getArray(mode.name()))
                .flatMap(JsonUtils::<JsonObject> stream)
                .collect(Collectors.toMap(
                        jsonObject -> jsonObject.getString(Dependency.KEY_URL),
                        Function.identity()));
    }

    private void assertDependency(String url, String type,
            Map<String, JsonObject> dependenciesMap) {
        url = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + url;
        JsonObject jsonValue = dependenciesMap.get(url);
        assertNotNull(
                "Expected dependencies map to have dependency with key=" + url,
                jsonValue);
        assertEquals(url, jsonValue.get(Dependency.KEY_URL).asString());
        assertEquals(type, jsonValue.get(Dependency.KEY_TYPE).asString());
    }

}
