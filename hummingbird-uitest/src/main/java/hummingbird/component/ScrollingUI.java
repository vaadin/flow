package hummingbird.component;

import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class ScrollingUI extends AbstractTestUIWithLog {

    @Override
    protected void setup(VaadinRequest request) {
        VerticalLayout vl = new VerticalLayout();
        vl.setHeight("5000px");
        Button topButton = new Button("Button at top");
        Button middleButton = new Button("Button in the middle");
        Button bottomButton = new Button("Button at the bottom");

        vl.addComponent(topButton);

        Label spacer = new Label("");
        vl.addComponent(spacer);
        vl.setExpandRatio(spacer, 1);

        vl.addComponent(middleButton);
        spacer = new Label("");
        vl.addComponent(spacer);
        vl.setExpandRatio(spacer, 1);

        vl.addComponent(bottomButton);

        addComponent(new Button("Scroll to top button", e -> {
            scrollIntoView(topButton);
        }));
        addComponent(new Button("Scroll to middle button", e -> {
            scrollIntoView(middleButton);
        }));
        addComponent(new Button("Scroll to bottom button", e -> {
            scrollIntoView(bottomButton);
        }));
        addComponent(new Button("Set scroll top to 200", e -> {
            setScrollTop(200);
        }));
        addComponent(new Button("Set scroll left to 200", e -> {
            setScrollLeft(200);
        }));
        addComponent(vl);
    }

}
