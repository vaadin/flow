/*
 * Copyright 2000-2019 Vaadin Ltd.
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
 *
 */
package com.vaadin.flow.server.frontend.scanner;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

/**
 * A container class for all components used in tests.
 */
public class ScannerTestComponents {

    @NpmPackage(value = "@vaadin/theme-0", version = "1.1.1")
    @JavaScript("frontend://theme-0.js")
    static class Theme0 implements AbstractTheme {
        public static final String DARK = "dark";
        public static final String FOO = "foo";

        @Override
        public String getBaseUrl() {
            return null;
        }

        @Override
        public String getThemeUrl() {
            return null;
        }
    }

    static class Theme1 extends Theme0 {
    }
    static class Theme2 extends Theme0 {
    }
    @JsModule("./theme-4.js")
    static class Theme4 extends Theme0 {
    }

    @JsModule("./theme-default.js")
    static class ThemeDefault extends Theme0 {
    }

    @NpmPackage(value = "@vaadin/component-0", version = "=2.1.0")
    @JsModule("./component-0.js")
    @JavaScript("frontend://component-0.js")
    @Tag("component-0")
    @NpmPackage(value = "@vaadin/component-0", version = "^1.1.0")
    @NpmPackage(value="@vaadin/vaadin-foo", version="1.23.114-alpha1")
    static class Component0 extends Component {
    }

    @JsModule("./component-1.js")
    @JavaScript("frontend://component-1.js")
    @Tag("component-1")
    @NpmPackage(value = "@vaadin/component-1", version = "1.1.1")
    static class Component1 extends Component0 {
    }

    @JsModule("./component-2.js")
    @JavaScript("frontend://component-2.js")
    @Tag("component-2")
    @NpmPackage(value = "@vaadin/component-2", version = "222.222.222")
    static class Component2 extends Component {
    }

    @JsModule("./component-3.js")
    @JavaScript("frontend://component-3.js")
    @Tag("component-3")
    @NpmPackage(value = "@vaadin/component-3", version = "~2.1.0")
    static class Component3 extends Component {
    }

    static class ComponentExtending extends GeneratedComponent {

    }

    @Tag("component-extends")
    @NpmPackage(value = "@vaadin/component-extended", version = "2.1.0")
    static abstract class GeneratedComponent extends Component {

    }


    @Theme(value = Theme1.class, variant = Theme0.DARK)
    @JsModule("./router-layout-1.js")
    public class RouterLayout1 implements RouterLayout {
        @Override
        public Element getElement() {
            return null;
        }
    }

    @Theme(value = Theme1.class, variant = Theme0.DARK)
    @JsModule("./router-layout-2.js")
    public class RouterLayout2 extends RouterLayout1 {
    }

    @JavaScript("frontend://view-0.js")
    public static abstract class View0 extends Component {
    }

    @Route(value = "")
    @Theme(Theme4.class)
    public static class RootViewWithTheme extends Component {
    }

    @Route(value = "")
    public static class RootViewWithoutThemeAnnotation extends Component {

    }

    @Route(value = "", layout = RouterLayout1.class)
    @JsModule("./view-3.js")
    @NoTheme
    public static class RootViewWithoutTheme extends View0 {
    }

    @Route(value = "", layout = RouterLayout1.class)
    public static class RootViewWithLayoutTheme extends FirstView {
    }

    @Route(value = "2", layout = RouterLayout1.class)
    public static class RootView2WithLayoutTheme {
    }

    @Route(value = "", layout = RouterLayout2.class)
    @Theme(value = Theme2.class, variant = Theme2.FOO)
    @JsModule("./view-2.js")
    public static class RootViewWithMultipleTheme extends Component {

        public RootViewWithMultipleTheme() {
           createView();
        }

        private void createView() {
            new Component3();
        }
    }

    @Route(value = "", layout = RouterLayout1.class)
    @JsModule("./view-1.js")
    @NpmPackage(value = "@foo/first-view", version = "0.0.1")
    public static class FirstView extends View0 {
        Component1 component1;
        RootViewWithMultipleTheme second;

        public FirstView() {
           createView();
        }

        private void createView() {
            new Component2();
        }
    }

