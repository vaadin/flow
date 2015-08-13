package hummingbird;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

public class HorizontalLayouts extends OrderedLayouts {

    @Override
    protected AbstractOrderedLayout createMainLayout() {
        VerticalLayout vl = new VerticalLayout();
        vl.setSizeFull();
        vl.setMargin(true);
        vl.setSpacing(true);
        return vl;
    }

    @Override
    protected AbstractOrderedLayout createLayout() {
        return new HorizontalLayout(new Button("Button 1"),
                new Button("Button 2"), new Button("Button 3"),
                new Button("Button 4"));
    }
}
