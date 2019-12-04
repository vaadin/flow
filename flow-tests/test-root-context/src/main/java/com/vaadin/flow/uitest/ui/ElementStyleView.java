/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ElementStyleView", layout = ViewTestLayout.class)
public class ElementStyleView extends AbstractDivView {

    static final String GREEN_BORDER = "4px solid rgb(0, 255, 0)";
    static final String RED_BORDER = "10px solid rgb(255, 0, 0)";

    @Override
    protected void onShow() {
        Element mainElement = getElement();
        mainElement.getStyle().set("--foo", RED_BORDER);

        Div div = new Div();
        div.setId("red-border");
        div.getElement().getStyle().set("border", "var(--foo)");
        div.setText("Div");

        Div div2 = new Div();
        div2.setId("green-border");
        div2.setText("Div 2");
        div2.getStyle().set("--foo", GREEN_BORDER);
        div2.getElement().getStyle().set("border", "var(--foo)");
        add(div, div2);

    }

}
