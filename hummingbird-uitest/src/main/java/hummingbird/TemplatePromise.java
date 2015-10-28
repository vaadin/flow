package hummingbird;

import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUI;

public class TemplatePromise extends AbstractTestUI {

    @Override
    protected void setup(VaadinRequest request) {
        addComponent(new TemplatePromiseTemplate());
    }

}
