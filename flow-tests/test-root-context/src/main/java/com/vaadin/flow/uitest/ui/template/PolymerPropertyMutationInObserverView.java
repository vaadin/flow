/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.PolymerPropertyMutationInObserverView")
public class PolymerPropertyMutationInObserverView extends AbstractDivView {

    @Tag("property-mutation-in-observer")
    @JsModule("./PolymerPropertyMutationInObserver.js")
    public static class PolymerPropertyMutationInObserver
            extends PolymerTemplate<Message> {

        public void setText(String text) {
            getModel().setText(text);
        }

        private Div getValueDiv(String eventOldValue, String eventValue) {
            Div div = new Div();
            div.setText(String.format(
                    "Event old value: %s, event value: %s, current model value: %s",
                    eventOldValue, eventValue, getModel().getText()));
            div.addClassName("model-value");
            return div;
        }
    }

    public PolymerPropertyMutationInObserverView() {
        PolymerPropertyMutationInObserver template = new PolymerPropertyMutationInObserver();
        template.setId("template");
        template.getElement().addPropertyChangeListener("text",
                event -> add(template.getValueDiv((String) event.getOldValue(),
                        (String) event.getValue())));
        template.setText("initially set value");
        add(template);
    }

}
