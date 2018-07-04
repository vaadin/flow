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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@JavaScript("frontend://in-memory-connector.js")
@Route(value = "com.vaadin.flow.uitest.ui.InMemoryChildrenView", layout = ViewTestLayout.class)
public class InMemoryChildrenView extends AbstractDivView {

    @Override
    protected void onShow() {
        Label label = new Label();
        label.setId("in-memory");
        label.setText("In memory element");
        getElement().appendVirtualChild(label.getElement());
        getElement().getNode()
                .runWhenAttached(ui -> ui.getPage().executeJavaScript(
                        "window.inMemoryConnector.init($0, $1)", getElement(),
                        label));
        Div target = new Div();
        target.setId("target");
        add(target);
        NativeButton button = new NativeButton(
                "Add copy of in-memory element to the target",
                event -> getElement().callFunction("useInMemoryElement",
                        target));
        button.setId("copy");
        add(button);
    }
}
