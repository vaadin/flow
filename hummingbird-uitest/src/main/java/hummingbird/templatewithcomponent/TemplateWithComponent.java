package hummingbird.templatewithcomponent;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class TemplateWithComponent extends UI {

    @Override
    protected void init(VaadinRequest request) {
        setContent(new GridTemplate());
    }

}
