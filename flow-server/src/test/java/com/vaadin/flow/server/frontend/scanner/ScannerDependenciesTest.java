package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.BridgeClass;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Component0;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Component1;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Component2;
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
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.theme.NoTheme;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScannerDependenciesTest {

    static FrontendDependencies getFrontendDependencies(Class<?>... classes) throws Exception {
        FrontendDependencies frontendDependencies = new FrontendDependencies(
                new DefaultClassFinder(new HashSet<>(
                        new ArrayList<>(Arrays.asList(classes)))));
        return frontendDependencies;
    }

    @Test
    public void visitRouteEntryPoint_ExpectToAlsoVisitImplementedInterface() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(RouteInterfaceComponent.class);

        assertTrue("Missing dependency from implemented interface",
                deps.getModules().contains("myModule.js"));

        assertEquals("There should be 1 css import", 1, deps.getCss().size());
        
        assertEquals("Invalid css import", "frontend://styles/interface.css",
                deps.getCss().iterator().next().value);
    }

    @Test
    public void should_extractClassesFromSignatures() {
        Set<String> classes = new HashSet<>();
        FrontendClassVisitor visitor = new FrontendClassVisitor(null, null, false);

        visitor.addSignatureToClasses(classes,
                "(Lcom/vaadin/flow/component/tabs/Tabs;Ljava/lang/String;Ljava/lang/Character;CLjava/lang/Integer;ILjava/lang/Long;JLjava/lang/Double;DLjava/lang/Float;FLjava/lang/Byte;BLjava/lang/Boolean;Z)Lcom/vaadin/flow/component/button/Button;");
        assertEquals(11, classes.size());
        assertArrayEquals(new String[] { "", "java.lang.Float",
                "com.vaadin.flow.component.button.Button",
                "java.lang.Character", "java.lang.Long", "java.lang.Double",
                "java.lang.Boolean", "com.vaadin.flow.component.tabs.Tabs",
                "java.lang.String", "java.lang.Byte", "java.lang.Integer" },
                classes.toArray());

        visitor.addSignatureToClasses(classes,
                "([Lcom/vaadin/flow/component/Component;)V");
        assertEquals(12, classes.size());
        assertTrue(classes.contains("com.vaadin.flow.component.Component"));

        visitor.addSignatureToClasses(classes,
                "(Lcom/vaadin/flow/component/orderedlayout/FlexComponent$Alignment;[Lcom/vaadin/flow/component/Component;)");
        assertEquals(13, classes.size());
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
    public void should_visitNpmPakageAnnotations() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(Component1.class, Component2.class);
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
    public void should_visitSuperNpmPakageAnnotations() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(
                ScannerTestComponents.ComponentExtending.class);
        assertEquals(1, deps.getPackages().size());
        assertTrue(
                deps.getPackages().containsKey("@vaadin/component-extended"));

        assertEquals("2.1.0",
                deps.getPackages().get("@vaadin/component-extended"));
    }

    @Test
    public void when_MultipleVersions_should_returnFirstVisitedOne()
            throws Exception {
        FrontendDependencies deps = getFrontendDependencies(Component0.class);
        assertEquals("=2.1.0", deps.getPackages().get("@vaadin/component-0"));
    }


    @Test
    public void should_summarize_when_MultipleViews() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(SecondView.class, FirstView.class);

        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());

        assertEquals(8, deps.getModules().size());
        assertEquals(1, deps.getPackages().size());
        assertEquals(6, deps.getScripts().size());
    }

    @Test
    public void should_visit_Constructor() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(SecondView.class);
        assertTrue(deps.getModules().contains("./component-3.js"));
    }

    @Test
    public void should_resolveComponentFactories() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(ThirdView.class);

        assertEquals(3, deps.getModules().size());
        assertEquals(0, deps.getPackages().size());
        assertEquals(0, deps.getScripts().size());
        assertTrue(deps.getModules().contains("./my-component.js"));
        assertTrue(deps.getModules().contains("./my-static-factory.js"));
        assertTrue(deps.getModules().contains("./my-another-component.js"));
    }

    @Test
    public void should_notVisitNonAnnotatredClasses() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(UnAnnotatedClass.class);
        assertEquals(0, deps.getEndPoints().size());
        assertEquals(0, deps.getClasses().size());
    }

    @Test
    public void should_cacheVisitedClasses() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(RoutedClassWithoutAnnotations.class);
        assertEquals(1, deps.getEndPoints().size());
        assertEquals(2, deps.getClasses().size());
        assertTrue(deps.getClasses().contains(Route.class.getName()));
        assertTrue(deps.getClasses().contains(RoutedClassWithoutAnnotations.class.getName()));
    }

    @Test
    public void should_cacheSuperVisitedClasses() throws Exception {
        List<Class<?>> visited = Arrays.asList(Route.class, NoTheme.class, JsModule.class, LoadMode.class,
                RoutedClassWithAnnotations.class, RoutedClassWithoutAnnotations.class, RoutedClass.class,
                BridgeClass.class);

        // Visit a route that extends an extra routed class
        FrontendDependencies deps = getFrontendDependencies(RoutedClass.class);
        assertEquals(1, deps.getEndPoints().size());
        assertEquals(9, deps.getClasses().size());
        for (Class<?> clz : visited) {
            assertTrue("should cache " + clz.getName(), deps.getClasses().contains(clz.getName()));
        }

        // Visit the same route but also the super routed class, the number of visited classes should
        // be the same, but number of entry points increases
        deps = getFrontendDependencies(RoutedClassWithoutAnnotations.class, RoutedClass.class);
        assertEquals(2, deps.getEndPoints().size());
        assertEquals(9, deps.getClasses().size());
        for (Class<?> clz : visited) {
            assertTrue("should cache " + clz.getName(), deps.getClasses().contains(clz.getName()));
        }
    }

    @Test // #5509
    public void should_visitDynamicRoute() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(RouteWithNestedDynamicRouteClass.class);
        assertEquals(3, deps.getModules().size());
        assertTrue(deps.getModules().contains("dynamic-route.js"));
        assertTrue(deps.getModules().contains("dynamic-component.js"));
        assertTrue(deps.getModules().contains("dynamic-layout.js"));
    }

    @Test // #5658
    public void should_visitFactoryBeans() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(RouteWithViewBean.class);
        assertEquals(1, deps.getModules().size());
        assertTrue(deps.getModules().contains("dynamic-component.js"));
    }

    @Test
    public void should_visitServices() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(RouteWithService.class);
        assertEquals(2, deps.getModules().size());
        assertTrue(deps.getModules().contains("dynamic-component.js"));
        assertTrue(deps.getModules().contains("dynamic-layout.js"));
    }
}
