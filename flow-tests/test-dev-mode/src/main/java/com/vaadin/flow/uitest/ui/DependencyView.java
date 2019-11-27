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
 */
package com.vaadin.flow.uitest.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DependencyView", layout = ViewTestLayout.class)
public class DependencyView extends AbstractDivView {

    private StreamRegistration htmlImport2;
    private StreamRegistration htmlImport3;

    @Tag("div")
    @HtmlImport("/test-files/html/orderedHtmlImport.html")
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

        /* HTML imports */
        NativeButton htmlOrder = new NativeButton("Test HTML order", e -> {
            getPage().addHtmlImport(
                    "base://" + htmlImport2.getResourceUri().toString());

            // This failure can only be seen in the browser console
            getPage().addHtmlImport("/doesnotexist.html");

            // Can't test JS/HTML order because of #765
            getPage().addHtmlImport(
                    "base://" + htmlImport3.getResourceUri().toString());
        });
        htmlOrder.setId("loadHtml");

        /* HTML & JS order */
        NativeButton mixedOrder = new NativeButton("Test HTML & JS order",
                e -> {
                    getPage().addHtmlImport(
                            "/test-files/html/combinedMixed.html");
                });
        mixedOrder.setId("loadMixed");

        NativeButton allBlue = new NativeButton(
                "Load 'everything blue' stylesheet", e -> {
                    getPage().addStyleSheet(
                            "/test-files/css/allblueimportant.css");

                });
        allBlue.setId("loadBlue");

        NativeButton loadUnavailableResources = new NativeButton(
                "Load unavailable resources", e -> {
                    getPage().addStyleSheet("/not-found.css");
                    getPage().addHtmlImport("/not-found.html");
                    getPage().addJavaScript("/not-found.js");
                });
        loadUnavailableResources.setId("loadUnavailableResources");

        Div log = new Div();
        log.setId("log");

        add(jsOrder, htmlOrder, mixedOrder, allBlue, loadUnavailableResources,
                new Hr(), log);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        htmlImport2 = registerResource(ui, "htmlimport2.html",
                new HTMLImportStreamFactory("HTML import 2", 1000));
        htmlImport3 = registerResource(ui, "htmlimport3.html",
                new HTMLImportStreamFactory("HTML import 3", 0));

        getPage().addStyleSheet("/test-files/css/allred.css");
        getPage().addJavaScript("/test-files/js/body-click-listener.js");
        getPage().addHtmlImport("/test-files/html/htmlimport1.html");
    }

    public static StreamRegistration registerResource(UI ui, String name,
            InputStreamFactory streamFactory) {
        return ui.getSession().getResourceRegistry()
                .registerResource(new StreamResource(name, streamFactory));
    }

    public static class JSStreamFactory implements InputStreamFactory {
        private String name;
        private int delay;

        public JSStreamFactory(String name, int delay) {
            this.name = name;
            this.delay = delay;
        }

        @Override
        public InputStream createInputStream() {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // Ignore
            }
            return stringToStream("window.logMessage('" + name + " loaded');");
        }

        protected InputStream stringToStream(String jsString) {
            byte[] bytes = jsString.getBytes(StandardCharsets.UTF_8);
            return new ByteArrayInputStream(bytes);
        }
    }

    public static class HTMLImportStreamFactory extends JSStreamFactory {

        public HTMLImportStreamFactory(String name, int delay) {
            super(name, delay);
        }

        @Override
        protected InputStream stringToStream(String jsString) {
            return super.stringToStream(
                    "<script type='text/javascript'>" + jsString + "</script>");
        }
    }

}
