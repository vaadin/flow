package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.flow.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.PropertiesUpdatedBeforeChangeEventsView", layout = ViewTestLayout.class)
public class PropertiesUpdatedBeforeChangeEventsView extends AbstractDivView {

    @Tag("properties-updated-before-change-events")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/PropertiesUpdatedBeforeChangeEvents.html")
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
