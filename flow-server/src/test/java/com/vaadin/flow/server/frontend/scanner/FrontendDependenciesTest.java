/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
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

public class FrontendDependenciesTest {

    private ClassFinder classFinder = Mockito.mock(ClassFinder.class);

    @Before
    public void setUp() throws ClassNotFoundException {
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

        Mockito.when(classFinder.loadClass(UI.class.getName()))
                .thenReturn((Class) UI.class);
    }

    @Test
    public void routedComponent_entryPointsAreCollected() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(RouteComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "foo.js");
        DepsTests.assertImports(dependencies.getScripts(), "bar.js");
    }

    @Test
    public void appShellConfigurator_collectedAsEntryPoint()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppShell.class));
        Mockito.when(classFinder.loadClass(FakeLumo.class.getName()))
                .thenReturn((Class) FakeLumo.class);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        Assert.assertEquals("UI, AppShell should be found", 2,
                dependencies.getEntryPoints().size());

        AbstractTheme theme = dependencies.getTheme();
        Assert.assertNotNull("Theme not found in entry point", theme);

        ThemeDefinition themeDefinition = dependencies.getThemeDefinition();
        Assert.assertNotNull("ThemeDefinition is not filled", themeDefinition);
        Assert.assertEquals(FakeLumo.class, themeDefinition.getTheme());
    }

    @Test
    public void themeDefiningClassAndName_throwsException()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(FaultyThemeAnnotation.class));
        Mockito.when(classFinder.loadClass(FakeLumo.class.getName()))
                .thenReturn((Class) FakeLumo.class);

        IllegalStateException exception = Assert.assertThrows(
                IllegalStateException.class,
                () -> new FrontendDependencies(classFinder, false));

        Assert.assertEquals("Unexpected message for the thrown exception",
                "Theme name and theme class can not both be specified. "
                        + "Theme name uses Lumo and can not be used in combination with custom theme class.",
                exception.getMessage());
    }

    @Test
    public void noDefaultThemeAvailable_throwsException()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppThemeShell.class));
        Mockito.when(classFinder.loadClass(FrontendDependencies.LUMO))
                .thenThrow(ClassNotFoundException.class);

        IllegalStateException exception = Assert.assertThrows(
                IllegalStateException.class,
                () -> new FrontendDependencies(classFinder, false));

        Assert.assertEquals("Thrown exception didn't contain correct message",
                "Lumo dependency needs to be available on the classpath when using a theme name.",
                exception.getMessage());
    }

    @Test
    public void appThemeDefined_getsLumoAsTheme() {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppThemeShell.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        Assert.assertEquals("Faulty default theme received", FakeLumo.class,
                dependencies.getThemeDefinition().getTheme());

    }

    @Test
    public void onlyThemeVariantDefined_getsLumoAsTheme_preserveVariant() {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(ThemeVariantOnly.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        Assert.assertEquals("Faulty default theme received", FakeLumo.class,
                dependencies.getThemeDefinition().getTheme());
        Assert.assertEquals("Faulty variant received", "dark",
                dependencies.getThemeDefinition().getVariant());

    }

    @Test
    public void hasErrorParameterComponent_entryPointIsCollected() {
        Mockito.when(classFinder.getSubTypesOf(HasErrorParameter.class))
                .thenReturn(Collections.singleton(ErrorComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);
        DepsTests.assertImportsExcludingUI(dependencies.getModules(),
                "./src/bar.js");
        DepsTests.assertImports(dependencies.getScripts(), "./src/baz.js");
    }

    @Test
    public void componentInsideUiInitListener_entryPointsAreCollected() {
        Mockito.when(classFinder.getSubTypesOf(UIInitListener.class))
                .thenReturn(Collections.singleton(MyUIInitListener.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "baz.js");
        DepsTests.assertImports(dependencies.getScripts(), "foobar.js");

    }

    @Test
    public void componentInsideUiInitListenerInsideServiceInitListener_entryPointsAreCollected() {
        Mockito.when(classFinder.getSubTypesOf(VaadinServiceInitListener.class))
                .thenReturn(Collections.singleton(MyServiceListener.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);
        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "baz.js");
        DepsTests.assertImports(dependencies.getScripts(), "foobar.js");
    }

    @Test
    public void jsScriptOrderIsPreserved() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(JsOrderComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        DepsTests.assertImports(dependencies.getScripts(), "a.js", "b.js",
                "c.js");
    }

    @Test
    public void jsModuleOrderIsPreserved() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class)).thenReturn(
                Collections.singleton(JsModuleOrderComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "c.js",
                "b.js", "a.js");
    }

    // flow #6524
    @Test
    public void extractsAndScansClassesFromMethodReferences() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class)).thenReturn(
                Collections.singleton(RouteComponentWithMethodReference.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "foo.js",
                "baz.js", "bar.js");
    }

    @Test
    public void defaultThemeIsLoadedForExporters() throws Exception {
        FakeLumo.class.newInstance();
        Mockito.when(classFinder.getSubTypesOf(WebComponentExporter.class))
                .thenReturn(Stream.of(MyExporter.class)
                        .collect(Collectors.toSet()));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, true);

        Assert.assertNotNull(dependencies.getTheme());
        Assert.assertNotNull(dependencies.getThemeDefinition());
    }

    @Test // #9861
    public void collectEntryPoints_uiIsAlwaysCollected() {
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        Optional<EntryPointData> uiEndpointData = dependencies.getEntryPoints()
                .stream().filter(entryPoint -> entryPoint.getName()
                        .equals(UI.class.getName()))
                .findAny();
        Assert.assertTrue("UI should be visited", uiEndpointData.isPresent());
    }

    @Test // #9861
    public void classInMultipleEntryPoints_collectEntryPointsNotOverrideInitial() {
        // Reference found through first entry point
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(TestRoute.class));
        // Reference found through second entry point, should not clear
        Mockito.when(classFinder.getSubTypesOf(HasErrorParameter.class))
                .thenReturn(Collections.singleton(TestRoute.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);
        DepsTests.assertImports(dependencies.getModules(), "reference.js",
                "@vaadin/common-frontend/ConnectionIndicator.js");
    }

    @Test // #9861
    public void visitedExporter_previousEntryPointsNotOverridden()
            throws InstantiationException, IllegalAccessException {

        FakeLumo.class.newInstance();
        // Reference found through first entry point
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(ReferenceExporter.class));
        // Re-visit through exporter.
        Mockito.when(classFinder.getSubTypesOf(WebComponentExporter.class))
                .thenReturn(Stream.of(ReferenceExporter.class)
                        .collect(Collectors.toSet()));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, true);

        DepsTests.assertImports(dependencies.getModules(), "reference.js",
                "@vaadin/common-frontend/ConnectionIndicator.js");
    }

    @Test // #9861
    public void shouldVisit_shouldNotMatchOnPartOfPackage() {

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, true);

        Assert.assertTrue(
                "second package should match fully not as starts with 'spring != springframework'",
                dependencies.shouldVisit("org.springframework.samples"));
        Assert.assertTrue(
                "second package should match fully not as starts with 'spring != springframework'",
                dependencies.shouldVisit("org.springframework"));
        Assert.assertFalse("should not visit with only 2 packages 'org.spring'",
                dependencies.shouldVisit("org.spring"));

        Assert.assertTrue(
                "second package should match fully not as starts with 'sun != sunny'",
                dependencies.shouldVisit("com.sunny.app"));
        Assert.assertTrue(
                "second package should match fully not as starts with 'sun != sunny'",
                dependencies.shouldVisit("com.sunny"));
        Assert.assertFalse("should not visit with only 2 packages 'com.sun'",
                dependencies.shouldVisit("com.sun"));
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

}