    public static class MyComponent extends Component {
    }

    @Route(value = "second", layout = RouterLayout2.class)
    @JsModule("./view-2.js")
    public static class SecondView extends Component {
        public SecondView() {
            new Component3();
        }
    }

    @Route()
    public static class ThirdView {
        public void foo() {
            new ComponentFactory().createMyComponent();
            this.add(StaticComponentsFactory.createAnotherComponent(null));
        }
        private void add(Object o) {
        }
    }

    @JsModule("./my-component.js")
    public static class ComponentFactory {
        public MyComponent createMyComponent()  {
            return new MyComponent();
        }
    }

    @JsModule("./my-static-factory.js")
    public static class StaticComponentsFactory {
        public static AnotherComponent createAnotherComponent(String label) {
            return AnotherComponent.createMyComponent(label);
        }
    }

    @JsModule("./my-another-component.js")
    public static class AnotherComponent {
        public static AnotherComponent createMyComponent(String label)  {
            return new AnotherComponent();
        }
    }

    public static class NoThemeExporter extends WebComponentExporter<RootViewWithTheme> {
        public NoThemeExporter() {
            super("root-view");
        }

        @Override
        public void configureInstance(WebComponent<RootViewWithTheme> webComponent, RootViewWithTheme component) {

        }
    }

    @Theme(Theme2.class)
    public static class ThemeExporter extends WebComponentExporter<RootViewWithTheme> {
        public ThemeExporter() {
            super("root-view");
        }

        @Override
        public void configureInstance(WebComponent<RootViewWithTheme> webComponent, RootViewWithTheme component) {

        }
    }

    public static class UnAnnotatedClass {
    }

    @Route
    public static class RoutedClassWithoutAnnotations {
    }

    @Route("route-1")
    @NoTheme
    @JsModule("./foo")
    public static class RoutedClassWithAnnotations extends RoutedClassWithoutAnnotations {
    }

    public static class BridgeClass extends RoutedClassWithAnnotations {
    }

    @Route("route-2")
    public static class RoutedClass extends BridgeClass {
    }

    @Route("css-route-1")
    @CssImport("./foo.css")
    @CssImport(value = "./foo.css", include = "bar")
    @CssImport(value = "./foo.css", id = "bar")
    @CssImport(value = "./foo.css", themeFor = "bar")
    public static class CssClass1 {
    }

    @Route("css-route-2")
    @CssImport("./foo.css")
    @CssImport(value = "./foo.css", include = "bar")
    @CssImport(value = "./foo.css", id = "bar")
    @CssImport(value = "./foo.css", themeFor = "bar")
    public static class CssClass2 extends CssClass1 {
    }

    @JsModule("dynamic-component.js")
    public static class DynamicComponentClass extends Component {
    }
    @JsModule("dynamic-layout.js")
    public static class DynamicLayoutClass implements RouterLayout {
        @Override
        public Element getElement() {
            return null;
        }
    }

    @Route("dynamic-route")
    @JsModule("dynamic-route.js")
    public static class RouteWithNestedDynamicRouteClass {
        public RouteWithNestedDynamicRouteClass() {
            registerRoute();
        }

        @SuppressWarnings("unchecked")
        private void registerRoute() {
            RouteConfiguration.forSessionScope().setRoute("foo",
                    DynamicComponentClass.class,
                    DynamicLayoutClass.class);
        }
    }

    @Route()
    public static class RouteWithViewBean {
        public RouteWithViewBean() {
            UI.getCurrent().add(BeanFactory.getBean(DynamicComponentClass.class));
        }
    }

    public static class BeanFactory {
        public static <T> T getBean(Class<T> type) {
            return null;
        }
    }


    @Route()
    public static class RouteWithService {
        public RouteWithService() {
            UserRouteService.registerUserRoute(RouteConfiguration.forSessionScope(), "donald");
        }
    }

    public static class UserRouteService {
        @SuppressWarnings("unchecked")
        public static void registerUserRoute(RouteConfiguration config, String userId) {
            config.setRoute(userId, DynamicComponentClass.class, DynamicLayoutClass.class);
        }
    }
}
