/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;
import com.vaadin.ui.html.NativeButton;
import com.vaadin.ui.html.Hr;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HasText;

@Route(value = "com.vaadin.flow.uitest.ui.OrderedDependencyView", layout = ViewTestLayout.class)
public class OrderedDependencyView extends AbstractDivView {

    @Tag("div")
    @HtmlImport("/test-files/html/htmlimport1.html")
    @StyleSheet("/test-files/css/allred.css")
    static class HtmlComponent extends Component implements HasText {

        public HtmlComponent() {
            setText("Super component");
        }
    }

    @Tag("div")
    @HtmlImport("/test-files/html/htmlimport2.html")
    @StyleSheet("/test-files/css/allblueimportant.css")
    static class Html2Component extends HtmlComponent {

        public Html2Component() {
            setText("Extending child component");
        }
    }

    @Tag("div")
    @JavaScript("/test-files/js/script1.js")
    static class ScriptComponent extends Component implements HasText {

        public ScriptComponent() {
            setText("Super script component");
        }
    }

    @Tag("div")
    @JavaScript("/test-files/js/script2.js")
    static class Script2Component extends ScriptComponent {

        public Script2Component() {
            setText("Extending script child component");
        }
    }

    @Override
    protected void onShow() {
        Html2Component html2Component = new Html2Component();
        html2Component.setId("component");
        add(html2Component, new Hr());

        NativeButton scripts = new NativeButton("Add scripts",
                event -> add(new Script2Component()));
        scripts.setId("addJs");
        add(scripts);
    }

}
