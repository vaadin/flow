package com.vaadin.flow.server.frontend.scanner;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.scanner.ScannerDependenciesTest.UISearchField.SearchFieldComponentDefinitionCreator;
import com.vaadin.flow.server.frontend.scanner.ScannerDependenciesTest.UISearchField.UISearchLayout;
import com.vaadin.flow.server.frontend.scanner.ScannerDependenciesTest.UISearchField.UISearchLayout.SearchLayoutComponentDefinitionCreator;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.BridgeClass;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Component0;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Component1;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Component2;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.DynamicComponentClassWithTwoImports;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.FirstView;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RouteWithNestedDynamicRouteClass;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RouteWithService;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RouteWithViewBean;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RoutedClass;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RoutedClassWithAnnotations;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RoutedClassWithoutAnnotations;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.SecondView;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Theme1;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.ThirdView;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.UnAnnotatedClass;
import com.vaadin.flow.server.frontend.scanner.samples.RouteInterfaceComponent;
import com.vaadin.flow.theme.NoTheme;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScannerDependenciesTest {

    static FrontendDependencies getFrontendDependencies(Class<?>... classes) {
        FrontendDependencies frontendDependencies = new FrontendDependencies(
                new DefaultClassFinder(new HashSet<>(
                        new ArrayList<>(Arrays.asList(classes)))) {
                    @Override
                    public boolean shouldInspectClass(String className) {
                        return className.startsWith("com.vaadin");
                    }
                }, true, null, true);
        return frontendDependencies;
    }

    @Test
    public void visitRouteEntryPoint_ExpectToAlsoVisitImplementedInterface() {
        FrontendDependencies deps = getFrontendDependencies(
                RouteInterfaceComponent.class);

        assertTrue("Missing dependency from implemented interface",
                DepsTests.merge(deps.getModules()).contains("myModule.js"));

        DepsTests.assertImportCount(1, deps.getCss());

        assertEquals("Invalid css import", "frontend://styles/interface.css",
                DepsTests.merge(deps.getCss()).get(0).getValue());
    }

    @Test
    public void should_extractClassesFromSignatures() {
        Set<String> classes = new HashSet<>();
        FrontendClassVisitor visitor = new FrontendClassVisitor(
                new ClassInfo("foo"));

        visitor.addSignatureToClasses(classes,
                "(Lcom/vaadin/flow/component/tabs/Tabs;Ljava/lang/String;Ljava/lang/Character;CLjava/lang/Integer;ILjava/lang/Long;JLjava/lang/Double;DLjava/lang/Float;FLjava/lang/Byte;BLjava/lang/Boolean;Z)Lcom/vaadin/flow/component/button/Button;");
        assertArrayEquals(new String[] { "java.lang.Float",
                "com.vaadin.flow.component.button.Button",
                "java.lang.Character", "java.lang.Long", "java.lang.Double",
                "java.lang.Boolean", "com.vaadin.flow.component.tabs.Tabs",
                "java.lang.String", "java.lang.Byte", "java.lang.Integer" },
                classes.toArray());
        int count = classes.size();

        visitor.addSignatureToClasses(classes,
                "([Lcom/vaadin/flow/component/Component;)V");
        assertEquals(count + 1, classes.size());
        assertTrue(classes.contains("com.vaadin.flow.component.Component"));

        visitor.addSignatureToClasses(classes,
                "(Lcom/vaadin/flow/component/orderedlayout/FlexComponent$Alignment;[Lcom/vaadin/flow/component/Component;)");
        assertEquals(count + 2, classes.size());
        assertTrue(classes.contains(
                "com.vaadin.flow.component.orderedlayout.FlexComponent$Alignment"));

        // Apart from proper signature representation, it should handle class
        // names, and class paths
        visitor.addSignatureToClasses(classes, this.getClass().getName());
        assertTrue(classes.contains(this.getClass().getName()));

        visitor.addSignatureToClasses(classes,
                "com/vaadin/flow/server/frontend/FrontendDependenciesTestComponents$AnotherComponent");
        assertTrue(classes.contains(
                "com.vaadin.flow.server.frontend.FrontendDependenciesTestComponents$AnotherComponent"));

    }

    @Test
    public void should_visitNpmPakageAnnotations() {
        FrontendDependencies deps = getFrontendDependencies(Component1.class,
                Component2.class);
        assertEquals(4, deps.getPackages().size());
        assertTrue(deps.getPackages().containsKey("@vaadin/component-1"));
        assertTrue(deps.getPackages().containsKey("@vaadin/component-2"));
        assertTrue(deps.getPackages().containsKey("@vaadin/component-0"));
        assertTrue(deps.getPackages().containsKey("@vaadin/vaadin-foo"));
        assertEquals("1.1.1", deps.getPackages().get("@vaadin/component-1"));
        assertEquals("222.222.222",
                deps.getPackages().get("@vaadin/component-2"));
        assertEquals("=2.1.0", deps.getPackages().get("@vaadin/component-0"));
        assertEquals("1.23.114-alpha1",
                deps.getPackages().get("@vaadin/vaadin-foo"));
    }

    @Test
    public void should_visitSuperNpmPakageAnnotations() {
        FrontendDependencies deps = getFrontendDependencies(
                ScannerTestComponents.ComponentExtending.class);
        assertEquals(1, deps.getPackages().size());
        assertTrue(
                deps.getPackages().containsKey("@vaadin/component-extended"));

        assertEquals("2.1.0",
                deps.getPackages().get("@vaadin/component-extended"));
    }

    @Test
    public void when_MultipleVersions_should_returnFirstVisitedOne() {
        FrontendDependencies deps = getFrontendDependencies(Component0.class);
        assertEquals("=2.1.0", deps.getPackages().get("@vaadin/component-0"));
    }

    @Test
    public void should_summarize_when_MultipleViews() {
        FrontendDependencies deps = getFrontendDependencies(SecondView.class,
                FirstView.class);

        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());

        DepsTests.assertImportCount(9, deps.getModules());
        assertEquals(1, deps.getPackages().size());
        DepsTests.assertImportCount(6, deps.getScripts());
    }

    @Test
    public void should_visit_Constructor() {
        FrontendDependencies deps = getFrontendDependencies(SecondView.class);
        DepsTests.assertHasImports(deps.getModules(), "./component-3.js");
    }

    @Test
    public void should_resolveComponentFactories() {
        FrontendDependencies deps = getFrontendDependencies(ThirdView.class);

        assertEquals(0, deps.getPackages().size());
        DepsTests.assertImportCount(0, deps.getScripts());
        DepsTests.assertImportsExcludingUI(deps.getModules(),
                "./my-component.js", "./my-static-factory.js",
                "./my-another-component.js");
    }

    @Test
    public void should_notVisitNonAnnotatredClasses() {
        FrontendDependencies deps = getFrontendDependencies(
                UnAnnotatedClass.class);
        assertEquals("Only UI should be found", 1,
                deps.getEntryPoints().size());
    }

    @Test
    public void should_cacheVisitedClasses() {
        FrontendDependencies deps = getFrontendDependencies(
                RoutedClassWithoutAnnotations.class);
        assertEquals(2, deps.getEntryPoints().size());
        assertTrue("Should cache visited classes",
                deps.getClasses().size() > 2);
        assertTrue(deps.getClasses().contains(Route.class.getName()));
        assertTrue(deps.getClasses()
                .contains(RoutedClassWithoutAnnotations.class.getName()));
    }

    @Test
    public void should_cacheSuperVisitedClasses() {
        List<Class<?>> visited = Arrays.asList(Route.class, NoTheme.class,
                JsModule.class, RoutedClassWithAnnotations.class,
                RoutedClassWithoutAnnotations.class, RoutedClass.class,
                BridgeClass.class);

        // Visit a route that extends an extra routed class
        FrontendDependencies deps = getFrontendDependencies(RoutedClass.class);
        assertEquals("Should find RoutedClass and UI", 2,
                deps.getEntryPoints().size());
        int visitedClassesAmount = deps.getClasses().size();
        for (Class<?> clz : visited) {
            assertTrue("should cache " + clz.getName(),
                    deps.getClasses().contains(clz.getName()));
        }

        // Visit the same route but also the super routed class, the number of
        // visited classes should
        // be the same, but number of entry points increases
        deps = getFrontendDependencies(RoutedClassWithoutAnnotations.class,
                RoutedClass.class);
        assertEquals("Should contain UI, RoutedClass and its parent", 3,
                deps.getEntryPoints().size());
        assertEquals(visitedClassesAmount, deps.getClasses().size());
        for (Class<?> clz : visited) {
            assertTrue("should cache " + clz.getName(),
                    deps.getClasses().contains(clz.getName()));
        }
    }

    @Test
    public void should_visitDynamicRouteWithTwoImports() {
        FrontendDependencies deps = getFrontendDependencies(
                DynamicComponentClassWithTwoImports.class);
        DepsTests.assertImportsExcludingUI(deps.getModules(),
                "dynamic-component.js", "another-dynamic-component.js");
    }

    @Test // #5509
    public void should_visitDynamicRoute() {
        FrontendDependencies deps = getFrontendDependencies(
                RouteWithNestedDynamicRouteClass.class);
        DepsTests.assertImportsExcludingUI(deps.getModules(),
                "dynamic-route.js", "dynamic-component.js",
                "dynamic-layout.js");
    }

    @Test // #5658
    public void should_visitFactoryBeans() {
        FrontendDependencies deps = getFrontendDependencies(
                RouteWithViewBean.class);
        DepsTests.assertImportsExcludingUI(deps.getModules(),
                "dynamic-component.js");
    }

    @Test
    public void should_visitServices() {
        FrontendDependencies deps = getFrontendDependencies(
                RouteWithService.class);
        DepsTests.assertImportsExcludingUI(deps.getModules(),
                "dynamic-component.js", "dynamic-layout.js");
    }

    @Test
    public void should_visitMethodAnnotations() {
        FrontendDependencies deps = getFrontendDependencies(
                MethodAnnotationRoute.class);
        DepsTests.assertImportsExcludingUI(deps.getModules(),
                "./search-layout.js", "./search-field.js");
    }

    @Route
    public static class MethodAnnotationRoute {
        private MyPmo myPmo;
    }

    @UISearchLayout
    public static class MyPmo {

        @UISearchField()
        public String getText() {
            return "value to be displayed";
        }
    }

    public interface ComponentDefinitionCreator<T> {

    }

    public @interface LinkTo {
        Class<?> value();
    }

    @LinkTo(SearchFieldComponentDefinitionCreator.class)
    public @interface UISearchField {
        class SearchFieldComponentDefinitionCreator
                implements ComponentDefinitionCreator<UISearchField> {
            public Supplier<Component> create(UISearchField annotation,
                    AnnotatedElement annotatedElement) {
                return () -> new SearchField();
            }
        }

        @LinkTo(SearchLayoutComponentDefinitionCreator.class)
        public @interface UISearchLayout {
            class SearchLayoutComponentDefinitionCreator
                    implements ComponentDefinitionCreator<UISearchLayout> {
                public Supplier<Component> create(UISearchLayout annotation,
                        AnnotatedElement annotatedElement) {
                    return () -> new SearchLayout();
                }
            }
        }

        @JsModule("./search-layout.js")
        public static class SearchLayout extends Component {

        }

        @JsModule("./search-field.js")
        public static class SearchField extends Component {

        }

    }

}
