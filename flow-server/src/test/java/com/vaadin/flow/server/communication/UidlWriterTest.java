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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentTest;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UITest;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RoutePathProvider;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.MockVaadinContext.RoutePathProviderImpl;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.BundleUtils;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@NotThreadSafe
public class UidlWriterTest {
    private static final String CSS_STYLE_NAME = Dependency.Type.STYLESHEET
            .name();
    private MockServletServiceSessionSetup mocks;

    @JavaScript("UI-parent-JAVASCRIPT")
    private static class ParentUI extends UI {
    }

    @JavaScript("UI-JAVASCRIPT")
    private static class TestUI extends ParentUI {
    }

    @Tag("div")
    @JavaScript("super-JAVASCRIPT")
    @StyleSheet("super-STYLESHEET")
    public static class SuperComponent extends Component {
    }

    public static class EmptyClassWithInterface extends SuperComponent
            implements AnotherComponentInterface {
    }

    @JavaScript("JAVASCRIPT")
    @StyleSheet("STYLESHEET")
    public static class ActualComponent extends EmptyClassWithInterface
            implements ComponentInterface {
    }

    @JavaScript("child1-JAVASCRIPT")
    @JavaScript("child2-JAVASCRIPT")
    @StyleSheet("child1-STYLESHEET")
    @StyleSheet("child2-STYLESHEET")
    public static class ChildComponent extends ActualComponent
            implements ChildComponentInterface2 {
    }

    @JavaScript("interface-JAVASCRIPT")
    @StyleSheet("interface-STYLESHEET")
    public interface ComponentInterface {
    }

    @JavaScript("anotherinterface-JAVASCRIPT")
    @StyleSheet("anotherinterface-STYLESHEET")
    public interface AnotherComponentInterface {
    }

    @JavaScript("childinterface1-JAVASCRIPT")
    @StyleSheet("childinterface1-STYLESHEET")
    public interface ChildComponentInterface1 {
    }

    @JavaScript("childinterface2-JAVASCRIPT")
    @StyleSheet("childinterface2-STYLESHEET")
    public interface ChildComponentInterface2 extends ChildComponentInterface1 {
    }

    @Tag("test")
    @JavaScript(value = "lazy.js", loadMode = LoadMode.LAZY)
    @StyleSheet(value = "lazy.css", loadMode = LoadMode.LAZY)
    @JavaScript(value = "inline.js", loadMode = LoadMode.INLINE)
    @StyleSheet(value = "inline.css", loadMode = LoadMode.INLINE)
    @JavaScript("eager.js")
    @StyleSheet("eager.css")
    public static class ComponentWithAllDependencyTypes extends Component {
    }

    @Tag("base")
    @Route(value = "", layout = ParentClass.class)
    public static class BaseClass extends Component {
    }

    @Tag("parent")
    @ParentLayout(SuperParentClass.class)
    public static class ParentClass extends Component implements RouterLayout {
    }

    @Tag("super-parent")
    public static class SuperParentClass extends Component
            implements RouterLayout {
    }

    @Tag("components-container")
    public static class ComponentsContainer extends Component
            implements HasComponents {

    }

    @After
    public void tearDown() {
        if (mocks != null) {
            mocks.cleanup();
        }
    }

    @Test
    public void testEncodeExecuteJavaScript_npmMode() {
        Element element = ElementFactory.createDiv();

        JavaScriptInvocation invocation1 = new JavaScriptInvocation(
                "$0.focus()", element);
        JavaScriptInvocation invocation2 = new JavaScriptInvocation(
                "console.log($0, $1)", "Lives remaining:", Integer.valueOf(3));
        List<PendingJavaScriptInvocation> executeJavaScriptList = Stream
                .of(invocation1, invocation2)
                .map(invocation -> new PendingJavaScriptInvocation(
                        element.getNode(), invocation))
                .collect(Collectors.toList());

        ArrayNode json = UidlWriter
                .encodeExecuteJavaScriptList(executeJavaScriptList);

        ArrayNode expectedJson = JacksonUtils.createArray(
                JacksonUtils.createArray(
                        // Null since element is not attached
                        JacksonUtils.nullNode(),
                        JacksonUtils.createNode("$0.focus()")),
                JacksonUtils.createArray(
                        JacksonUtils.createNode("Lives remaining:"),
                        JacksonUtils.createNode(3),
                        JacksonUtils.createNode("console.log($0, $1)")));

        assertTrue(JacksonUtils.jsonEquals(expectedJson, json));
    }

