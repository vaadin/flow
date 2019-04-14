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
package com.vaadin.flow.server.frontend;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

/**
 * A container class for all components used in tests.
 */
public class FrontendTestComponents {

    @NpmPackage("@vaadin/theme-0")
    @JavaScript("frontend://theme-0.js")
    static class Theme0 implements AbstractTheme {
        public static final String DARK = "dark";

        @Override
        public String getBaseUrl() {
            return null;
        }

        @Override
        public String getThemeUrl() {
            return null;
        }
    }

    @HtmlImport("frontend://theme-1.html")
    static class Theme1 extends Theme0 {
    }
    @HtmlImport("frontend://theme-2.html")
    static class Theme2 extends Theme0 {
    }
    @JsModule("./theme-4.js")
    static class Theme4 extends Theme0 {
    }

    @NpmPackage("@vaadin/component-0")
    @JsModule("./component-0.js")
    @HtmlImport("frontend://component-0.html")
    @JavaScript("frontend://component-0.js")
    @Tag("component-0")
    @NpmPackage("@vaadin/component-0")
    static class Component0 extends Component {
    }

    @JsModule("./component-1.js")
    @HtmlImport("frontend://component-1.html")
    @JavaScript("frontend://component-1.js")
    @Tag("component-1")
    static class Component1 extends Component0 {
    }

    @JsModule("./component-2.js")
    @HtmlImport("frontend://component-2.html")
    @JavaScript("frontend://component-2.js")
    @Tag("component-2")
    static class Component2 extends Component {
    }

    @NpmPackage("@vaadin/component-3")
    @JsModule("./component-3.js")
    @HtmlImport("frontend://component-3.html")
    @JavaScript("frontend://component-3.js")
    @Tag("component-3")
    static class Component3 extends Component {
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

    @Route(value = "", layout = RouterLayout1.class)
    @JsModule("./view-3.js")
    @NoTheme
    public static class RootViewWithoutTheme extends View0 {
    }

    @Route(value = "", layout = RouterLayout1.class)
    public static class RootViewWithLayoutTheme extends FirstView {
    }

    @Route(value = "", layout = RouterLayout2.class)
    @Theme(value = Theme2.class, variant = "foo")
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

    @Route(value = "second", layout = RouterLayout2.class)
    @JsModule("./view-2.js")
    public static class SecondView extends Component {

        public SecondView() {
           createView();
        }

        private void createView() {
            new Component3();
        }
    }
}
