/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.collections.TwoWayListBindingView", layout = ViewTestLayout.class)
@Tag("two-way-list-binding")
@JsModule("./TwoWayListBinding.js")
public class TwoWayListBindingView
        extends PolymerTemplate<TwoWayListBindingView.TwoWayBindingModel>
        implements HasComponents {

    public interface TwoWayBindingModel extends TemplateModel {

        void setMessages(List<Message> messages);

        @AllowClientUpdates
        List<Message> getMessages();

        void setEnable(boolean enable);
    }

    public TwoWayListBindingView() {
        getModel().setMessages(
                Arrays.asList(new Message("foo"), new Message("bar")));

        add(AbstractDivView.createButton("Show listing", "enable",
                event -> getModel().setEnable(true)));
    }

    @EventHandler
    private void valueUpdated() {
        Div div = new Div();
        div.setClassName("messages");
        div.setText(getModel().getMessages().toString());
        add(div);
    }
}
