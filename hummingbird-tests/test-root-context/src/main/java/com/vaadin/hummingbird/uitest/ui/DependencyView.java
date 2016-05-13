/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Hr;
import com.vaadin.server.InputStreamFactory;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResourceRegistration;
import com.vaadin.ui.Text;

public class DependencyView extends AbstractDivView {

    private StreamResourceRegistration jsFile;
    private StreamResourceRegistration htmlImport2;
    private StreamResourceRegistration htmlImport3;

    @Override
    protected void onShow() {
        add(new Text(
                "This test initially loads a stylesheet which makes all text red, a JavaScript for logging window messages, a JavaScript for handling body click events and an HTML which sends a window message"));
        add(new Hr());

        Div clickBody = new Div();
        clickBody.setText("Hello, click the body please");
        clickBody.setId("hello");
        add(clickBody);

        Button jsOrder = new Button("Test JS order", e -> {
            getPage().addJavaScript("/test-files/js/set-global-var.js");
            getPage().addJavaScript("/test-files/js/read-global-var.js");
        });
        jsOrder.setId("loadJs");

        /* HTML imports */
        Button htmlOrder = new Button("Test HTML order", e -> {
            getPage().addHtmlImport(htmlImport2.getResourceUri().toString());
            // This failure can only be seen in the browser console
            getPage().addHtmlImport("/doesnotexist.html");
            getPage().addJavaScript(jsFile.getResourceUri().toString());
            getPage().addHtmlImport(htmlImport3.getResourceUri().toString());
        });
        htmlOrder.setId("loadHtml");

        Button allBlue = new Button("Load 'everything blue' stylesheet", e -> {
            getPage().addStyleSheet("/test-files/css/allblueimportant.css");

        });
        allBlue.setId("loadBlue");
        add(jsOrder, htmlOrder, allBlue, new Hr());
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        htmlImport2 = getUI().get().getSession().getResourceRegistry()
                .registerResource(new StreamResource("htmlimport2.html",
                        new HTMLImportStreamFactory("HTML import 2", 1000)));
        htmlImport3 = getUI().get().getSession().getResourceRegistry()
                .registerResource(new StreamResource("htmlimport3.html",
                        new HTMLImportStreamFactory("HTML import 3", 0)));
        jsFile = getUI().get().getSession().getResourceRegistry()
                .registerResource(new StreamResource("jsFile.js",
                        new JSStreamFactory("JS File", 1500)));

        getPage().addStyleSheet("/test-files/css/allred.css");
        getPage().addJavaScript("/test-files/js/body-click-listener.js");
        getPage().addJavaScript("/test-files/js/messagehandler.js");
        getPage().addHtmlImport("/test-files/html/htmlimport1.html");
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
            return stringToStream(
                    "window.postMessage('" + name + " loaded','*');");
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
