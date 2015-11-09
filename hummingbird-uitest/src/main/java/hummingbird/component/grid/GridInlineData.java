package hummingbird.component.grid;

import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;

public class GridInlineData extends AbstractTestUIWithLog {

    private Grid grid;
    private int generatedId = 1;
    private int addedColumns = 0;

    @Override
    protected void setup(VaadinRequest request) {
        grid = new Grid();
        grid.addColumn("First");
        grid.addColumn("Last");
        grid.addColumn("Age", Integer.class);
        grid.addColumn("Alive", Boolean.class);

        grid.addRow("Artur", "Signell", 35, true);

        Button addRow = new Button("Add row", e -> {
            if (generatedId == 5) {
                log("Set height to 5 rows");
                grid.setHeightByRows(5);

            }
            Object[] data = new Object[4 + addedColumns];
            data[0] = "First " + generatedId;
            data[1] = "Last " + generatedId;
            data[2] = generatedId;
            data[3] = generatedId % 3 == 0;
            for (int i = 0; i < addedColumns; i++) {
                data[i + 4] = "Data for added column " + (i + 1);
            }
            grid.addRow(data);
            generatedId++;
            
            String rowString = "Added row: ";
            for (int i=0; i < data.length; i++) {
                rowString+=data[i]+", ";
            }
            log(rowString.substring(0, rowString.length()-2));
        });

        Button addColumn = new Button("Add column", e -> {
            addedColumns++;
            grid.addColumn("Added column " + addedColumns);
            log("Added column " + addedColumns);
        });

        addComponent(addRow);
        addComponent(addColumn);
        addComponent(grid);

        log("Initial render");
    }

}
