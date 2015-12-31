package hummingbird.template;

import java.util.List;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

public class TemplateWithLoopVariablesUI extends UI {

    public static class TemplateWithLoopVariables extends Template {

        private static final double INSERT_OFFSET_X = 0.3;
        private static final double INSERT_OFFSET_Y = 0.35;
        private int rows = 0;
        private int cols = 5;
        private int cellIndex = 0;

        @Override
        protected void init() {
            super.init();

            getNode().getMultiValued("rows");

            for (int rowIndex = 0; rowIndex < 5; rowIndex++) {
                insertRow(rowIndex);
            }

        }

        private void insertRow(int rowIndex) {
            System.out.println("Insert row at " + rowIndex);

            Row row = Template.Model.create(Row.class);
            getModel().getRows().add(rowIndex, row);
            ((StateNode) getNode().getMultiValued("rows").get(rowIndex))
                    .getMultiValued("cells");
            List<Cell> cells = row.getCells();
            for (int colIndex = 0; colIndex < cols; colIndex++) {
                cells.add(createCell(rowIndex, colIndex));
            }

            rows++;
        }

        private void removeRow(int rowIndex) {
            System.out.println("Remove row " + rowIndex);
            getModel().getRows().remove(rowIndex);
            rows--;
        }

        private void removeCol(int colIndex) {
            System.out.println("Remove col " + colIndex);
            for (Row row : getModel().getRows()) {
                row.getCells().remove(colIndex);
            }
            cols--;
        }

        private void insertCol(int colIndex) {
            System.out.println("Insert column at " + colIndex);
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                Row row = getModel().getRows().get(rowIndex);
                List<Cell> cells = row.getCells();
                cells.add(colIndex, createCell(rowIndex, colIndex));
            }
            cols++;
        }

        private Cell createCell(int rowIndex, int colIndex) {
            Cell cell = Template.Model.create(Cell.class);
            cell.setContent("Cell " + cellIndex++);
            return cell;
        }

        @Override
        protected Model getModel() {
            return (Model) super.getModel();
        }

        public interface Model extends Template.Model {
            public List<Row> getRows();

            public void setRows(List<Row> rows);
        }

        public interface Row {
            public List<Cell> getCells();

            public void setCells(List<Cell> cells);
        }

        public interface Cell {
            public String getContent();

            public void setContent(String content);
        }

        @TemplateEventHandler
        public void insert(int clickedRow, int clickedCol, double relativeX,
                double relativeY) {
            if (relativeX < INSERT_OFFSET_X
                    || relativeX > (1.0 - INSERT_OFFSET_X)) {
                // Add column
                int insertCol = clickedCol;
                if (relativeX > 0.5) {
                    insertCol++;
                }
                insertCol(insertCol);
            }
            if (relativeY < INSERT_OFFSET_Y
                    || relativeY > (1.0 - INSERT_OFFSET_Y)) {
                // Add row
                int insertRow = clickedRow;
                if (relativeY > 0.5) {
                    insertRow++;
                }
                insertRow(insertRow);
            }
            if (relativeX >= INSERT_OFFSET_X
                    && relativeX <= (1.0 - INSERT_OFFSET_X)
                    && relativeY >= INSERT_OFFSET_Y
                    && relativeY <= (1.0 - INSERT_OFFSET_Y)) {
                // Center click
                removeRow(clickedRow);
                removeCol(clickedCol);
            }
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        TemplateWithLoopVariables tpl = new TemplateWithLoopVariables();
        addComponent(tpl);
    }

}
