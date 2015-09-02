package hummingbird;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.ui.Template;

public class TemplateModelAccess extends Template {
    public TemplateModelAccess() {
        getNode().put("data", "server data");
    }

    @TemplateEventHandler
    protected void dataUpdated() {
        getNode().put("message", "server data is now " + getNode().get("data"));
    }
}
