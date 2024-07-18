/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DependencyView", layout = ViewTestLayout.class)
public class DependencyView extends AbstractDivView {

    @Tag("div")
    static class HtmlComponent extends Component implements HasText {

        public HtmlComponent() {
            setText("Text component");
        }
    }

    @Override
    protected void onShow() {
        add(new Text(
                "This test initially loads a stylesheet which makes all text red, a JavaScript for logging window messages, a JavaScript for handling body click events and an HTML which sends a window message"),
                new Hr(), new HtmlComponent(), new Hr());

        Div clickBody = new Div();
        clickBody.setText("Hello, click the body please");
        clickBody.setId("hello");
        add(clickBody);

        NativeButton jsOrder = new NativeButton("Test JS order", e -> {
            getPage().addJavaScript("/test-files/js/set-global-var.js");
            getPage().addJavaScript("/test-files/js/read-global-var.js",
                    LoadMode.LAZY);
        });
        jsOrder.setId("loadJs");

        NativeButton allBlue = new NativeButton(
                "Load 'everything blue' stylesheet", e -> {
                    getPage().addStyleSheet(
                            "/test-files/css/allblueimportant.css");

                });
        allBlue.setId("loadBlue");

        NativeButton loadUnavailableResources = new NativeButton(
                "Load unavailable resources", e -> {
                    getPage().addStyleSheet("/not-found.css");
                    getPage().addJavaScript("/not-found.js");
                });
        loadUnavailableResources.setId("loadUnavailableResources");

        Div log = new Div();
        log.setId("log");

        add(jsOrder, allBlue, loadUnavailableResources, new Hr(), log);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getPage().addStyleSheet("/test-files/css/allred.css");
        getPage().addJavaScript("/test-files/js/init-log-message.js");
        getPage().addJavaScript("/test-files/js/body-click-listener.js");
    }
}
