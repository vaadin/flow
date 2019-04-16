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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import org.slf4j.LoggerFactory;

/**
 * A container class for all components used in tests.
 */
public class NodeTestComponents {

    @NpmPackage(value = "@vaadin/vaadin-button", version = "1.1.1")
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

    @NpmPackage(value = "@vaadin/vaadin-element-mixin", version = "1.1.2")
    @JsModule("foo-dir/vaadin-npm-component.js")
    public static class VaadinNpmComponent extends Component {
    }

    @HtmlImport("frontend://bower_components/vaadin-element-mixin/foo-component.html")
    @NpmPackage(value = "@vaadin/vaadin-element-mixin", version = "1.1.2")
    @JsModule("vaadin-mixed-component/src/vaadin-mixed-component.js")
    public static class VaadinMixedComponent extends Component {
    }

    @HtmlImport("frontend://local-p2-template.html")
    public static class LocalP2Template extends Component {
    }

    @JsModule("./local-p3-template.js")
    @NpmPackage(value = "@foo/var-component", version = "1.1.0")
    public static class LocalP3Template extends Component {
    }

    @HtmlImport("foo.html")
    public static class FlatImport extends Component {
    }

    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-mixed-component.js")
    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-something-else.js")
    public static class TranslatedImports extends Component {

    }

    @Theme(value = LumoTest.class, variant = LumoTest.DARK)
    @Route
    public static class MainView extends Component {
        ButtonComponent buttonComponent;
        IconComponent iconComponent;
        VaadinBowerComponent vaadinBowerComponent;
        VaadinNpmComponent vaadinNpmComponent;
        VaadinMixedComponent vaadinMixedComponent;
        LocalP2Template localP2Template;
        LocalP3Template localP3Template;
        FlatImport flatImport;
        TranslatedImports translatedImports;
    }

    /**
     * Lumo component theme class implementation.
     */
    @HtmlImport("frontend://bower_components/vaadin-lumo-styles/color.html")
    @HtmlImport("frontend://bower_components/vaadin-lumo-styles/typography.html")
    @HtmlImport("frontend://bower_components/vaadin-lumo-styles/sizing.html")
    @HtmlImport("frontend://bower_components/vaadin-lumo-styles/spacing.html")
    @HtmlImport("frontend://bower_components/vaadin-lumo-styles/style.html")
    @HtmlImport("frontend://bower_components/vaadin-lumo-styles/icons.html")
    public static class LumoTest implements AbstractTheme {

        public static final String LIGHT = "light";
        public static final String DARK = "dark";

        public LumoTest() {
        }

        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "theme/lumo/";
        }

        @Override
        public List<String> getHeaderInlineContents() {
            return Collections.singletonList("<custom-style>\n"
                    + "    <style include=\"lumo-color lumo-typography\"></style>\n"
                    + "</custom-style>");
        }

        @Override
        @Deprecated
        public Map<String, String> getBodyAttributes(String variant) {
            return getHtmlAttributes(variant);
        }

        @Override
        public Map<String, String> getHtmlAttributes(String variant) {
            if (variant.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, String> attributes = new HashMap<>(1);
            switch (variant) {
                case LIGHT:
                    attributes.put("theme", LIGHT);
                    break;
                case DARK:
                    attributes.put("theme", DARK);
                    break;
                default:
                    LoggerFactory.getLogger(LumoTest.class.getName()).warn(
                            "Lumo theme variant not recognized: '{}'. Using no variant.",
                            variant);
            }
            return attributes;
        }
    }


    @NpmPackage(value = "@webcomponents/webcomponentsjs", version = "2.2.9")
    public static class ExtraImport {
    }
}

