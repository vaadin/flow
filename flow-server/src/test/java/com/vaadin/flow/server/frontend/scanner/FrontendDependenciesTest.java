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

        Mockito.doAnswer(invocation -> {
            return FrontendDependenciesTest.class.getClassLoader()
                    .getResource(invocation.getArgumentAt(0, String.class));
        }).when(classFinder).getResource(Mockito.anyString());

    }

    @Test
    public void routedComponent_endpointsAreCollected()
            throws ClassNotFoundException {
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
    public void hasErrorParameterComponent_endpointIsCollected()
            throws ClassNotFoundException {
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
    public void componentInsideUiInitListener_endpointsAreCollected()
            throws ClassNotFoundException {
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
    public void componentInsideUiInitListenerInsideServiceInitListener_endpointsAreCollected()
            throws ClassNotFoundException {
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
    public void jsScriptOrderIsPreserved() throws ClassNotFoundException {
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
}
