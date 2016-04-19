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

import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Hr;
import com.vaadin.ui.Text;

public class DependencyView extends AbstractDivView {

    @Override
    protected void onShow() {
        add(new Text(
                "This test initially loads a stylesheet which makes all text red and a javascript which listens to body clicks"));
        add(new Hr());

        getPage().addStyleSheet("/test-files/css/allred.css");
        getPage().addJavaScript("/test-files/js/body-click-listener.js");

        Div clickBody = new Div();
        clickBody.setText("Hello, click the body please");
        clickBody.setId("hello");
        add(clickBody);

        Button jsOrder = new Button("Test JS order", e -> {
            getPage().addJavaScript("/test-files/js/set-global-var.js");
            getPage().addJavaScript("/test-files/js/read-global-var.js");
        });
        jsOrder.setId("loadJs");
        Button allBlue = new Button("Load 'everything blue' stylesheet", e -> {
            getPage().addStyleSheet("/test-files/css/allblueimportant.css");

        });
        allBlue.setId("loadBlue");
        add(jsOrder, allBlue, new Hr());
    }

}
