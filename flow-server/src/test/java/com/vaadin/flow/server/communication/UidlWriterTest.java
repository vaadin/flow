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

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jcip.annotations.NotThreadSafe;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NotThreadSafe
public class UidlWriterTest {
    private static final String JS_TYPE_NAME = Dependency.Type.JAVASCRIPT
            .name();
    private static final String JS_MODULE_NAME = Dependency.Type.JS_MODULE
            .name();
    private static final String HTML_TYPE_NAME = Dependency.Type.HTML_IMPORT
            .name();
    private static final String CSS_STYLE_NAME = Dependency.Type.STYLESHEET
            .name();
    private MockServletServiceSessionSetup mocks;

    @JavaScript("UI-parent-JAVASCRIPT")
    @JsModule("UI-parent-JS_MODULE")
    private static class ParentUI extends UI {
    }

    @JavaScript("UI-JAVASCRIPT")
    @JsModule("UI-JS_MODULE")
    private static class TestUI extends ParentUI {
    }

    @Tag("div")
    @JavaScript("super-JAVASCRIPT")
    @JsModule("super-JS_MODULE")
    @StyleSheet("super-STYLESHEET")
    @HtmlImport("super-HTML_IMPORT")
    public static class SuperComponent extends Component {
    }

    public static class EmptyClassWithInterface extends SuperComponent
            implements AnotherComponentInterface {
    }

    @JavaScript("JAVASCRIPT")
    @JsModule("JS_MODULE")
    @StyleSheet("STYLESHEET")
    @HtmlImport("HTML_IMPORT")
    public static class ActualComponent extends EmptyClassWithInterface
            implements ComponentInterface {
    }

    @JavaScript("child1-JAVASCRIPT")
    @JavaScript("child2-JAVASCRIPT")
    @JsModule("child1-JS_MODULE")
    @JsModule("child2-JS_MODULE")
    @StyleSheet("child1-STYLESHEET")
    @StyleSheet("child2-STYLESHEET")
    @HtmlImport("child1-HTML_IMPORT")
    @HtmlImport("child2-HTML_IMPORT")
    public static class ChildComponent extends ActualComponent
            implements ChildComponentInterface2 {
    }

    @JavaScript("interface-JAVASCRIPT")
    @JsModule("interface-JS_MODULE")
    @StyleSheet("interface-STYLESHEET")
    @HtmlImport("interface-HTML_IMPORT")
    public interface ComponentInterface {
    }

    @JavaScript("anotherinterface-JAVASCRIPT")
    @JsModule("anotherinterface-JS_MODULE")
    @StyleSheet("anotherinterface-STYLESHEET")
    @HtmlImport("anotherinterface-HTML_IMPORT")
    public interface AnotherComponentInterface {
    }

    @JavaScript("childinterface1-JAVASCRIPT")
    @JsModule("childinterface1-JS_MODULE")
    @StyleSheet("childinterface1-STYLESHEET")
    @HtmlImport("childinterface1-HTML_IMPORT")
    public interface ChildComponentInterface1 {
    }

    @JavaScript("childinterface2-JAVASCRIPT")
    @JsModule("childinterface2-JS_MODULE")
    @StyleSheet("childinterface2-STYLESHEET")
    @HtmlImport("childinterface2-HTML_IMPORT")
    public interface ChildComponentInterface2 extends ChildComponentInterface1 {
    }

    @Tag("test")
    @JavaScript(value = "lazy.js", loadMode = LoadMode.LAZY)
    @JsModule(value = "lazy.mjs", loadMode = LoadMode.LAZY)
    @StyleSheet(value = "lazy.css", loadMode = LoadMode.LAZY)
    @HtmlImport(value = "lazy.html", loadMode = LoadMode.LAZY)
    @JavaScript(value = "inline.js", loadMode = LoadMode.INLINE)
    @JsModule(value = "inline.mjs", loadMode = LoadMode.INLINE)
    @StyleSheet(value = "inline.css", loadMode = LoadMode.INLINE)
    @HtmlImport(value = "inline.html", loadMode = LoadMode.INLINE)
    @JavaScript("eager.js")
    @JsModule("eager.mjs")
    @StyleSheet("eager.css")
    @HtmlImport("eager.html")
    public static class ComponentWithAllDependencyTypes extends Component {
    }

