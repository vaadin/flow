/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;

public class AbstractEventDataView extends AbstractDivView {

    public static final String EMPTY_VALUE = "EMPTY";
    public static final String TARGET_ID = "target";
    public static final String VIEW_CONTAINER = "View-container";
    public static final String HEADER = "Header";

    public AbstractEventDataView() {
        add(new Text(VIEW_CONTAINER), new H3(HEADER));
        setId(VIEW_CONTAINER);
    }

    protected void createComponents() {
        for (int i = 0; i < 10; i++) {
            final Div container = createContainer("Child-" + i);
            for (int j = 0; j < 10; j++) {
                final Div child = createContainer("Grandchild-" + i + j);
                child.getStyle().set("display", "inline-block");
                container.add(child);
            }
            add(container);
        }
    }

    private Div createContainer(String identifier) {
        final Div div = new Div(identifier);
        div.setId(identifier);
        div.getStyle().set("border", "1px solid orange").set("padding", "5px");
        return div;
    }
}
