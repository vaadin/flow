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
package com.vaadin.flow.test.components;

import java.io.Serializable;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.Route;

@JavaScript("./in-memory-connector.js")
@Route("in-memory-children")
public class InMemoryChildrenView extends Div {

    @JsDefinition
    public interface ConnectorJs extends Serializable {

        @JsExpression("window.inMemoryConnector.init(this, $0)")
        void init(Element inMemory);
    }

    public InMemoryChildrenView() {
        NativeLabel label = new NativeLabel();
        label.setId("in-memory");
        label.setText("In memory element");
        getElement().appendVirtualChild(label.getElement());
        getElement().executeJs(ConnectorJs.class).init(label.getElement());
        Div target = new Div();
        target.setId("target");
        add(target);
        NativeButton copy = new NativeButton(
                "Add copy of in-memory element to the target",
                event -> getElement().callJsFunction("useInMemoryElement",
                        target));
        copy.setId("copy");
        add(copy);
    }
}