    @Tag("test")
    @JavaScript(value = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
            + "inline.js", loadMode = LoadMode.INLINE)
    @JsModule(value = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
            + "inline.mjs", loadMode = LoadMode.INLINE)
    @StyleSheet(value = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
            + "inline.css", loadMode = LoadMode.INLINE)
    @HtmlImport(value = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
            + "inline.html", loadMode = LoadMode.INLINE)
    public static class ComponentWithFrontendProtocol extends Component {
    }

    @Tag("base")
    @HtmlImport("2.html")
    @Route(value = "", layout = ParentClass.class)
    public static class BaseClass extends Component {
    }

    @Tag("parent")
    @HtmlImport("1.html")
    @ParentLayout(SuperParentClass.class)
    public static class ParentClass extends Component implements RouterLayout {
    }

    @Tag("super-parent")
    @HtmlImport("0.html")
    public static class SuperParentClass extends Component
            implements RouterLayout {
    }

    private UI ui;

    @Before
    public void init() throws Exception {
        ui = initializeUIForDependenciesTest(new TestUI());
    }

    @After
    public void tearDown() {
        if (mocks != null) {
            mocks.cleanup();
        }
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
                Json.createNull(), Json.create("$0.focus()")), JsonUtils
                .createArray(Json.create("Lives remaining:"), Json.create(3),
                        Json.create("console.log($0, $1)")));

        assertTrue(JsonUtils.jsonEquals(expectedJson, json));
    }

    @Test
    public void componentDependencies() {
        mocks.getDeploymentConfiguration().setBowerMode(true);
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
        mocks.getDeploymentConfiguration().setBowerMode(true);
        assertComponentInterfaceDependencies();
    }

    @Test
    public void testNpmComponentInterfaceDependencies() {
        mocks.getDeploymentConfiguration().setBowerMode(false);
        assertComponentInterfaceDependencies();
    }

    private void assertComponentInterfaceDependencies() {
        UidlWriter uidlWriter = new UidlWriter();

        addInitialComponentDependencies(ui, uidlWriter);

        // test that dependencies only from new child interfaces are added
        ui.add(new ActualComponent(), new SuperComponent(),
                new ChildComponent());

        JsonObject response = uidlWriter.createUidl(ui, false);
        Map<String, JsonObject> dependenciesMap = getDependenciesMap(response);

        assertEquals(12, dependenciesMap.size());
        assertDependency("childinterface1-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("childinterface2-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("child1-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("child2-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        if (mocks.getDeploymentConfiguration().isBowerMode()) {
            assertDependency("childinterface1-" + HTML_TYPE_NAME,
                    HTML_TYPE_NAME, dependenciesMap);
            assertDependency("childinterface2-" + HTML_TYPE_NAME,
                    HTML_TYPE_NAME, dependenciesMap);
            assertDependency("child1-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                    dependenciesMap);
            assertDependency("child2-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                    dependenciesMap);
        } else {
            assertDependency("childinterface1-" + JS_MODULE_NAME,
                    JS_MODULE_NAME, dependenciesMap);
            assertDependency("childinterface2-" + JS_MODULE_NAME,
                    JS_MODULE_NAME, dependenciesMap);
            assertDependency("child1-" + JS_MODULE_NAME, JS_MODULE_NAME,
                    dependenciesMap);
            assertDependency("child2-" + JS_MODULE_NAME, JS_MODULE_NAME,
                    dependenciesMap);
        }
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
        mocks.getDeploymentConfiguration().setBowerMode(true);
        assertAllDependencyTypes();
    }

    @Test
    public void checkAllNpmTypesOfDependencies() {
        mocks.getDeploymentConfiguration().setBowerMode(false);
        assertAllDependencyTypes();
    }

    private void assertAllDependencyTypes() {
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        ui.add(new ComponentWithAllDependencyTypes());
        JsonObject response = uidlWriter.createUidl(ui, false);
        Map<LoadMode, List<JsonObject>> dependenciesMap = Stream
                .of(LoadMode.values())
                .map(mode -> response.getArray(mode.name()))
                .flatMap(JsonUtils::<JsonObject>stream).collect(Collectors
                        .toMap(jsonObject -> LoadMode.valueOf(
                                jsonObject.getString(Dependency.KEY_LOAD_MODE)),
                                Collections::singletonList, (list1, list2) -> {
                                    List<JsonObject> result = new ArrayList<>(
                                            list1);
                                    result.addAll(list2);
                                    return result;
                                }));

        assertThat(
                "Dependencies with all types of load mode should be present in this response",
                dependenciesMap.size(), is(LoadMode.values().length));

        Matcher<Iterable<? extends String>> matcher;
        if (mocks.getDeploymentConfiguration().isBowerMode()) {
            matcher = containsInAnyOrder("eager.js", "eager.html", "eager.css");
        } else {
            matcher = containsInAnyOrder("eager.js", "eager.mjs", "eager.css");
        }

        List<JsonObject> eagerDependencies = dependenciesMap
                .get(LoadMode.EAGER);
        assertThat("Should have 3 eager dependencies", eagerDependencies,
                hasSize(3));
        assertThat("Eager dependencies should not have inline contents",
                eagerDependencies.stream()
                        .filter(json -> json.hasKey(Dependency.KEY_CONTENTS))
                        .collect(Collectors.toList()), hasSize(0));
        assertThat("Should have 3 different eager urls",
                eagerDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_URL))
                        .map(url -> {
                            if (url.startsWith(
                                    ApplicationConstants.FRONTEND_PROTOCOL_PREFIX)) {
                                return url.substring(
                                        ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
                                                .length());
                            }
                            return url;
                        }).collect(Collectors.toList()), matcher);

        Matcher<Iterable<? extends Dependency.Type>> typeMatcher;
        if (mocks.getDeploymentConfiguration().isBowerMode()) {
            typeMatcher = containsInAnyOrder(Dependency.Type.HTML_IMPORT,
                    Dependency.Type.JAVASCRIPT, Dependency.Type.STYLESHEET);
        } else {
            typeMatcher = containsInAnyOrder(Dependency.Type.JS_MODULE,
                    Dependency.Type.JAVASCRIPT, Dependency.Type.STYLESHEET);
        }
        assertThat("Should have 3 different eager dependency types",
                eagerDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_TYPE))
                        .map(Dependency.Type::valueOf)
                        .collect(Collectors.toList()), typeMatcher);

        List<JsonObject> lazyDependencies = dependenciesMap.get(LoadMode.LAZY);
        assertThat("Should have 3 lazy dependencies", lazyDependencies,
                hasSize(3));
        assertThat("Lazy dependencies should not have inline contents",
                lazyDependencies.stream()
                        .filter(json -> json.hasKey(Dependency.KEY_CONTENTS))
                        .collect(Collectors.toList()), hasSize(0));

        if (mocks.getDeploymentConfiguration().isBowerMode()) {
            matcher = containsInAnyOrder("lazy.js", "lazy.html", "lazy.css");
        } else {
            matcher = containsInAnyOrder("lazy.js", "lazy.mjs", "lazy.css");
        }

        assertThat("Should have 3 different lazy urls",
                lazyDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_URL))
                        .map(url -> {
                            if (url.startsWith(
                                    ApplicationConstants.FRONTEND_PROTOCOL_PREFIX)) {
                                return url.substring(
                                        ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
                                                .length());
                            }
                            return url;
                        }).collect(Collectors.toList()), matcher);
        assertThat("Should have 3 different lazy dependency types",
                lazyDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_TYPE))
                        .map(Dependency.Type::valueOf)
                        .collect(Collectors.toList()), typeMatcher);

        List<JsonObject> inlineDependencies = dependenciesMap
                .get(LoadMode.INLINE);
        assertInlineDependencies(inlineDependencies, "/frontend/");
    }

    @Test
    public void checkAllTypesOfDependencies_uriResolverResolvesFrontendProtocol() {
        mocks.getDeploymentConfiguration().setBowerMode(true);
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        ui.add(new ComponentWithFrontendProtocol());
        JsonObject response = uidlWriter.createUidl(ui, false);
        List<JsonObject> inlineDependencies = JsonUtils.<JsonObject>stream(
                response.getArray(LoadMode.INLINE.name()))
                .collect(Collectors.toList());

        assertInlineDependencies(inlineDependencies, "/frontend/");
    }

    @Test
    public void checkAllNpmTypesOfDependencies_uriResolverResolvesFrontendProtocol() {
        mocks.getDeploymentConfiguration().setBowerMode(false);
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        ui.add(new ComponentWithFrontendProtocol());
        JsonObject response = uidlWriter.createUidl(ui, false);
        List<JsonObject> inlineDependencies = JsonUtils.<JsonObject>stream(
                response.getArray(LoadMode.INLINE.name()))
                .collect(Collectors.toList());

        assertInlineDependencies(inlineDependencies, "/frontend/");
    }

    @Test
    @Ignore("See https://github.com/vaadin/flow/issues/3822")
    public void parentViewDependenciesAreAddedFirst() {
        mocks.getDeploymentConfiguration().setBowerMode(true);
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
                3, eagerDependencies.length());

        List<Class<?>> expectedClassOrder = Arrays
                .asList(SuperParentClass.class, ParentClass.class,
                        BaseClass.class);

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
                        .collect(Collectors.toList()), hasSize(0));
        Matcher<Iterable<? extends String>> matcher;
        if (mocks.getDeploymentConfiguration().isBowerMode()) {
            matcher = containsInAnyOrder("inline.js", "inline.html",
                    "inline.css");
        } else {
            matcher = containsInAnyOrder("inline.js", "inline.mjs",
                    "inline.css");
        }
        assertThat("Should have different inline contents",
                inlineDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_CONTENTS))
                        .map(url -> {
                            if (!url.startsWith(expectedPrefix)) {
                                if(url.endsWith("mjs")) {
                                    return url;
                                }
                                throw new AssertionError(
                                        url + " should have the prefix "
                                                + expectedPrefix);
                            } else {
                                return url.substring(expectedPrefix.length());
                            }
                        }).collect(Collectors.toList()), matcher);

        Matcher<Iterable<? extends Dependency.Type>> typeMatcher;
        if (mocks.getDeploymentConfiguration().isBowerMode()) {
            typeMatcher = containsInAnyOrder(Dependency.Type.HTML_IMPORT,
                    Dependency.Type.JAVASCRIPT, Dependency.Type.STYLESHEET);
        } else {
            typeMatcher = containsInAnyOrder(Dependency.Type.JS_MODULE,
                    Dependency.Type.JAVASCRIPT, Dependency.Type.STYLESHEET);
        }
        assertThat("Should have 3 different inline dependency type",
                inlineDependencies.stream()
                        .map(json -> json.getString(Dependency.KEY_TYPE))
                        .map(Dependency.Type::valueOf)
                        .collect(Collectors.toList()), typeMatcher);
    }

    private UI initializeUIForDependenciesTest(UI ui) throws Exception {
        mocks = new MockServletServiceSessionSetup();

        VaadinSession session = mocks.getSession();
        session.lock();
        ui.getInternals().setSession(session);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(ui.getRouter().getRegistry());
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            routeConfiguration.setAnnotatedRoute(BaseClass.class);
        });
        for (String type : new String[] { "html", "js", "css", "mjs" }) {
            mocks.getServlet()
                    .addServletContextResource("/frontend/inline." + type,
                            "/frontend/inline." + type);
        }
        mocks.getServlet()
                .addServletContextResource("inline.mjs", "inline.mjs");

        HttpServletRequest servletRequestMock = mock(HttpServletRequest.class);

        VaadinServletRequest vaadinRequestMock = mock(
                VaadinServletRequest.class);

        when(vaadinRequestMock.getHttpServletRequest())
                .thenReturn(servletRequestMock);

        ui.doInit(vaadinRequestMock, 1);
        ui.getRouter().initializeUI(ui, vaadinRequestMock);

        return ui;
    }

    private void addInitialComponentDependencies(UI ui, UidlWriter uidlWriter) {
        ui.add(new ActualComponent());

        JsonObject response = uidlWriter.createUidl(ui, false);
        Map<String, JsonObject> dependenciesMap = getDependenciesMap(response);

        assertEquals(19, dependenciesMap.size());

        // UI parent first, then UI, then super component's dependencies, then
        // the interfaces and then the component
        assertDependency("UI-parent-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("UI-" + JS_TYPE_NAME, JS_TYPE_NAME, dependenciesMap);

        assertDependency("UI-parent-" + JS_MODULE_NAME, JS_MODULE_NAME,
                dependenciesMap);
        assertDependency("UI-" + JS_MODULE_NAME, JS_MODULE_NAME,
                dependenciesMap);

        assertDependency("super-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("anotherinterface-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency("interface-" + JS_TYPE_NAME, JS_TYPE_NAME,
                dependenciesMap);
        assertDependency(JS_TYPE_NAME, JS_TYPE_NAME, dependenciesMap);

        if (mocks.getDeploymentConfiguration().isBowerMode()) {
            assertDependency("super-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                    dependenciesMap);
            assertDependency("anotherinterface-" + HTML_TYPE_NAME,
                    HTML_TYPE_NAME, dependenciesMap);
            assertDependency("interface-" + HTML_TYPE_NAME, HTML_TYPE_NAME,
                    dependenciesMap);
            assertDependency(HTML_TYPE_NAME, HTML_TYPE_NAME, dependenciesMap);

            assertDependency("0.html", HTML_TYPE_NAME, dependenciesMap);
            // parent router layouts
            assertDependency("1.html", HTML_TYPE_NAME, dependenciesMap);
            assertDependency("2.html", HTML_TYPE_NAME, dependenciesMap);
        } else {
            assertDependency("super-" + JS_MODULE_NAME, JS_MODULE_NAME,
                    dependenciesMap);
            assertDependency("anotherinterface-" + JS_MODULE_NAME,
                    JS_MODULE_NAME, dependenciesMap);
            assertDependency("interface-" + JS_MODULE_NAME, JS_MODULE_NAME,
                    dependenciesMap);
            assertDependency(JS_MODULE_NAME, JS_MODULE_NAME, dependenciesMap);

            assertDependency("/frontend/0.js", JS_MODULE_NAME, dependenciesMap);
            // parent router layouts
            assertDependency("/frontend/1.js", JS_MODULE_NAME, dependenciesMap);
            assertDependency("/frontend/2.js", JS_MODULE_NAME, dependenciesMap);
        }
        assertDependency("super-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);

        assertDependency("anotherinterface-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);

        assertDependency("interface-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);

        assertDependency(CSS_STYLE_NAME, CSS_STYLE_NAME, dependenciesMap);

    }

    private Map<String, JsonObject> getDependenciesMap(JsonObject response) {
        return Stream.of(LoadMode.values())
                .map(mode -> response.getArray(mode.name()))
                .flatMap(JsonUtils::<JsonObject>stream).collect(Collectors
                        .toMap(jsonObject -> jsonObject
                                        .getString(Dependency.KEY_URL),
                                Function.identity()));
    }

    private void assertDependency(String url, String type,
            Map<String, JsonObject> dependenciesMap) {
        if (!type.equals(JS_MODULE_NAME)) {
            url = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + url;
        }
        JsonObject jsonValue = dependenciesMap.get(url);
        assertNotNull(
                "Expected dependencies map to have dependency with key=" + url,
                jsonValue);
        assertEquals(url, jsonValue.get(Dependency.KEY_URL).asString());
        assertEquals(type, jsonValue.get(Dependency.KEY_TYPE).asString());
    }

}