    @Test
    public void componentDependencies_npmMode() throws Exception {
        UI ui = initializeUIForDependenciesTest(new TestUI());
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        // no dependencies should be resent in next response
        ObjectNode response = uidlWriter.createUidl(ui, false);
        assertFalse(response.has(LoadMode.EAGER.name()));
        assertFalse(response.has(LoadMode.INLINE.name()));
        assertFalse(response.has(LoadMode.LAZY.name()));
    }

    @Test
    public void componentDependencies_productionMode_scanForParentClasses()
            throws Exception {
        UI ui = initializeUIForDependenciesTest(new TestUI());
        mocks.getDeploymentConfiguration().setProductionMode(true);

        UidlWriter uidlWriter = new UidlWriter();
        ui.add(new ChildComponent());

        // no dependencies should be resent in next response
        ObjectNode response = uidlWriter.createUidl(ui, false);
        Set<String> chunks = getDependenciesMap(response).keySet().stream()
                .filter(key -> key
                        .startsWith("return window.Vaadin.Flow.loadOnDemand('"))
                .map(key -> key
                        .replace("return window.Vaadin.Flow.loadOnDemand('", "")
                        .replace("');", ""))
                .collect(Collectors.toSet());

        Set<String> expectedChunks = Stream
                .of(TestUI.class, BaseClass.class, ChildComponent.class,
                        ActualComponent.class, EmptyClassWithInterface.class,
                        SuperComponent.class)
                .map(BundleUtils::getChunkId).collect(Collectors.toSet());

        assertEquals(expectedChunks, chunks);
    }

    @Test
    public void componentDependencies_developmentMode_onlySendComponentSpecificChunks()
            throws Exception {
        UidlWriter uidlWriter = new UidlWriter();
        UI ui = initializeUIForDependenciesTest(new TestUI());
        ui.add(new ChildComponent());

        // no dependencies should be resent in next response
        ObjectNode response = uidlWriter.createUidl(ui, false);
        Set<String> chunks = getDependenciesMap(response).keySet().stream()
                .filter(key -> key
                        .startsWith("return window.Vaadin.Flow.loadOnDemand('"))
                .map(key -> key
                        .replace("return window.Vaadin.Flow.loadOnDemand('", "")
                        .replace("');", ""))
                .collect(Collectors.toSet());

        Set<String> expectedChunks = Stream
                .of(TestUI.class, BaseClass.class, ChildComponent.class)
                .map(BundleUtils::getChunkId).collect(Collectors.toSet());

        assertEquals(expectedChunks, chunks);
    }

