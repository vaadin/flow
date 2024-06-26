/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.frontend.scanner.samples.ErrorComponent;
import com.vaadin.flow.server.frontend.scanner.samples.JsOrderComponent;
import com.vaadin.flow.server.frontend.scanner.samples.MyServiceListener;
import com.vaadin.flow.server.frontend.scanner.samples.MyUIInitListener;
import com.vaadin.flow.server.frontend.scanner.samples.RouteComponent;
import com.vaadin.flow.server.frontend.scanner.samples.RouteComponentWithLayout;
import com.vaadin.flow.server.frontend.scanner.samples.RouteComponentWithMethodReference;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

import static org.hamcrest.CoreMatchers.is;

public class FrontendDependenciesTest {

    private ClassFinder classFinder = Mockito.mock(ClassFinder.class);

    @Before
    public void setUp() throws ClassNotFoundException {
        Mockito.when(classFinder.loadClass(Route.class.getName()))
                .thenReturn((Class) Route.class);

        Mockito.when(classFinder.loadClass(UIInitListener.class.getName()))
                .thenReturn((Class) UIInitListener.class);

        Mockito.when(classFinder
                .loadClass(VaadinServiceInitListener.class.getName()))
                .thenReturn((Class) VaadinServiceInitListener.class);

        Mockito.when(
                classFinder.loadClass(WebComponentExporter.class.getName()))
                .thenReturn((Class) WebComponentExporter.class);

        Mockito.when(classFinder.loadClass(HasErrorParameter.class.getName()))
                .thenReturn((Class) HasErrorParameter.class);

        Mockito.when(classFinder.loadClass(FrontendDependencies.LUMO))
                .thenReturn((Class) FakeLumo.class);

        Mockito.doAnswer(invocation -> FrontendDependenciesTest.class
                .getClassLoader().getResource(invocation.getArgument(0)))
                .when(classFinder).getResource(Mockito.anyString());

    }

    @Test
    public void routedComponent_endpointsAreCollected() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(RouteComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);
        List<String> modules = dependencies.getModules();
        Assert.assertEquals(1, modules.size());
        Assert.assertEquals("foo.js", modules.get(0));

