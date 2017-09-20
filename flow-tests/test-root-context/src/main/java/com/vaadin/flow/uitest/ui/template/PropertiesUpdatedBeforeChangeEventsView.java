package com.vaadin.flow.uitest.ui.template;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.uitest.ui.AbstractDivView;

public class PropertiesUpdatedBeforeChangeEventsView extends AbstractDivView {

    @Tag("properties-updated-before-change-events")
    @HtmlImport("/com/vaadin/flow/uitest/ui/template/PropertiesUpdatedBeforeChangeEvents.html")
    public static class PropertiesUpdatedBeforeChangeEvents
            extends PolymerTemplate<Message> {

        @Override
        protected Message getModel() {
            return super.getModel();
        }
    }

    public PropertiesUpdatedBeforeChangeEventsView() {
        PropertiesUpdatedBeforeChangeEvents template = new PropertiesUpdatedBeforeChangeEvents();
        template.getElement().synchronizeProperty("firstProp",
                "first-prop-changed");
        template.getElement().synchronizeProperty("secondProp",
                "second-prop-changed");
        template.getElement().addPropertyChangeListener("firstProp",
                event -> template.getModel().setText(
                        template.getElement().getProperty("secondProp")));
        add(template);
    }
}
