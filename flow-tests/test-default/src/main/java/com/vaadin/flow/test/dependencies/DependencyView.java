/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.dependencies;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;

@Route("dependency")
public class DependencyView extends Div {

    public DependencyView() {
        add(new Text(
                "This test initially loads a stylesheet which makes all text red, a JavaScript for logging window messages, a JavaScript for handling body click events and an HTML which sends a window message"),
                new Hr(), new Hr());

        Div clickBody = new Div();
        clickBody.setText("Hello, click the body please");
        clickBody.setId("hello");
        add(clickBody);

        NativeButton jsOrder = new NativeButton("Test JS order", e -> {
            Page page = getUI().get().getPage();
            page.addJavaScript("/test-files/js/set-global-var.js");
            page.addJavaScript("/test-files/js/read-global-var.js",
                    LoadMode.LAZY);
        });
        jsOrder.setId("loadJs");

        NativeButton allBlue = new NativeButton(
                "Load 'everything blue' stylesheet", e -> {
                    getUI().get().getPage().addStyleSheet(
                            "/test-files/css/allblueimportant.css");
                });
        allBlue.setId("loadBlue");

        NativeButton loadUnavailableResources = new NativeButton(
                "Load unavailable resources", e -> {
                    Page page = getUI().get().getPage();
                    page.addStyleSheet("/not-found.css");
                    page.addJavaScript("/not-found.js");
                });
        loadUnavailableResources.setId("loadUnavailableResources");

        Div log = new Div();
        log.setId("log");

        add(jsOrder, allBlue, loadUnavailableResources, new Hr(), log);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Page page = attachEvent.getUI().getPage();
        page.addStyleSheet("/test-files/css/allred.css");
        page.addJavaScript("/frontend/test-files/js/body-click-listener.js");
    }

}
