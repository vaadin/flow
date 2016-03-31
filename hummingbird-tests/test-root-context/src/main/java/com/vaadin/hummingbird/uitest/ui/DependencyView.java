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

import com.vaadin.hummingbird.uitest.component.Button;
import com.vaadin.hummingbird.uitest.component.Div;
import com.vaadin.hummingbird.uitest.component.Hr;
import com.vaadin.hummingbird.uitest.component.Text;

public class DependencyView extends AbstractDivView {

    @Override
    protected void onShow() {
        addComponent(new Text(
                "This test initially loads a stylesheet which makes all text red and a javascript which listens to body clicks"));
        addComponent(new Hr());

        getPage().addStyleSheet("/test-files/css/allred.css");
        getPage().addJavaScript("/test-files/js/body-click-listener.js");

        addComponent(new Div("Hello, click the body please").setId("hello"));

        Button jsOrder = new Button("Test JS order").setId("loadJs");
        jsOrder.getElement().addEventListener("click", e -> {
            getPage().addJavaScript("/test-files/js/set-global-var.js");
            getPage().addJavaScript("/test-files/js/read-global-var.js");
        });
        Button allBlue = new Button("Load 'everything blue' stylesheet")
                .setId("loadBlue");
        allBlue.getElement().addEventListener("click", e -> {
            getPage().addStyleSheet("/test-files/css/allblueimportant.css");

        });
        addComponent(jsOrder, allBlue, new Hr());
    }

}
