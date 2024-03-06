/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import java.util.UUID;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.AfterServerChangesView", layout = ViewTestLayout.class)
public class AfterServerChangesView extends AbstractDivView {

    @Tag("after-server-changes")
    @JsModule("./AfterServerChanges.js")
    public static class AfterServerChanges extends PolymerTemplate<Message> {

        @Override
        protected Message getModel() {
            return super.getModel();
        }
    }

    public AfterServerChangesView() {
        add(new AfterServerChanges());
        AfterServerChanges component = new AfterServerChanges();
        add(component);

        add(new OneWayPolymerBindingView());

        add(createButton("Remove the second component", "remove",
                event -> remove(component)));

        add(createButton("Update components", "update",
                event -> updateComponents()));
    }

    private void updateComponents() {
        getChildren()
                .filter(component -> component.getClass()
                        .equals(AfterServerChanges.class))
                .map(AfterServerChanges.class::cast)
                .forEach(component -> component.getModel()
                        .setText(UUID.randomUUID().toString()));
    }

}
