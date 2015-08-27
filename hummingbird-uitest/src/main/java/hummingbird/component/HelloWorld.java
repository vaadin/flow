package hummingbird.component;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

public class HelloWorld extends UI {

    @Override
    protected void init(VaadinRequest request) {
        addComponent(new Button("Say hello",
                e -> addComponent(new Label("Hello from the server"))));
    }

}
