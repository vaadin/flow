/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
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

    @JavaScript("context://test-files/js/body-click-listener.js")
    public static class JsResourceComponent extends Div {

        public JsResourceComponent() {
            setText("Hello, click the body please");
            setId("hello");
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
