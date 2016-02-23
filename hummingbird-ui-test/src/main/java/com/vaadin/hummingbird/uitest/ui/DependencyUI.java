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

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Dependency;
import com.vaadin.ui.Dependency.Type;
import com.vaadin.ui.UI;

public class DependencyUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        getElement().appendChild(new Element("div").setTextContent(
                "This test initially loads a stylesheet which makes all text red and a javascript which listens to body clicks"));
        getElement().appendChild(new Element("hr"));
        getPage().addDependency(new Dependency(Type.STYLESHEET,
                "/VAADIN/test-files/css/allred.css"));
        getPage().addDependency(new Dependency(Type.JAVASCRIPT,
                "/VAADIN/test-files/js/body-click-listener.js"));
        getElement()
                .appendChild(new Element("div")
                        .setTextContent("Hello, click the body please"))
                .setAttribute("id", "hello");

        Element jsOrder = new Element("button").setTextContent("Test JS order")
                .setAttribute("id", "loadJs");
        jsOrder.addEventListener("click", e -> {
            getPage().addDependency(new Dependency(Type.JAVASCRIPT,
                    "/VAADIN/test-files/js/set-global-var.js"));
            getPage().addDependency(new Dependency(Type.JAVASCRIPT,
                    "/VAADIN/test-files/js/read-global-var.js"));
        });
        Element allBlue = new Element("button")
                .setTextContent("Load 'everything blue' stylesheet")
                .setAttribute("id", "loadBlue");
        allBlue.addEventListener("click", e -> {
            getPage().addDependency(new Dependency(Type.STYLESHEET,
                    "/VAADIN/test-files/css/allblueimportant.css"));

        });
        getElement().appendChild(jsOrder, allBlue, new Element("hr"));
    }

}
