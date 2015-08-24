package hummingbird.component;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.UI;

public class CompositeUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        setContent(new MyComposite());
    }

    public static class MyComposite extends Composite {

        public MyComposite() {
        }

        @Override
        protected Component initContent() {
            HorizontalLayout hl = new HorizontalLayout();
            Button first = new Button("First");
            NativeButton second = new NativeButton("Second");
            first.addClickListener(e -> {
                hl.addComponent(new Button("New"));
                // hl.addComponentAsFirst(hl.getComponent(1));
            });
            second.addClickListener(e -> {
                // hl.addComponentAsFirst(hl.getComponent(1));
            });

            hl.addComponents(first, second);
            return hl;
        }

    }
}
