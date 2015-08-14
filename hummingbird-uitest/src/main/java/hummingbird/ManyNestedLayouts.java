package hummingbird;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class ManyNestedLayouts extends UI {

    private int layers = 0;
    private Button b;

    @Override
    protected void init(VaadinRequest request) {
        // setContent(new Button("Foo", e -> {
        // setContent(new VerticalLayout(new Button("bar")));
        // }));
        createLayers(10);
    }

    private void createLayers(int count) {
        VerticalLayout root = new VerticalLayout();
        VerticalLayout last = root;

        for (int i = 1; i < count; i++) {
            VerticalLayout vl = new VerticalLayout();
            last.addComponent(vl);
            last = vl;
        }
        b = new Button("Inside " + count + " VerticalLayouts. Add more!");
        b.getElement().setStyle("display", "inline-block");
        b.addClickListener(e -> {
            createLayers(layers * 2);
        });
        last.addComponent(b);

        layers = count;
        setContent(root);

    }

    // private void addLayers(int i) {
    // wrap(i);
    // layers += i;
    // // b.setCaption("Inside " + layers + " layouts. Add more!");
    //
    // }
    //
    // private void wrap(int count) {
    // VerticalLayout root = new VerticalLayout();
    // VerticalLayout last = root;
    // for (int i = 1; i < count; i++) {
    // VerticalLayout vl = new VerticalLayout();
    // last.addComponent(vl);
    // last = vl;
    // }
    // ((VerticalLayout) b.getParent()).addComponent(root);
    // last.addComponent(b);
    // ;
    // // last.addComponent(getContent());
    // // setContent(root);
    // }

}
