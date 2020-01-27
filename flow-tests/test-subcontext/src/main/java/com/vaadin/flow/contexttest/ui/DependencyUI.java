/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.contexttest.ui;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinRequest;

@StyleSheet("context://test-files/css/allred.css")
public class DependencyUI extends UI {

    @StyleSheet("context://test-files/css/allblueimportant.css")
    public static class AllBlueImportantComponent extends Div {

        public AllBlueImportantComponent() {
            setText("allblueimportant.css component");
        }

    }

    public static class JsResourceComponent extends Div {

        public JsResourceComponent() {
            setText("Hello, click the body please");
            setId("hello");
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            attachEvent.getUI().getPage().addJavaScript(
                    "context://frontend/test-files/js/body-click-listener.js");
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        getElement().appendChild(ElementFactory.createDiv(
                "This test initially loads a stylesheet which makes all text red and a JavaScript which listens to body clicks"));
        getElement().appendChild(ElementFactory.createHr());
        add(new JsResourceComponent());

        Element jsOrder = ElementFactory.createButton("Load js")
                .setAttribute("id", "loadJs");
        StreamRegistration jsStreamRegistration = getSession()
                .getResourceRegistry().registerResource(getJsResource());
        jsOrder.addEventListener("click", e -> {
            getPage().addJavaScript("base://"
                    + jsStreamRegistration.getResourceUri().toString());
        });
        Element allBlue = ElementFactory
                .createButton("Load 'everything blue' stylesheet")
                .setAttribute("id", "loadBlue");
        allBlue.addEventListener("click", e -> {
            add(new AllBlueImportantComponent());

        });
        getElement().appendChild(jsOrder, allBlue, ElementFactory.createHr());
    }

    private StreamResource getJsResource() {
        StreamResource jsRes = new StreamResource("element-appender.js", () -> {
            String js = "var div = document.createElement('div');"
                    + "div.id = 'appended-element';"
                    + "div.textContent = 'Added by script';"
                    + "document.body.appendChild(div, null);";

            // Wait to ensure that client side will stop until the JavaScript is
            // loaded
            try {
                Thread.sleep(500);
            } catch (Exception e1) {
            }
            return new ByteArrayInputStream(
                    js.getBytes(StandardCharsets.UTF_8));
        });
        return jsRes;
    }

}