        Set<String> scripts = dependencies.getScripts();
        Assert.assertEquals(1, scripts.size());
        Assert.assertEquals("bar.js", scripts.iterator().next());
    }

    @Test
    public void hasErrorParameterComponent_endpointIsCollected() {
        Mockito.when(classFinder.getSubTypesOf(HasErrorParameter.class))
                .thenReturn(Collections.singleton(ErrorComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);
        List<String> modules = dependencies.getModules();
        Assert.assertEquals(1, modules.size());
        Assert.assertEquals("./src/bar.js", modules.get(0));

        Set<String> scripts = dependencies.getScripts();
        Assert.assertEquals(1, scripts.size());
        Assert.assertEquals("./src/baz.js", scripts.iterator().next());
    }

    @Test
    public void componentInsideUiInitListener_endpointsAreCollected() {
        Mockito.when(classFinder.getSubTypesOf(UIInitListener.class))
                .thenReturn(Collections.singleton(MyUIInitListener.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);
        List<String> modules = dependencies.getModules();
        Assert.assertEquals(1, modules.size());
        Assert.assertEquals("baz.js", modules.get(0));

        Set<String> scripts = dependencies.getScripts();
        Assert.assertEquals(1, scripts.size());
        Assert.assertEquals("foobar.js", scripts.iterator().next());
    }

    @Test
    public void componentInsideUiInitListenerInsideServiceInitListener_endpointsAreCollected() {
        Mockito.when(classFinder.getSubTypesOf(VaadinServiceInitListener.class))
                .thenReturn(Collections.singleton(MyServiceListener.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);
        List<String> modules = dependencies.getModules();
        Assert.assertEquals(1, modules.size());
        Assert.assertEquals("baz.js", modules.get(0));

        Set<String> scripts = dependencies.getScripts();
        Assert.assertEquals(1, scripts.size());
        Assert.assertEquals("foobar.js", scripts.iterator().next());
    }

    @Test
    public void jsScriptOrderIsPreserved() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(JsOrderComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        Set<String> scripts = dependencies.getScripts();
        Assert.assertEquals(LinkedHashSet.class, scripts.getClass());

        Assert.assertEquals(new ArrayList<>(dependencies.getScripts()),
                Arrays.asList("a.js", "b.js", "c.js"));
    }

    // flow #6408
    @Test
    public void annotationsInRouterLayoutWontBeFlaggedAsBelongingToTheme() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class)).thenReturn(
                Collections.singleton(RouteComponentWithLayout.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        List<String> expectedOrder = Arrays.asList("theme-foo.js", "foo.js");
        Assert.assertThat("Theme's annotations should come first",
                dependencies.getModules(), is(expectedOrder));
    }

    // flow #6524
    @Test
    public void extractsAndScansClassesFromMethodReferences() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class)).thenReturn(
                Collections.singleton(RouteComponentWithMethodReference.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        List<String> modules = dependencies.getModules();
        Assert.assertEquals(3, modules.size());
        Assert.assertTrue(modules.contains("foo.js"));
        Assert.assertTrue(modules.contains("bar.js"));
        Assert.assertTrue(modules.contains("baz.js"));
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

    @Test
    public void themeDefiningNonLumoClassAndName_throwsException()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(FaultyThemeAnnotation.class));
        Mockito.when(classFinder.loadClass(FakeLumo.class.getName()))
                .thenReturn((Class) FakeLumo.class);

        Mockito.when(classFinder.loadClass(MyTheme.class.getName()))
                .thenReturn((Class) MyTheme.class);

        IllegalStateException exception = Assert.assertThrows(
                IllegalStateException.class,
                () -> new FrontendDependencies(classFinder, false));

        Assert.assertEquals("Unexpected message for the thrown exception",
                "Theme name and theme class can not both be specified. "
                        + "Theme name uses Lumo and can not be used in combination with"
                        + " custom theme class that doesn't extend Lumo.",
                exception.getMessage());
    }

    @Test
    public void themeDefiningLumoClassAndName_loadsLumo()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(OkClassExtension.class));
        Mockito.when(classFinder.loadClass(FakeLumo.class.getName()))
                .thenReturn((Class) FakeLumo.class);

        Mockito.when(classFinder.loadClass(MyLumoTheme.class.getName()))
                .thenReturn((Class) MyLumoTheme.class);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        Assert.assertEquals("Faulty default theme received", MyLumoTheme.class,
                dependencies.getThemeDefinition().getTheme());
    }

    @Test
    public void noDefaultThemeAvailable_throwsException()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(MyAppTheme.class));
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
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(MyAppTheme.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        Assert.assertEquals("Faulty default theme received", FakeLumo.class,
                dependencies.getThemeDefinition().getTheme());

    }

    @Test
    public void onlyThemeVariantDefined_getsLumoAsTheme_preserveVariant() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(ThemeVariantOnly.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false);

        Assert.assertEquals("Faulty default theme received", FakeLumo.class,
                dependencies.getThemeDefinition().getTheme());
        Assert.assertEquals("Faulty variant received", "dark",
                dependencies.getThemeDefinition().getVariant());

    }

    @Theme(FakeLumo.class)
    public static class MyApp extends Component {
    }

    @Theme(themeFolder = "my-theme")
    public static class MyAppTheme extends Component {
    }

    @Theme(themeFolder = "my-theme", value = MyTheme.class)
    public static class FaultyThemeAnnotation extends Component {
    }

    @Theme(themeFolder = "my-theme", value = MyLumoTheme.class)
    public static class OkClassExtension extends Component {
    }

    @Theme(variant = "dark")
    public static class ThemeVariantOnly extends Component {
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

    public static class MyTheme implements AbstractTheme {
        public MyTheme() {
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

    public static class MyLumoTheme extends FakeLumo {
        public MyLumoTheme() {
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
}
