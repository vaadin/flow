package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.PropertiesUpdatedBeforeChangeEventsView", layout = ViewTestLayout.class)
public class PropertiesUpdatedBeforeChangeEventsView extends AbstractDivView {

    @Tag("properties-updated-before-change-events")
    @JsModule("PropertiesUpdatedBeforeChangeEvents.js")
    public static class PropertiesUpdatedBeforeChangeEvents
            extends PolymerTemplate<Message> {

        @Override
        protected Message getModel() {
            return super.getModel();
        }
    }

    public PropertiesUpdatedBeforeChangeEventsView() {
        PropertiesUpdatedBeforeChangeEvents template = new PropertiesUpdatedBeforeChangeEvents();
        template.getElement().addPropertyChangeListener("firstProp",
                "first-prop-changed",
                event -> template.getModel().setText(
                        template.getElement().getProperty("secondProp")))
                .synchronizeProperty("secondProp");
        add(template);
    }
}
