package hummingbird.component.grid;

import com.vaadin.elements.core.grid.Grid;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

public class AddGridAfterInit extends UI {

    @Override
    protected void init(VaadinRequest request) {
        Button b = new Button("add grid", e -> {
            Grid grid = new Grid();
            grid.addColumn("First");
            grid.addColumn("Last");
            grid.addRow("Foo", "bar");
            addComponent(grid);
        });
        addComponent(b);
    }

}
