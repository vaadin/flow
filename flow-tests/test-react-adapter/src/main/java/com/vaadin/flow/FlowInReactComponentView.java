/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
