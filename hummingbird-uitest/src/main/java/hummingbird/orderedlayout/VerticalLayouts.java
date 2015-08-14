package hummingbird.orderedlayout;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

public class VerticalLayouts extends OrderedLayouts {

    @Override
    protected AbstractOrderedLayout createMainLayout() {
        HorizontalLayout hl = new HorizontalLayout();
        hl.setHeight("100%");
        hl.setMargin(true);
        hl.setSpacing(true);
        return hl;
    }

    @Override
    protected AbstractOrderedLayout createLayout() {
        VerticalLayout vl = new VerticalLayout(new Button("Button 1"),
                new Button("Button 2"), new Button("Button 3"),
                new Button("Button 4"));
        vl.setHeight("100%");
        return vl;
    }
}
