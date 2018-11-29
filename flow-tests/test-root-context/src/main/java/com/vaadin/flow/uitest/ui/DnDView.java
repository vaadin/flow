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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DnDView", layout = ViewTestLayout.class)
@JavaScript("dnd.js")
public class DnDView extends Div {

    public DnDView() {
        Div source = new Div();
        source.setText("Source");
        source.setId("source");
        source.getElement().setAttribute("draggable", "true");
        source.getElement().executeJavaScript("this.ondragstart=drag;");

        Div target = new Div();
        target.getStyle().set("width", "100px");
        target.getStyle().set("height", "50px");
        target.getStyle().set("border", "1px solid");
        target.setId("target");
        target.getElement().executeJavaScript("this.ondrop=drop;");
        target.getElement().executeJavaScript("this.ondragover=allowDrop;");

        add(source, target);
    }

}
