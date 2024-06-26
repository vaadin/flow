/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

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
