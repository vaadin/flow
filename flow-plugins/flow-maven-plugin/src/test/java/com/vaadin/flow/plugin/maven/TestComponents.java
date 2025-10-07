/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.plugin.maven;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.CustomLumo;
import com.vaadin.flow.theme.Theme;

/**
 * A container class for all components used in tests.
 */
public class TestComponents {
    @NpmPackage(value = "@vaadin/button", version = "24.0.3")
    class ButtonComponent extends Component {
    }

    @JsModule("@polymer/iron-icon/iron-icon.js")
    class IconComponent extends Component {
    }

    @JsModule("@vaadin/vaadin-date-picker/src/vaadin-date-picker.js")
    @JsModule("@vaadin/vaadin-date-picker/src/vaadin-month-calendar.js")
    @JavaScript("./ExampleConnector.js")
    @JavaScript("https://foo.com/bar.js")
    @JavaScript("//foo.com/bar.js")
    public static class VaadinComponent extends Component {
    }

    @NpmPackage(value = "@vaadin/vaadin-element-mixin", version = "2.4.2")
    @JsModule("@vaadin/vaadin-element-mixin/vaadin-element-mixin.js")
    public static class VaadinElementMixin extends Component {
    }

    @JsModule("foo-dir/vaadin-npm-component.js")
    public static class VaadinNpmComponent extends Component {
    }

    @JsModule("vaadin-mixed-component/src/vaadin-mixed-component.js")
    public static class VaadinMixedComponent extends Component {
    }

    @JsModule("./local-template.js")
    public static class LocalTemplate extends Component {
    }

    @JsModule("./local-p3-template.js")
    public static class LocalP3Template extends Component {
    }

    @JsModule("./foo.js")
    public static class FlatImport extends Component {
    }

    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-mixed-component.js")
    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-something-else.js")
    public static class TranslatedImports extends Component {

    }

    @Route
    @Theme(themeClass = CustomLumo.class, variant = CustomLumo.DARK)
    public static class MainView extends Component {
        ButtonComponent buttonComponent;
        IconComponent iconComponent;
        VaadinComponent vaadinBowerComponent;
        VaadinElementMixin vaadinElementMixin;
        VaadinNpmComponent vaadinNpmComponent;
        VaadinMixedComponent vaadinMixedComponent;
        LocalTemplate localP2Template;
        LocalP3Template localP3Template;
        FlatImport flatImport;
        TranslatedImports translatedImports;
    }

}
