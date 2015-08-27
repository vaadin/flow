package hummingbird.component;

import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class FocusUI extends AbstractTestUIWithLog {

    @Override
    protected void setup(VaadinRequest request) {
        VerticalLayout playground = new VerticalLayout();
        Button b = new Button("Create and focus after attach", e -> {
            playground.removeAllComponents();
            TextField tf = new TextField("Focused after attach");
            playground.addComponent(tf);
            tf.focus();
        });
        Button b2 = new Button("Create and focus before attach", e -> {
            playground.removeAllComponents();
            TextField tf = new TextField("Focused before attach");
            tf.focus();
            playground.addComponent(tf);
        });
        add(b);
        add(b2);
        add(playground);

    }

}
