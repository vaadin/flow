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

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

/**
 * A container class for all components used in tests.
 */
public class NodeTestComponents extends NodeUpdateTestUtil {

    public static final String BUTTON_COMPONENT_FQN = ButtonComponent.class
            .getName();
    public static final String ICON_COMPONENT_FQN = IconComponent.class
            .getName();

    @NpmPackage(value = "@vaadin/vaadin-button", version = "1.1.1")
    class ButtonComponent extends Component {
    }

    @JsModule("@polymer/iron-icon/iron-icon.js")
    class IconComponent extends Component {
    }

    @JsModule("@vaadin/vaadin-date-picker/src/vaadin-date-picker.js")
    @JsModule("@vaadin/vaadin-date-picker/src/vaadin-month-calendar.js")
    @JavaScript("ExampleConnector.js")
    public static class VaadinBowerComponent extends Component {
    }

    @NpmPackage(value = "@vaadin/vaadin-element-mixin", version = "1.1.2")
    @JsModule("@vaadin/vaadin-element-mixin/vaadin-element-mixin.js")
    public static class VaadinElementMixin extends Component {
    }

    @JsModule("./foo-dir/vaadin-npm-component.js")
    @JavaScript("frontend://foo-dir/javascript-lib.js")
    public static class VaadinNpmComponent extends Component {
    }

    @JsModule("vaadin-mixed-component/src/vaadin-mixed-component.js")
    public static class VaadinMixedComponent extends Component {
    }

    @JsModule("./local-template.js")
    @JsModule("3rdparty/component.js")
    public static class LocalTemplate extends Component {
    }

    @JsModule("./local-p3-template.js")
    @NpmPackage(value = "@foo/var-component", version = "1.1.0")
    public static class LocalP3Template extends Component {
    }

    @JsModule("frontend://frontend-p3-template.js")
    @JsModule("unresolved/component")
    public static class FrontendP3Template extends Component {
    }

    @JsModule("./foo.js")
    @CssImport("@vaadin/vaadin-mixed-component/bar.css")
    @CssImport("./foo.css")
    @CssImport(value = "./foo.css")
    @CssImport(value = "./foo.css", include = "bar")
    @CssImport(value = "./foo.css", id = "baz")
    @CssImport(value = "./foo.css", id = "baz", include = "bar")
    @CssImport(value = "./foo.css", themeFor = "foo-bar")
    @CssImport(value = "./foo.css", themeFor = "foo-bar", include = "bar")
    public static class FlatImport extends Component {
    }

    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-mixed-component.js")
    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-something-else.js")
    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-something-else")
    @JsModule("@vaadin/vaadin-mixed-component/src/vaadin-custom-themed-component.js")
    public static class TranslatedImports extends Component {

    }

    @JsModule("./common-js-file.js")
    @Theme(value = LumoTest.class, variant = LumoTest.DARK)
    @Route
    public static class MainLayout implements RouterLayout {
        @Override
        public Element getElement() {
            return null;
        }
    }

    @Route(value = "", layout = MainLayout.class)
    public static class MainView extends Component {
        ButtonComponent buttonComponent;
        IconComponent iconComponent;
        VaadinBowerComponent vaadinBowerComponent;
        VaadinElementMixin vaadinElementMixin;
        VaadinNpmComponent vaadinNpmComponent;
        VaadinMixedComponent vaadinMixedComponent;
        LocalTemplate localP2Template;
        LocalP3Template localP3Template;
        FrontendP3Template frontendP3Template;
        FlatImport flatImport;
        TranslatedImports translatedImports;
        JavaScriptOrder order;
    }

    /**
     * Lumo component theme class implementation.
     */
    @JsModule("@vaadin/vaadin-lumo-styles/color.js")
    @JsModule("@vaadin/vaadin-lumo-styles/typography.js")
    @JsModule("@vaadin/vaadin-lumo-styles/sizing.js")
    @JsModule("@vaadin/vaadin-lumo-styles/spacing.js")
    @JsModule("@vaadin/vaadin-lumo-styles/style.js")
    @JsModule("@vaadin/vaadin-lumo-styles/icons.js")
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

    @NpmPackage(value = "@vaadin/vaadin-shrinkwrap", version = "1.2.3")
    public static class VaadinShrinkWrap extends Component {
    }

    @JavaScript("javascript/a.js")
    @JavaScript("javascript/b.js")
    @JavaScript("javascript/c.js")
    @JsModule("jsmodule/g.js")
    public static class JavaScriptOrder extends Component {

    }
}
