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
package com.vaadin.flow.server.frontend.scanner;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.frontend.scanner.samples.ErrorComponent;
import com.vaadin.flow.server.frontend.scanner.samples.JsModuleOrderComponent;
import com.vaadin.flow.server.frontend.scanner.samples.JsOrderComponent;
import com.vaadin.flow.server.frontend.scanner.samples.MyServiceListener;
import com.vaadin.flow.server.frontend.scanner.samples.MyUIInitListener;
import com.vaadin.flow.server.frontend.scanner.samples.RouteComponent;
import com.vaadin.flow.server.frontend.scanner.samples.RouteComponentWithMethodReference;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendDependenciesTest {

    private ClassFinder classFinder = Mockito.mock(ClassFinder.class);

    @BeforeEach
    void setUp() throws ClassNotFoundException {
        Mockito.when(classFinder.loadClass(Mockito.anyString()))
                .thenAnswer(q -> {
                    String className = q.getArgument(0);
                    if (className.equals(FrontendDependencies.LUMO)) {
                        return FakeLumo.class;
                    }
                    return Class.forName(className);
                });

        Mockito.doAnswer(invocation -> FrontendDependenciesTest.class
                .getClassLoader().getResource(invocation.getArgument(0)))
                .when(classFinder).getResource(Mockito.anyString());
        Mockito.when(classFinder.shouldInspectClass(Mockito.anyString()))
                .thenReturn(true);

        Mockito.when(classFinder.loadClass(UI.class.getName()))
                .thenReturn((Class) UI.class);
    }

    @Test
    void routedComponent_entryPointsAreCollected() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(RouteComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "foo.js");
        DepsTests.assertImports(dependencies.getScripts(), "bar.js");
    }

    @Test
    void appShellConfigurator_collectedAsEntryPoint()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppShell.class));
        Mockito.when(classFinder.loadClass(FakeLumo.class.getName()))
                .thenReturn((Class) FakeLumo.class);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        assertEquals(3, dependencies.getEntryPoints().size(),
                "UI, AppShell should be found");

        AbstractTheme theme = dependencies.getTheme();
        assertNotNull(theme, "Theme not found in entry point");

        ThemeDefinition themeDefinition = dependencies.getThemeDefinition();
        assertNotNull(themeDefinition, "ThemeDefinition is not filled");
        assertEquals(FakeLumo.class, themeDefinition.getTheme());
    }

    @Test
    void themeDefiningClassAndName_throwsException()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(FaultyThemeAnnotation.class));
        Mockito.when(classFinder.loadClass(FakeLumo.class.getName()))
                .thenReturn((Class) FakeLumo.class);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new FrontendDependencies(classFinder, false, null, true));

        assertEquals("Theme name and theme class can not both be specified. "
                + "Theme name uses Lumo and can not be used in combination with custom theme class.",
                exception.getMessage(),
                "Unexpected message for the thrown exception");
    }

    @Test
    void noDefaultThemeAvailable_throwsException()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppThemeShell.class));
        Mockito.when(classFinder.loadClass(FrontendDependencies.LUMO))
                .thenThrow(ClassNotFoundException.class);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new FrontendDependencies(classFinder, false, null, true));

        assertEquals(
                "Lumo dependency needs to be available on the classpath when using a theme name.",
                exception.getMessage(),
                "Thrown exception didn't contain correct message");
    }

    @Test
    void appThemeDefined_getsLumoAsTheme() {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppThemeShell.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        assertEquals(FakeLumo.class,
                dependencies.getThemeDefinition().getTheme(),
                "Faulty default theme received");
    }

    @Test
    void themeDefined_themeCssLoaded() {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppShell.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        boolean cssFound = false;
        for (ChunkInfo key : dependencies.getCss().keySet()) {
            cssFound = cssFound || dependencies.getCss().get(key).stream()
                    .anyMatch(css -> css.getValue()
                            .equals("@vaadin/vaadin-lumo-styles/lumo.css"));
        }

        assertTrue(cssFound);
    }

    @Test
    void themeNotDefined_ButReferenced_themeCssNotLoaded() {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(ThemeReferenceShell.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        boolean cssFound = false;
        for (ChunkInfo key : dependencies.getCss().keySet()) {
            cssFound = cssFound || dependencies.getCss().get(key).stream()
                    .anyMatch(css -> css.getValue()
                            .equals("@vaadin/vaadin-lumo-styles/lumo.css"));
        }

        assertFalse(cssFound);
    }

    @Test
    void onlyThemeVariantDefined_getsLumoAsTheme_preserveVariant() {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(ThemeVariantOnly.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        assertEquals(FakeLumo.class,
                dependencies.getThemeDefinition().getTheme(),
                "Faulty default theme received");
        assertEquals("dark", dependencies.getThemeDefinition().getVariant(),
                "Faulty variant received");
    }

    @Test
    void defaultThemeAnnotation_getsLumoAsTheme() {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(
                        Collections.singleton(DefaultThemeAnnotation.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        AbstractTheme theme = dependencies.getTheme();
        assertNotNull(theme,
                "Theme should be found for @Theme with default values");

        ThemeDefinition themeDefinition = dependencies.getThemeDefinition();
        assertNotNull(themeDefinition, "ThemeDefinition should be filled");
        assertEquals(FakeLumo.class, themeDefinition.getTheme(),
                "Should default to Lumo theme");
        assertEquals("", themeDefinition.getVariant(),
                "Variant should be empty");
        assertEquals("", themeDefinition.getName(),
                "Theme name should be empty");
    }

    @Test
    void hasErrorParameterComponent_entryPointIsCollected() {
        Mockito.when(classFinder.getSubTypesOf(HasErrorParameter.class))
                .thenReturn(Collections.singleton(ErrorComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);
        DepsTests.assertImportsExcludingUI(dependencies.getModules(),
                "./src/bar.js");
        DepsTests.assertImports(dependencies.getScripts(), "./src/baz.js");
    }

    @Test
    void componentInsideUiInitListener_entryPointsAreCollected() {
        Mockito.when(classFinder.getSubTypesOf(UIInitListener.class))
                .thenReturn(Collections.singleton(MyUIInitListener.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "baz.js");
        DepsTests.assertImports(dependencies.getScripts(), "foobar.js");

    }

    @Test
    void componentInsideUiInitListenerInsideServiceInitListener_entryPointsAreCollected() {
        Mockito.when(classFinder.getSubTypesOf(VaadinServiceInitListener.class))
                .thenReturn(Collections.singleton(MyServiceListener.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);
        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "baz.js");
        DepsTests.assertImports(dependencies.getScripts(), "foobar.js");
    }

    @Test
    void jsScriptOrderIsPreserved() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(JsOrderComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImports(dependencies.getScripts(), "a.js", "b.js",
                "c.js");
    }

    @Test
    void jsModuleOrderIsPreserved() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class)).thenReturn(
                Collections.singleton(JsModuleOrderComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "c.js",
                "b.js", "a.js");
    }

    // flow #6524
    @Test
    void extractsAndScansClassesFromMethodReferences() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class)).thenReturn(
                Collections.singleton(RouteComponentWithMethodReference.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "baz.js",
                "bar.js", "foo.js");
    }

    @Test
    void defaultThemeIsNotLoadedForExporters() throws Exception {
        FakeLumo.class.getDeclaredConstructor().newInstance();
        Mockito.when(classFinder.getSubTypesOf(WebComponentExporter.class))
                .thenReturn(Stream.of(MyExporter.class)
                        .collect(Collectors.toSet()));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, true, null, true);

        assertNull(dependencies.getTheme());
        assertNull(dependencies.getThemeDefinition());
    }

    @Test // #9861
    void collectEntryPoints_uiIsAlwaysCollected() {
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        Optional<EntryPointData> uiEndpointData = dependencies.getEntryPoints()
                .stream().filter(entryPoint -> entryPoint.getName()
                        .equals(UI.class.getName()))
                .findAny();
        assertTrue(uiEndpointData.isPresent(), "UI should be visited");
    }

    @Test // #9861
    void classInMultipleEntryPoints_collectEntryPointsNotOverrideInitial() {
        // Reference found through first entry point
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(TestRoute.class));
        // Reference found through second entry point, should not clear
        Mockito.when(classFinder.getSubTypesOf(HasErrorParameter.class))
                .thenReturn(Collections.singleton(TestRoute.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);
        DepsTests.assertImports(dependencies.getModules(), "reference.js",
                "@vaadin/common-frontend/ConnectionIndicator.js");
    }

    @Test // 20074
    void layoutClasses_collectedAsEntrypoint() {
        Mockito.when(classFinder.getAnnotatedClasses(Layout.class))
                .thenReturn(Collections.singleton(MainLayout.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        Optional<EntryPointData> layoutEndpointData = dependencies
                .getEntryPoints().stream().filter(entryPoint -> entryPoint
                        .getName().equals(MainLayout.class.getName()))
                .findAny();
        assertTrue(layoutEndpointData.isPresent(),
                "MainLayout should be visited");
        DepsTests.assertImports(dependencies.getModules(), "reference.js",
                "@vaadin/common-frontend/ConnectionIndicator.js");
    }

    @Test // #9861
    void visitedExporter_previousEntryPointsNotOverridden() throws Exception {

        FakeLumo.class.getDeclaredConstructor().newInstance();
        // Reference found through first entry point
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(ReferenceExporter.class));
        // Re-visit through exporter.
        Mockito.when(classFinder.getSubTypesOf(WebComponentExporter.class))
                .thenReturn(Stream.of(ReferenceExporter.class)
                        .collect(Collectors.toSet()));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, true, null, true);

        DepsTests.assertImports(dependencies.getModules(), "reference.js",
                "@vaadin/common-frontend/ConnectionIndicator.js");
    }

    @Test // #9861
    void shouldVisit_shouldNotMatchOnPartOfPackage() {

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, true, null, true);

        assertTrue(dependencies.shouldVisit("org.springseason.samples"),
                "second package should match fully not as starts with 'spring != springseason'");
        assertTrue(dependencies.shouldVisit("org.springseason"),
                "second package should match fully not as starts with 'spring != springseason'");
        assertFalse(dependencies.shouldVisit("org.spring"),
                "should not visit with only 2 packages 'org.spring'");

        assertTrue(dependencies.shouldVisit("com.sunny.app"),
                "second package should match fully not as starts with 'sun != sunny'");
        assertTrue(dependencies.shouldVisit("com.sunny"),
                "second package should match fully not as starts with 'sun != sunny'");
        assertFalse(dependencies.shouldVisit("com.sun"),
                "should not visit with only 2 packages 'com.sun'");
    }

    @Test
    void classScanningForChildAndParentEntryPoint_ordered_childrenSeeClassesFromParent() {
        LinkedHashSet<Class<?>> hierarchy = Stream
                .of(ParentRoute.class, AnnotatedChildRoute.class,
                        ChildRoute.class, GrandChildRoute.class)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(hierarchy);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        hierarchy.forEach(entryPointClass -> verifyEntryPointData(dependencies,
                entryPointClass));
    }

    @Test
    void classScanningForChildAndParentEntryPoint_shuffled_childrenSeeClassesFromParent() {
        LinkedHashSet<Class<?>> hierarchy = Stream
                .of(GrandChildRoute.class, AnnotatedChildRoute.class,
                        ParentRoute.class, ChildRoute.class)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(hierarchy);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        hierarchy.forEach(entryPointClass -> verifyEntryPointData(dependencies,
                entryPointClass));
    }

    @Test
    void classScanningForChildAndParentEntryPoint_reversed_childrenSeeClassesFromParent() {
        LinkedHashSet<Class<?>> hierarchy = Stream
                .of(GrandChildRoute.class, ChildRoute.class,
                        AnnotatedChildRoute.class, ParentRoute.class)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(hierarchy);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        hierarchy.forEach(entryPointClass -> verifyEntryPointData(dependencies,
                entryPointClass));
    }

    @Test
    void classScanningForNpmPackage_collectsNpmAssets()
            throws ClassNotFoundException {
        LinkedHashSet<Class<?>> hierarchy = Stream.of(Assets.class)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Mockito.when(
                classFinder.getAnnotatedClasses(NpmPackage.class.getName()))
                .thenReturn(hierarchy);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        assertEquals(1, dependencies.getAssets().size());
        assertEquals(1, dependencies.getAssets().get("images").size());
        assertEquals("images/22x25/**:22x25",
                dependencies.getAssets().get("images").get(0));
    }

    @Test
    void classScanningForNpmPackage_duplicatePackages_collectsAllNpmAssets()
            throws ClassNotFoundException {
        LinkedHashSet<Class<?>> hierarchy = Stream
                .of(Assets.class, DuplicatedAssets.class)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Mockito.when(
                classFinder.getAnnotatedClasses(NpmPackage.class.getName()))
                .thenReturn(hierarchy);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        assertEquals(1, dependencies.getAssets().size());
        assertEquals(2, dependencies.getAssets().get("images").size());
        assertTrue(dependencies.getAssets().get("images")
                .contains("images/22x25/**:22x25"));
        assertTrue(dependencies.getAssets().get("images")
                .contains("images/28x28/**:28x28"));

    }

    private static EntryPointData getEntryPointByClass(
            FrontendDependencies dependencies, Class<?> entryPointClass) {
        Optional<EntryPointData> childEntryPoint = dependencies.getEntryPoints()
                .stream().filter(entryPoint -> entryPoint.getName()
                        .equals(entryPointClass.getName()))
                .findAny();
        assertTrue(childEntryPoint.isPresent());
        return childEntryPoint.get();
    }

    private static void verifyEntryPointData(FrontendDependencies dependencies,
            Class<?> entryPointClass) {
        EntryPointData entryPointData = getEntryPointByClass(dependencies,
                entryPointClass);

        assertNotNull(entryPointData.reachableClasses);
        assertFalse(entryPointData.reachableClasses.isEmpty());

        // Child entrypoint should see classes reachable from parent entrypoint,
        // not only the parent class
        assertTrue(entryPointData.reachableClasses.size() > 1);

        assertNotNull(entryPointData.getModules());
        assertEquals(1, entryPointData.getModules().size());
        assertEquals("reference.js",
                entryPointData.getModules().iterator().next());
    }

    public static class MyComponent extends Component {
    }

    public static class MyExporter extends WebComponentExporter<MyComponent> {
        public MyExporter() {
            super("tag-tag");
        }

        @Override
        protected void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {
        }
    }

    @CssImport("@vaadin/vaadin-lumo-styles/lumo.css")
    public static class FakeLumo implements AbstractTheme {
        public FakeLumo() {
        }

        @Override
        public String getBaseUrl() {
            return null;
        }

        @Override
        public String getThemeUrl() {
            return null;
        }
    }

    public static class ThemeReferenceShell implements AppShellConfigurator {
        FakeLumo lumo = new FakeLumo();
    }

    @Theme(themeClass = FakeLumo.class)
    public static class MyAppShell implements AppShellConfigurator {
    }

    @Theme("my-theme")
    public static class MyAppThemeShell implements AppShellConfigurator {
    }

    @Theme(value = "my-theme", themeClass = FakeLumo.class)
    public static class FaultyThemeAnnotation implements AppShellConfigurator {
    }

    @Theme(variant = "dark")
    public static class ThemeVariantOnly implements AppShellConfigurator {
    }

    @Theme
    public static class DefaultThemeAnnotation implements AppShellConfigurator {
    }

    @JsModule("reference.js")
    @Tag("div")
    public static class Referenced extends Component {
    }

    @Route("reference")
    public static class ReferenceExporter
            extends WebComponentExporter<MyComponent> {
        public ReferenceExporter() {
            super("tag-tag");
        }

        @Override
        protected void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {
            Referenced ref = new Referenced();
        }
    }

    @Route("reference")
    @Tag("div")
    public static class TestRoute extends Component
            implements HasErrorParameter<NotFoundException> {
        Referenced ref;

        public TestRoute() {
            ref = new Referenced();
        }

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            return 0;
        }
    }

    @Route("parent")
    public static class ParentRoute extends Component implements HasComponents {
        private Referenced myComponent = new Referenced();

        public ParentRoute() {
            add(myComponent);
        }
    }

    @Route("child")
    public static class AnnotatedChildRoute extends ParentRoute {
    }

    public static class ChildRoute extends ParentRoute {
    }

    public static class GrandChildRoute extends ChildRoute {
    }

    @Tag("div")
    @Layout
    @JsModule("reference.js")
    public static class MainLayout extends Component implements RouterLayout {
    }

    @NpmPackage(value = "images", version = "1.1.1", assets = {
            "images/22x25/**:22x25" })
    @Tag("div")
    public static class Assets extends Component {
    }

    @Tag("div")
    @NpmPackage(value = "images", version = "1.1.1", assets = {
            "images/28x28/**:28x28" })
    public static class DuplicatedAssets extends Component {
    }
}
