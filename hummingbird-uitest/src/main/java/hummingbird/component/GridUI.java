package hummingbird.component;

import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.Grid;

public class GridUI extends AbstractTestUIWithLog {

    @Override
    protected void setup(VaadinRequest request) {
        Grid grid = new Grid();
        grid.addColumn("First Name");
        grid.addColumn("Last Name");
        grid.addRow("John", "Doe");
        addComponent(grid);

    }

}