    @Test
    public void testComponentInterfaceDependencies_npmMode() throws Exception {
        UI ui = initializeUIForDependenciesTest(new TestUI());
        UidlWriter uidlWriter = new UidlWriter();

        addInitialComponentDependencies(ui, uidlWriter);

        // test that dependencies only from new child interfaces are added
        ui.add(new ActualComponent(), new SuperComponent(),
                new ChildComponent());

        ObjectNode response = uidlWriter.createUidl(ui, false);
        Map<String, ObjectNode> dependenciesMap = ComponentTest
                .filterLazyLoading(getDependenciesMap(response));

        assertEquals(4, dependenciesMap.size());
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
    public void checkAllTypesOfDependencies_npmMode() throws Exception {
        UI ui = initializeUIForDependenciesTest(new TestUI());
        UidlWriter uidlWriter = new UidlWriter();
        addInitialComponentDependencies(ui, uidlWriter);

        ui.add(new ComponentWithAllDependencyTypes());
        ObjectNode response = uidlWriter.createUidl(ui, false);
        Map<LoadMode, List<ObjectNode>> dependenciesMap = Stream
                .of(LoadMode.values())
                .map(mode -> (ArrayNode) response.get(mode.name()))
                .flatMap(JacksonUtils::<ObjectNode> stream)
                .collect(Collectors.toMap(
                        jsonObject -> LoadMode.valueOf(jsonObject
                                .get(Dependency.KEY_LOAD_MODE).textValue()),
                        Collections::singletonList, (list1, list2) -> {
                            List<ObjectNode> result = new ArrayList<>(list1);
                            result.addAll(list2);
                            return result;
                        }));
        dependenciesMap.get(LoadMode.LAZY)
                .removeIf(obj -> obj.get(Dependency.KEY_URL).textValue()
                        .contains("Flow.loadOnDemand"));
        assertThat(
                "Dependencies with all types of load mode should be present in this response",
                dependenciesMap.size(), is(LoadMode.values().length));

        List<ObjectNode> eagerDependencies = dependenciesMap
                .get(LoadMode.EAGER);
        assertThat("Should have an eager dependency", eagerDependencies,
                hasSize(1));
        assertThat("Eager dependencies should not have inline contents",
                eagerDependencies.stream()
                        .filter(json -> json.has(Dependency.KEY_CONTENTS))
                        .collect(Collectors.toList()),
                hasSize(0));

        ObjectNode eagerDependency = eagerDependencies.get(0);
        assertEquals("eager.css",
                eagerDependency.get(Dependency.KEY_URL).textValue());
        assertEquals(Dependency.Type.STYLESHEET, Dependency.Type
                .valueOf(eagerDependency.get(Dependency.KEY_TYPE).textValue()));

        List<ObjectNode> lazyDependencies = dependenciesMap.get(LoadMode.LAZY);
        ObjectNode lazyDependency = lazyDependencies.get(0);
        assertEquals("lazy.css",
                lazyDependency.get(Dependency.KEY_URL).textValue());
        assertEquals(Dependency.Type.STYLESHEET, Dependency.Type
                .valueOf(lazyDependency.get(Dependency.KEY_TYPE).textValue()));

        List<ObjectNode> inlineDependencies = dependenciesMap
                .get(LoadMode.INLINE);
        assertInlineDependencies(inlineDependencies);
    }

    @Test
    public void resynchronizationRequested_responseFieldContainsResynchronize()
            throws Exception {
        UI ui = initializeUIForDependenciesTest(new TestUI());
        UidlWriter uidlWriter = new UidlWriter();

        ObjectNode response = uidlWriter.createUidl(ui, false, true);
        assertTrue("Response contains resynchronize field",
                response.has(ApplicationConstants.RESYNCHRONIZE_ID));
        assertTrue("Response resynchronize field is set to true", response
                .get(ApplicationConstants.RESYNCHRONIZE_ID).booleanValue());
    }

    @Test
    public void createUidl_allChangesCollected_uiIsNotDirty() throws Exception {
        UI ui = initializeUIForDependenciesTest(new TestUI());

        ComponentsContainer container = new ComponentsContainer();
        container.add(new ChildComponent());
        ui.add(container);
        // removing all elements causes an additional ListClearChange to be
        // added during collectChanges process
        container.removeAll();

        UidlWriter uidlWriter = new UidlWriter();
        uidlWriter.createUidl(ui, false, true);

        assertFalse("UI is still dirty after creating UIDL",
                ui.getInternals().isDirty());
    }

    @Test
    public void createUidl_collectChangesUIStillDirty_shouldNotLoopEndlessly()
            throws Exception {
        UI ui = initializeUIForDependenciesTest(spy(new TestUI()));
        StateTree stateTree = spy(ui.getInternals().getStateTree());
        UIInternals internals = spy(ui.getInternals());

        when(ui.getInternals()).thenReturn(internals);
        when(internals.getStateTree()).thenReturn(stateTree);
        when(stateTree.hasDirtyNodes()).thenReturn(true);

        UidlWriter uidlWriter = new UidlWriter();
        uidlWriter.createUidl(ui, false, true);

        assertTrue(
                "Simulating collectChanges bug and expecting UI to be still dirty after creating UIDL",
                ui.getInternals().isDirty());
    }

    private void assertInlineDependencies(List<ObjectNode> inlineDependencies) {
        assertThat("Should have an inline dependency", inlineDependencies,
                hasSize(1));
        assertThat("Eager dependencies should not have urls",
                inlineDependencies.stream()
                        .filter(json -> json.has(Dependency.KEY_URL))
                        .collect(Collectors.toList()),
                hasSize(0));

        ObjectNode inlineDependency = inlineDependencies.get(0);

        String url = inlineDependency.get(Dependency.KEY_CONTENTS).textValue();
        assertEquals("inline.css", url);
        assertEquals(Dependency.Type.STYLESHEET, Dependency.Type.valueOf(
                inlineDependency.get(Dependency.KEY_TYPE).textValue()));
    }

    private UI initializeUIForDependenciesTest(UI ui) throws Exception {
        mocks = new MockServletServiceSessionSetup();

        VaadinServletContext context = (VaadinServletContext) mocks.getService()
                .getContext();
        Lookup lookup = context.getAttribute(Lookup.class);
        Mockito.when(lookup.lookup(RoutePathProvider.class))
                .thenReturn(new RoutePathProviderImpl());

        VaadinSession session = mocks.getSession();
        session.lock();
        ui.getInternals().setSession(session);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(ui.getInternals().getRouter().getRegistry());
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            routeConfiguration.setAnnotatedRoute(BaseClass.class);
        });

