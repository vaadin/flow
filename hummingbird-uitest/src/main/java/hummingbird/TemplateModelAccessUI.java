package hummingbird;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class TemplateModelAccessUI extends UI {
    @Override
    protected void init(VaadinRequest request) {
        TemplateModelAccess c = new TemplateModelAccess();
        c.setDataValue("test");
        addComponent(c);
    }
}