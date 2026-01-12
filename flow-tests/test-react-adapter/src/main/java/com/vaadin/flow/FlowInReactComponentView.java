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
package com.vaadin.flow;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.FlowInReactComponentView")
public class FlowInReactComponentView extends Div {

    public static final String ADD_MAIN = "add";
    public static final String REMOVE_MAIN = "remove";
    public static final String ADD_SECONDARY = "add-secondary";
    public static final String REMOVE_SECONDARY = "remove-secondary";

    public FlowInReactComponentView() {

        ReactLayout gridLayout = new ReactLayout();
        add(gridLayout);

        NativeButton addDiv = new NativeButton("Add div",
                event -> gridLayout.add(new Div("Clicked button")));
        addDiv.setId(ADD_MAIN);
        NativeButton removeDiv = new NativeButton("Remove div",
                event -> gridLayout.getChildren()
                        .filter(component -> component instanceof Div)
                        .findFirst().ifPresent(Component::removeFromParent));
        removeDiv.setId(REMOVE_MAIN);

        gridLayout.add(new H3("Flow Admin View"), addDiv, removeDiv);

        NativeButton addSecondary = new NativeButton("Add div",
                event -> gridLayout.addSecondary(new Div("Secondary div")));
        addSecondary.setId(ADD_SECONDARY);
        NativeButton removeSecondary = new NativeButton("Remove div",
                event -> gridLayout.getSecondaryChildren()
                        .filter(component -> component instanceof Div)
                        .findFirst().ifPresent(Component::removeFromParent));
        removeSecondary.setId(REMOVE_SECONDARY);
        gridLayout.addSecondary(new H4("Second container"), addSecondary,
                removeSecondary);
    }
}
