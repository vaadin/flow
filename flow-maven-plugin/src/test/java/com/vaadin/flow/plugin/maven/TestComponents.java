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
package com.vaadin.flow.plugin.maven;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;


/**
 * A container class for all components used in tests.
 */
public class TestComponents {
    @NpmPackage("@vaadin/vaadin-button")
    class ButtonComponent extends Component {
    }

    @HtmlImport("frontend://bower_components/iron-icon/iron-icon.html")
    class IconComponent extends Component {
    }

    @HtmlImport("frontend://bower_components/vaadin-element-mixin/src/vaadin-element-mixin.html")
    @HtmlImport("frontend://bower_components/vaadin-element-mixin/src/something-else.html")
    @JavaScript("frontend://ExampleConnector.js")
    public static class VaadinBowerComponent extends Component {
    }

    @NpmPackage("@vaadin/vaadin-element-mixin")
    @JsModule("foo-dir/vaadin-npm-component.js")
    public static class VaadinNpmComponent extends Component {
    }

    @HtmlImport("frontend://bower_components/vaadin-element-mixin/foo-component.html")
    @NpmPackage("@vaadin/vaadin-element-mixin")
    @JsModule("vaadin-mixed-component/src/vaadin-mixed-component.js")
    public static class VaadinMixedComponent extends Component {
    }

    @HtmlImport("frontend://local-p2-template.html")
    public static class LocalP2Template extends Component {
    }

    @JsModule("./local-p3-template.js")
    @Theme(value = Lumo.class, variant = Lumo.DARK)
    public static class LocalP3Template extends Component {
    }

    @HtmlImport("foo.html")
    public static class FlatImport extends Component {
    }

    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-mixed-component.js")
    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-something-else.js")
    public static class TranslatedImports extends Component {

    }

}