        for (String type : new String[] { "html", "js", "css" }) {
            mocks.getServlet().addServletContextResource("inline." + type,
                    "inline." + type);
        }

        HttpServletRequest servletRequestMock = mock(HttpServletRequest.class);

        VaadinServletRequest vaadinRequestMock = mock(
                VaadinServletRequest.class);

        when(vaadinRequestMock.getHttpServletRequest())
                .thenReturn(servletRequestMock);

        ui.doInit(vaadinRequestMock, 1, "foo");
        ui.getInternals().getRouter().initializeUI(ui,
                UITest.requestToLocation(vaadinRequestMock));

        return ui;
    }

    private void addInitialComponentDependencies(UI ui, UidlWriter uidlWriter) {
        ui.add(new ActualComponent());

        ObjectNode response = uidlWriter.createUidl(ui, false);
        Map<String, ObjectNode> dependenciesMap = ComponentTest
                .filterLazyLoading(getDependenciesMap(response));

        assertEquals(4, dependenciesMap.size());

        // UI parent first, then UI, then super component's dependencies, then
        // the interfaces and then the component
        assertDependency("super-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);

        assertDependency("anotherinterface-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);

        assertDependency("interface-" + CSS_STYLE_NAME, CSS_STYLE_NAME,
                dependenciesMap);

        assertDependency(CSS_STYLE_NAME, CSS_STYLE_NAME, dependenciesMap);
    }

    private Map<String, ObjectNode> getDependenciesMap(ObjectNode response) {
        return Stream.of(LoadMode.values())
                .map(mode -> (ArrayNode) response.get(mode.name()))
                .flatMap(JacksonUtils::<ObjectNode> stream)
                .collect(Collectors.toMap(jsonObject -> jsonObject
                        .get(Dependency.KEY_URL).textValue(),
                        Function.identity()));
    }

    private void assertDependency(String url, String type,
            Map<String, ObjectNode> dependenciesMap) {
        ObjectNode jsonValue = dependenciesMap.get(url);
        assertNotNull(
                "Expected dependencies map to have dependency with key=" + url,
                jsonValue);
        assertEquals(url, jsonValue.get(Dependency.KEY_URL).textValue());
        assertEquals(type, jsonValue.get(Dependency.KEY_TYPE).textValue());
    }

}
