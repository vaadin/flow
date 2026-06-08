/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.router;

import jakarta.servlet.ServletContext;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockServletContext;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteHierarchyTest {

    /**
     * Extending class to expose the protected getRouteRegistry() for Mockito.
     */
    private static class MockService extends VaadinServletService {
        @Override
        public RouteRegistry getRouteRegistry() {
            return super.getRouteRegistry();
        }
    }

    private ApplicationRouteRegistry registry;
    private MockService vaadinService;
    private RouteConfiguration routeConfiguration;

    @BeforeEach
    void init() {
        ServletContext servletContext = new MockServletContext();
        VaadinServletContext vaadinContext = new MockVaadinContext(
                servletContext);
        registry = ApplicationRouteRegistry.getInstance(vaadinContext);

        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.getFrontendFolder())
                .thenReturn(new File("/frontend"));

        vaadinService = Mockito.mock(MockService.class);
        Mockito.when(vaadinService.getRouteRegistry()).thenReturn(registry);
        Mockito.when(vaadinService.getContext()).thenReturn(vaadinContext);
        Mockito.when(vaadinService.getDeploymentConfiguration())
                .thenReturn(configuration);

        VaadinService.setCurrent(vaadinService);
        routeConfiguration = RouteConfiguration.forRegistry(registry);
    }

    @AfterEach
    void tearDown() {
        VaadinService.setCurrent(null);
    }

    // ----- Scenario 1: routeClass with no @Route annotation -----

    @Tag(Tag.DIV)
    static class PlainComponent extends Component {
    }

    @Test
    void resolveAncestors_classWithoutRouteAnnotation_returnsEmptyList() {
        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(PlainComponent.class, routeConfiguration);
        assertTrue(ancestors.isEmpty(),
                "Expected empty list for class without @Route");
    }

    // ----- Scenario 2: @Route with no parent -----

    @Tag(Tag.DIV)
    @Route("foo")
    static class FooView extends Component {
    }

    @Test
    void resolveAncestors_singleRouteNoParent_returnsSingletonChain() {
        routeConfiguration.setAnnotatedRoute(FooView.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(FooView.class, routeConfiguration);
        assertEquals(List.of(FooView.class), ancestors);
    }

    // ----- Scenario 3: URL-prefix walking happy path -----

    @Tag(Tag.DIV)
    @Route("a")
    static class AView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("a/b")
    static class AbView extends Component {
    }

    @Test
    void resolveAncestors_urlPrefixHappyPath_returnsTwoElementChain() {
        routeConfiguration.setAnnotatedRoute(AView.class);
        routeConfiguration.setAnnotatedRoute(AbView.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(AbView.class, routeConfiguration);
        assertEquals(List.of(AView.class, AbView.class), ancestors);
    }

    // ----- Scenario 4: missing intermediate, single strip terminates -----

    @Tag(Tag.DIV)
    @Route("a/b/c")
    static class AbcGapView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("ga")
    static class GapAView extends Component {
    }

    @Test
    void resolveAncestors_missingIntermediate_terminatesAfterOneStrip() {
        // Register only "ga" (a different route) and "a/b/c", leaving "a/b"
        // and "a" unregistered. Walker should strip once to "a/b", find no
        // route there, and terminate with only the leaf in the chain.
        routeConfiguration.setAnnotatedRoute(GapAView.class);
        routeConfiguration.setAnnotatedRoute(AbcGapView.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(AbcGapView.class, routeConfiguration);
        assertEquals(List.of(AbcGapView.class), ancestors);
    }

    // ----- Scenario 5: empty-template root @Route("") -----

    @Tag(Tag.DIV)
    @Route("")
    static class HomeView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("docs")
    static class DocsView extends Component {
    }

    @Test
    void resolveAncestors_emptyTemplateRoot_isFoundAsParent() {
        routeConfiguration.setAnnotatedRoute(HomeView.class);
        routeConfiguration.setAnnotatedRoute(DocsView.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(DocsView.class, routeConfiguration);
        assertEquals(List.of(HomeView.class, DocsView.class), ancestors);
    }

    // ----- Scenario 6: @RouteParent overrides URL-prefix walking -----

    @Tag(Tag.DIV)
    @Route("orders")
    static class OrdersView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("orders/edit/:id")
    @RouteParent(OrdersView.class)
    static class EditOrderView extends Component {
    }

    @Test
    void resolveAncestors_routeParentAnnotation_takesPrecedenceOverUrlPrefix() {
        routeConfiguration.setAnnotatedRoute(OrdersView.class);
        routeConfiguration.setAnnotatedRoute(EditOrderView.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(EditOrderView.class, routeConfiguration);
        assertEquals(List.of(OrdersView.class, EditOrderView.class), ancestors);
    }

    // ----- Scenario 7: cycle detection via @RouteParent -----

    @Tag(Tag.DIV)
    @Route("cycle-a")
    @RouteParent(CycleBView.class)
    static class CycleAView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("cycle-b")
    @RouteParent(CycleAView.class)
    static class CycleBView extends Component {
    }

    @Test
    void resolveAncestors_cycleViaRouteParent_truncatesWithoutDuplicates() {
        routeConfiguration.setAnnotatedRoute(CycleAView.class);
        routeConfiguration.setAnnotatedRoute(CycleBView.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(CycleAView.class, routeConfiguration);
        // The chain must contain CycleAView and may contain CycleBView once,
        // but never repeat. Length is at most 2.
        assertTrue(ancestors.size() <= 2,
                "Cycle should be truncated; chain length must be <= 2 but was "
                        + ancestors.size());
        assertTrue(ancestors.contains(CycleAView.class),
                "Chain must include the starting class");
        assertEquals(ancestors.size(), ancestors.stream().distinct().count(),
                "Chain must not contain duplicate entries");
    }

    // ----- Scenario 8: @RouteParent points to non-@Route class, URL fallback
    // works -----

    @Tag(Tag.DIV)
    static class NonRouteParent extends Component {
    }

    @Tag(Tag.DIV)
    @Route("p")
    static class PView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("p/q")
    @RouteParent(NonRouteParent.class)
    static class PqViewWithBadAnnotation extends Component {
    }

    @Test
    void resolveAncestors_routeParentNotAnnotated_fallsBackToUrlPrefix() {
        routeConfiguration.setAnnotatedRoute(PView.class);
        routeConfiguration.setAnnotatedRoute(PqViewWithBadAnnotation.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(PqViewWithBadAnnotation.class,
                        routeConfiguration);
        assertEquals(List.of(PView.class, PqViewWithBadAnnotation.class),
                ancestors);
    }

    // ----- Scenario 9: @RouteParent invalid, no URL fallback either -----

    @Tag(Tag.DIV)
    @Route("standalone")
    @RouteParent(NonRouteParent.class)
    static class StandaloneWithBadAnnotation extends Component {
    }

    @Test
    void resolveAncestors_routeParentNotAnnotatedAndNoUrlFallback_terminates() {
        // Only register the leaf; "standalone" has no URL-prefix ancestor
        // since "" is not registered.
        routeConfiguration.setAnnotatedRoute(StandaloneWithBadAnnotation.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(StandaloneWithBadAnnotation.class,
                        routeConfiguration);
        assertEquals(List.of(StandaloneWithBadAnnotation.class), ancestors);
    }

    // ----- Scenario 10: deep multi-step URL-prefix walking -----

    @Tag(Tag.DIV)
    @Route("a/b/c")
    static class AbcView extends Component {
    }

    @Test
    void resolveAncestors_deepChain_walksMultipleSteps() {
        routeConfiguration.setAnnotatedRoute(AView.class);
        routeConfiguration.setAnnotatedRoute(AbView.class);
        routeConfiguration.setAnnotatedRoute(AbcView.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(AbcView.class, routeConfiguration);
        assertEquals(List.of(AView.class, AbView.class, AbcView.class),
                ancestors);
    }

    // ----- Scenario 11: resolveParent consistency with three-element chain
    // -----

    @Test
    void resolveParent_threeElementChain_returnsSecondToLast() {
        routeConfiguration.setAnnotatedRoute(AView.class);
        routeConfiguration.setAnnotatedRoute(AbView.class);
        routeConfiguration.setAnnotatedRoute(AbcView.class);

        Optional<Class<? extends Component>> parent = RouteHierarchy
                .resolveParent(AbcView.class, routeConfiguration);
        assertEquals(Optional.of(AbView.class), parent);
    }

    // ----- Scenario 12: resolveParent on single-element chain -----

    @Test
    void resolveParent_singletonChain_returnsEmpty() {
        routeConfiguration.setAnnotatedRoute(FooView.class);

        Optional<Class<? extends Component>> parent = RouteHierarchy
                .resolveParent(FooView.class, routeConfiguration);
        assertEquals(Optional.empty(), parent);
    }

    // ----- Scenario 13: parameterised template (users/:id) -----

    @Tag(Tag.DIV)
    @Route("users")
    static class UsersView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("users/:id")
    static class UserDetailView extends Component {
    }

    @Test
    void resolveAncestors_parameterisedTemplate_stripsParameterSegment() {
        routeConfiguration.setAnnotatedRoute(UsersView.class);
        routeConfiguration.setAnnotatedRoute(UserDetailView.class);

        List<Class<? extends Component>> ancestors = RouteHierarchy
                .resolveAncestors(UserDetailView.class, routeConfiguration);
        assertEquals(List.of(UsersView.class, UserDetailView.class), ancestors);
    }

    // ----- Scenario 14: null arguments throw NPE -----

    @Test
    void nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> RouteHierarchy
                .resolveAncestors(null, routeConfiguration));
        assertThrows(NullPointerException.class,
                () -> RouteHierarchy.resolveAncestors(FooView.class, null));
        assertThrows(NullPointerException.class,
                () -> RouteHierarchy.resolveParent(null, routeConfiguration));
        assertThrows(NullPointerException.class,
                () -> RouteHierarchy.resolveParent(FooView.class, null));
    }

    // ----- Scenario 15: param-aware resolveAncestors, non-@Route returns empty
    // -----

    @Test
    void resolveAncestors_paramAware_classWithoutRouteAnnotation_returnsEmptyList() {
        List<RouteHierarchy.Entry> entries = RouteHierarchy.resolveAncestors(
                PlainComponent.class, RouteParameters.empty(),
                routeConfiguration);
        assertTrue(entries.isEmpty());
    }

    // ----- Scenario 16: param-aware resolveAncestors, single non-parameterised
    // route returns Entry with empty params -----

    @Test
    void resolveAncestors_paramAware_singleRouteNoParams_returnsEntryWithEmptyParams() {
        routeConfiguration.setAnnotatedRoute(FooView.class);

        List<RouteHierarchy.Entry> entries = RouteHierarchy.resolveAncestors(
                FooView.class, RouteParameters.empty(), routeConfiguration);
        assertEquals(List.of(new RouteHierarchy.Entry(FooView.class,
                RouteParameters.empty())), entries);
    }

    // ----- Scenario 17: param-aware resolveAncestors projects leaf parameters
    // onto the parameterised template only -----

    @Test
    void resolveAncestors_paramAware_parameterisedLeaf_projectsParamsToLeafOnly() {
        routeConfiguration.setAnnotatedRoute(UsersView.class);
        routeConfiguration.setAnnotatedRoute(UserDetailView.class);

        RouteParameters available = new RouteParameters(Map.of("id", "42"));
        List<RouteHierarchy.Entry> entries = RouteHierarchy.resolveAncestors(
                UserDetailView.class, available, routeConfiguration);

        assertEquals(2, entries.size());
        assertEquals(new RouteHierarchy.Entry(UsersView.class,
                RouteParameters.empty()), entries.get(0));
        assertEquals(
                new RouteHierarchy.Entry(UserDetailView.class,
                        new RouteParameters(Map.of("id", "42"))),
                entries.get(1));
    }

    // ----- Scenario 18: deep parameterised hierarchy - each ancestor gets only
    // the parameters its template declares (UC4 from vaadin/use-cases) -----

    @Tag(Tag.DIV)
    @Route("projects")
    static class ProjectsView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("projects/:projectId")
    @RouteParent(ProjectsView.class)
    static class ProjectView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("projects/:projectId/tasks")
    @RouteParent(ProjectView.class)
    static class TasksView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("projects/:projectId/tasks/:taskId")
    @RouteParent(TasksView.class)
    static class TaskDetailView extends Component {
    }

    @Test
    void resolveAncestors_paramAware_deepParameterisedHierarchy_projectsPerTemplate() {
        routeConfiguration.setAnnotatedRoute(ProjectsView.class);
        routeConfiguration.setAnnotatedRoute(ProjectView.class);
        routeConfiguration.setAnnotatedRoute(TasksView.class);
        routeConfiguration.setAnnotatedRoute(TaskDetailView.class);

        RouteParameters available = new RouteParameters(
                Map.of("projectId", "apollo", "taskId", "2"));
        List<RouteHierarchy.Entry> entries = RouteHierarchy.resolveAncestors(
                TaskDetailView.class, available, routeConfiguration);

        assertEquals(4, entries.size());
        // Root has no template parameters.
        assertEquals(new RouteHierarchy.Entry(ProjectsView.class,
                RouteParameters.empty()), entries.get(0));
        // Two middle ancestors keep :projectId only - never :taskId.
        RouteParameters projectIdOnly = new RouteParameters(
                Map.of("projectId", "apollo"));
        assertEquals(new RouteHierarchy.Entry(ProjectView.class, projectIdOnly),
                entries.get(1));
        assertEquals(new RouteHierarchy.Entry(TasksView.class, projectIdOnly),
                entries.get(2));
        // Leaf carries both.
        assertEquals(new RouteHierarchy.Entry(TaskDetailView.class, available),
                entries.get(3));
    }

    // ----- Scenario 19: param-aware resolveAncestors drops parameters not
    // declared by any ancestor's template -----

    @Test
    void resolveAncestors_paramAware_dropsUnknownParameters() {
        routeConfiguration.setAnnotatedRoute(UsersView.class);
        routeConfiguration.setAnnotatedRoute(UserDetailView.class);

        RouteParameters available = new RouteParameters(
                Map.of("id", "42", "unrelated", "ignored"));
        List<RouteHierarchy.Entry> entries = RouteHierarchy.resolveAncestors(
                UserDetailView.class, available, routeConfiguration);

        RouteParameters leafParams = entries.get(1).parameters();
        assertEquals(Optional.of("42"), leafParams.get("id"));
        assertTrue(leafParams.get("unrelated").isEmpty(),
                "Unknown parameter must not appear in projected params");
    }

    // ----- Scenario 20: param-aware resolveParent returns projected parent
    // entry -----

    @Test
    void resolveParent_paramAware_returnsProjectedParentEntry() {
        routeConfiguration.setAnnotatedRoute(ProjectsView.class);
        routeConfiguration.setAnnotatedRoute(ProjectView.class);
        routeConfiguration.setAnnotatedRoute(TasksView.class);

        RouteParameters available = new RouteParameters(
                Map.of("projectId", "apollo"));
        Optional<RouteHierarchy.Entry> parent = RouteHierarchy
                .resolveParent(TasksView.class, available, routeConfiguration);

        assertEquals(
                Optional.of(new RouteHierarchy.Entry(ProjectView.class,
                        new RouteParameters(Map.of("projectId", "apollo")))),
                parent);
    }

    // ----- Scenario 21: param-aware resolveParent returns empty for root view
    // -----

    @Test
    void resolveParent_paramAware_singletonChain_returnsEmpty() {
        routeConfiguration.setAnnotatedRoute(FooView.class);

        Optional<RouteHierarchy.Entry> parent = RouteHierarchy.resolveParent(
                FooView.class, RouteParameters.empty(), routeConfiguration);
        assertEquals(Optional.empty(), parent);
    }

    // ----- Scenario 22: param-aware null arguments throw NPE -----

    @Test
    void paramAware_nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> RouteHierarchy.resolveAncestors(null,
                        RouteParameters.empty(), routeConfiguration));
        assertThrows(NullPointerException.class, () -> RouteHierarchy
                .resolveAncestors(FooView.class, null, routeConfiguration));
        assertThrows(NullPointerException.class,
                () -> RouteHierarchy.resolveAncestors(FooView.class,
                        RouteParameters.empty(), null));
        assertThrows(NullPointerException.class,
                () -> RouteHierarchy.resolveParent(null,
                        RouteParameters.empty(), routeConfiguration));
        assertThrows(NullPointerException.class, () -> RouteHierarchy
                .resolveParent(FooView.class, null, routeConfiguration));
        assertThrows(NullPointerException.class, () -> RouteHierarchy
                .resolveParent(FooView.class, RouteParameters.empty(), null));
    }
}
