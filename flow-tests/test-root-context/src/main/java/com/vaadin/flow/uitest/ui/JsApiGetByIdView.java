/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.JsApiGetByIdView", layout = ViewTestLayout.class)
@JavaScript("frontend://js-get-by-id.js")
public class JsApiGetByIdView extends AbstractDivView {

    public JsApiGetByIdView() {
        Label label = new Label("Original label");
        label.setId("source");
        add(label);

        Label target = new Label();
        target.setId("target");
        add(target);

        getElement().getNode()
                .runWhenAttached(ui -> ui.getPage().executeJs(
                        "window.jsApiConnector.jsFunction($0, this.$appId, $1)",
                        target, label.getElement().getNode().getId()));

        add(createButton("Update target", "update",
                event -> target.getElement().callJsFunction("operation")));
    }
}
