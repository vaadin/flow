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
package com.vaadin.hummingbird.contexttest.ui;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class DependencyUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        getElement().appendChild(ElementFactory.createDiv(
                "This test initially loads a stylesheet which makes all text red and a javascript which listens to body clicks"));
        getElement().appendChild(ElementFactory.createHr());
        getPage().addStyleSheet("context://test-files/css/allred.css");
        getPage().addJavaScript(getServletToContextPath(
                "test-files/js/body-click-listener.js"));
        getElement()
                .appendChild(ElementFactory
                        .createDiv("Hello, click the body please"))
                .setAttribute("id", "hello");

        Element jsOrder = ElementFactory.createButton("Load js")
                .setAttribute("id", "loadJs");
        jsOrder.addEventListener("click", e -> {
            getPage().addJavaScript(
                    "context://test-files/js/element-appender.js");
        });
        Element allBlue = ElementFactory
                .createButton("Load 'everything blue' stylesheet")
                .setAttribute("id", "loadBlue");
        allBlue.addEventListener("click", e -> {
            getPage().addStyleSheet(
                    "context://test-files/css/allblueimportant.css");

        });
        getElement().appendChild(jsOrder, allBlue, ElementFactory.createHr());
    }

    protected String getServletToContextPath(String url) {
        return url;
    }

    protected String makeRelativeToContext(String url) {
        return url;
    }
}
