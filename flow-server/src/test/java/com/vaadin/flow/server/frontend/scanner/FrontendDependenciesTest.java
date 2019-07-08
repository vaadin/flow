package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.BridgeClass;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.Component0;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.Component1;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.Component2;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.CssClass1;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.CssClass2;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.FirstView;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.NoThemeExporter;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.RootView2WithLayoutTheme;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.RootViewWithLayoutTheme;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.RootViewWithMultipleTheme;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.RootViewWithTheme;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.RootViewWithoutTheme;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.RootViewWithoutThemeAnnotation;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.RoutedClass;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.RoutedClassWithAnnotations;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.RoutedClassWithoutAnnotations;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.SecondView;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.Theme1;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.Theme2;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.Theme4;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.ThemeExporter;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.ThirdView;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.UnAnnotatedClass;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.theme.NoTheme;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FrontendDependenciesTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private FrontendDependencies create(Class<?>... classes) throws Exception {
        FrontendDependencies frontendDependencies = new FrontendDependencies(
                new DefaultClassFinder(new HashSet<>(
                        new ArrayList<>(Arrays.asList(classes)))));
        return frontendDependencies;
    }

    @Test
    public void should_extractClassesFromSignatures() {
        Set<String> classes = new HashSet<>();
        FrontendClassVisitor visitor = new FrontendClassVisitor(null, null);

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
        FrontendDependencies deps = create(Component1.class, Component2.class);
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
        FrontendDependencies deps = create(
                FrontendDependenciesTestComponents.ComponentExtending.class);
        assertEquals(1, deps.getPackages().size());
        assertTrue(
                deps.getPackages().containsKey("@vaadin/component-extended"));

        assertEquals("2.1.0",
                deps.getPackages().get("@vaadin/component-extended"));
    }

    @Test
    public void when_MultipleVersions_should_returnFirstVisitedOne()
            throws Exception {
        FrontendDependencies deps = create(Component0.class);
        assertEquals("=2.1.0", deps.getPackages().get("@vaadin/component-0"));
    }

    @Test
    public void should_takeThemeFromTheView() throws Exception {
        FrontendDependencies deps = create(RootViewWithTheme.class);

        assertEquals(Theme4.class, deps.getThemeDefinition().getTheme());

        assertEquals(1, deps.getModules().size());
        assertTrue(deps.getModules().contains("./theme-4.js"));

        assertEquals(0, deps.getPackages().size());

        assertEquals(1, deps.getScripts().size());
        assertTrue(deps.getScripts().contains("frontend://theme-0.js"));
    }

    @Test
    public void should_not_takeTheme_when_NoTheme() throws Exception {
        FrontendDependencies deps = create(RootViewWithoutTheme.class);
        assertNull(deps.getThemeDefinition());

        assertEquals(2, deps.getModules().size());
        assertEquals(0, deps.getPackages().size());
        assertEquals(2, deps.getScripts().size());
    }

    @Test
    public void should_takeThemeFromLayout() throws Exception {
        FrontendDependencies deps = create(RootViewWithLayoutTheme.class);
        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());
        assertEquals(Theme1.DARK, deps.getThemeDefinition().getVariant());

        assertEquals(8, deps.getModules().size());
        assertEquals(1, deps.getPackages().size());
        assertEquals(6, deps.getScripts().size());

        assertTrue(deps.getPackages().containsKey("@foo/first-view"));
        assertEquals("0.0.1", deps.getPackages().get("@foo/first-view"));
    }


    @Test
    public void should_takeThemeWhenMultipleTheme() throws Exception {
        FrontendDependencies deps = create(RootViewWithMultipleTheme.class);
        assertEquals(Theme2.class, deps.getThemeDefinition().getTheme());
        assertEquals(Theme2.FOO, deps.getThemeDefinition().getVariant());

        assertEquals(4, deps.getModules().size());
        assertEquals(0, deps.getPackages().size());
        assertEquals(2, deps.getScripts().size());
    }

    @Test
    public void should_takeTheme_when_AnyRouteValue() throws Exception {
        FrontendDependencies deps = create(SecondView.class);

        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());

        assertEquals(4, deps.getModules().size());
        assertEquals(0, deps.getPackages().size());
        assertEquals(2, deps.getScripts().size());
    }

    @Test
    public void should_throw_when_MultipleThemes() throws Exception {
        exception.expect(IllegalStateException.class);
        create(RootViewWithMultipleTheme.class, FirstView.class);
    }

    @Test
    public void should_throw_when_ThemeAndNoTheme() throws Exception {
        exception.expect(IllegalStateException.class);
        create(FirstView.class, RootViewWithoutTheme.class);
    }

    @Test
    public void should_summarize_when_MultipleViews() throws Exception {
        FrontendDependencies deps = create(SecondView.class, FirstView.class);

        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());

        assertEquals(8, deps.getModules().size());
        assertEquals(1, deps.getPackages().size());
        assertEquals(6, deps.getScripts().size());
    }

    @Test
    public void should_resolveComponentFactories() throws Exception {
        FrontendDependencies deps = create(ThirdView.class);

        assertEquals(3, deps.getModules().size());
        assertEquals(0, deps.getPackages().size());
        assertEquals(0, deps.getScripts().size());
        assertTrue(deps.getModules().contains("./my-component.js"));
        assertTrue(deps.getModules().contains("./my-static-factory.js"));
        assertTrue(deps.getModules().contains("./my-another-component.js"));
    }

    @Test
    public void should_visitDefaultTheme_when_noThemeAnnotationIsGiven()
            throws Exception {

        DefaultClassFinder finder = spy(new DefaultClassFinder(
                Collections.singleton(RootViewWithoutThemeAnnotation.class)));

        // we'll do a partial mock here since we want to keep the other
        // behavior of the DefaultClassFinder. Theme4 is used as a fake Lumo
        // since it has @JsModule annotation which makes it easy to verify
        // that the Theme was actually visited and modules collected
        Mockito.doReturn(Theme4.class).when(finder)
                .loadClass(FrontendDependencies.LUMO);

        FrontendDependencies deps = new FrontendDependencies(finder);
        assertEquals(
                "Theme4 should have been returned when default theme was selected",
                Theme4.class, deps.getThemeDefinition().getTheme());
        assertTrue("Theme4 should have been visited and JsModule collected",
                deps.getModules().contains("./theme-4.js"));
    }

    @Test
    public void should_takeThemeFromExporter_when_exporterFound()
            throws Exception {
        FrontendDependencies deps = create(ThemeExporter.class);

        assertEquals(Theme2.class, deps.getThemeDefinition().getTheme());
    }

    @Test
    public void should_defaultToLumoTheme_when_noThemeDefinedByExporter()
            throws Exception {
        // RootViewWithTheme is added to the list just to make sure exporter
        // handles theming default, not the other crawlers
        DefaultClassFinder finder = spy(new DefaultClassFinder(
                new HashSet<>(Arrays.asList(NoThemeExporter.class,
                        RootViewWithTheme.class))));

        Mockito.doReturn(Theme4.class).when(finder)
                .loadClass(FrontendDependencies.LUMO);

        FrontendDependencies deps = new FrontendDependencies(finder);
        assertEquals(
                "Theme4 should have been returned when default theme was selected",
                Theme4.class, deps.getThemeDefinition().getTheme());
        assertTrue("Theme4 should have been visited and JsModule collected",
                deps.getModules().contains("./theme-4.js"));
    }

    @Test // flow#5715
    public void should_notAttemptToOverrideTheme_when_noExportersFound()
            throws ClassNotFoundException {
        DefaultClassFinder finder = spy(new DefaultClassFinder(
                Collections.singleton(RootViewWithTheme.class)));

        new FrontendDependencies(finder);
        verify(finder, times(0)).loadClass(FrontendDependencies.LUMO);
    }

    @Test
    public void should_notVisitNonAnnotatredClasses() throws Exception {
        FrontendDependencies deps = create(UnAnnotatedClass.class);
        assertEquals(0, deps.getEndPoints().size());
        assertEquals(0, deps.getClasses().size());
    }

    @Test
    public void should_cacheVisitedClasses() throws Exception {
        FrontendDependencies deps = create(RoutedClassWithoutAnnotations.class);
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
        FrontendDependencies deps = create(RoutedClass.class);
        assertEquals(1, deps.getEndPoints().size());
        assertEquals(9, deps.getClasses().size());
        for (Class<?> clz : visited) {
            assertTrue("should cache " + clz.getName(), deps.getClasses().contains(clz.getName()));
        }

        // Visit the same route but also the super routed class, the number of visited classes should
        // be the same, but number of entry points increases
        deps = create(RoutedClassWithoutAnnotations.class, RoutedClass.class);
        assertEquals(2, deps.getEndPoints().size());
        assertEquals(9, deps.getClasses().size());
        for (Class<?> clz : visited) {
            assertTrue("should cache " + clz.getName(), deps.getClasses().contains(clz.getName()));
        }
    }

    @Test
    public void should_takeThemeFromLayout_ifLayoutAlreadyVisited() throws Exception {
        // Make sure that all entry-points sharing layouts are correctly theming-configured
        FrontendDependencies deps = create(RootViewWithLayoutTheme.class, RootView2WithLayoutTheme.class);
        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());
        deps.getEndPoints().forEach(endPoint -> {
            assertEquals(Theme1.class.getName(), endPoint.getTheme().getName());
        });
    }

    @Test
    public void should_visitCssImports() throws Exception {
        FrontendDependencies deps = create(CssClass1.class, CssClass2.class);
        assertEquals(2, deps.getEndPoints().size());
        deps.getEndPoints().forEach(endPoint -> {
            assertEquals(4, endPoint.getCss().size());

            Iterator<CssData> it = endPoint.getCss().iterator();
            assertEquals("value: ./foo.css", it.next().toString());
            assertEquals("value: ./foo.css include:bar", it.next().toString());
            assertEquals("value: ./foo.css id:bar", it.next().toString());
            assertEquals("value: ./foo.css themefor:bar", it.next().toString());

        });
    }


    @Test
    public void should_sumarizeCssImports() throws Exception {
        FrontendDependencies deps = create(CssClass1.class, CssClass2.class);
        assertEquals(4, deps.getCss().size());
    }

}
