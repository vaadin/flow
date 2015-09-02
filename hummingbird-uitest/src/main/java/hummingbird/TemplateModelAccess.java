package hummingbird;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Template;

public class TemplateModelAccess extends Template {
    @TemplateEventHandler
    protected void dataUpdated() {
        Notification.show("Data updated to " + getDataValue());
    }

    public String getDataValue() {
        return getNode().get("data", String.class);
    }

    public void setDataValue(String value) {
        getNode().put("data", value);
    }
}
