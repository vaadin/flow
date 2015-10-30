package hummingbird.template;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

public class CreatedEvents extends UI {

    @Override
    protected void init(VaadinRequest request) {
        addComponent(new CreatedEventsTemplate());
    }

    public static class CreatedEventsTemplate extends Template {

    }
}
