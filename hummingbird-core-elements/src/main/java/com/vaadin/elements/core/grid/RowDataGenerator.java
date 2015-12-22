package com.vaadin.elements.core.grid;

import com.vaadin.data.DataGenerator;
import com.vaadin.data.Item;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.shared.ui.grid.GridState;
import com.vaadin.ui.renderers.Renderer;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Class for generating all row and cell related data for the essential parts of
 * Grid.
 */
class RowDataGenerator implements DataGenerator {

    /**
     *
     */
    private final Grid grid;

    /**
     * @param grid
     */
    RowDataGenerator(Grid grid) {
        this.grid = grid;
    }

    private void put(String key, String value, JsonObject object) {
        if (value != null && !value.isEmpty()) {
            object.put(key, value);
        }
    }

    @Override
    public void generateData(Object itemId, Item item, JsonObject rowData) {
        RowReference row = new RowReference(grid);
        row.set(itemId);

        if (grid.rowStyleGenerator != null) {
            String style = grid.rowStyleGenerator.getStyle(row);
            put(GridState.JSONKEY_ROWSTYLE, style, rowData);
        }

        if (grid.rowDescriptionGenerator != null) {
            String description = grid.rowDescriptionGenerator
                    .getDescription(row);
            put(GridState.JSONKEY_ROWDESCRIPTION, description, rowData);

        }

        JsonObject cellStyles = Json.createObject();
        JsonObject cellData = Json.createObject();
        JsonObject cellDescriptions = Json.createObject();

        CellReference cell = new CellReference(row);

        for (Column column : grid.getColumns()) {
            cell.set(column.getPropertyId());

            writeData(cell, cellData);
            writeStyles(cell, cellStyles);
            writeDescriptions(cell, cellDescriptions);
        }

        if (grid.cellDescriptionGenerator != null
                && cellDescriptions.keys().length > 0) {
            rowData.put(GridState.JSONKEY_CELLDESCRIPTION, cellDescriptions);
        }

        if (grid.cellStyleGenerator != null && cellStyles.keys().length > 0) {
            rowData.put(GridState.JSONKEY_CELLSTYLES, cellStyles);
        }

        rowData.put(GridState.JSONKEY_DATA, cellData);
    }

    private void writeStyles(CellReference cell, JsonObject styles) {
        if (grid.cellStyleGenerator != null) {
            String style = grid.cellStyleGenerator.getStyle(cell);
            put(grid.getColumnIdByPropertyId(cell.getPropertyId()), style,
                    styles);
        }
    }

    private void writeDescriptions(CellReference cell,
            JsonObject descriptions) {
        if (grid.cellDescriptionGenerator != null) {
            String description = grid.cellDescriptionGenerator
                    .getDescription(cell);
            put(grid.getColumnIdByPropertyId(cell.getPropertyId()), description,
                    descriptions);
        }
    }

    private void writeData(CellReference cell, JsonObject data) {
        Column column = grid.getColumn(cell.getPropertyId());
        Converter<?, ?> converter = column.getConverter();
        Renderer<?> renderer = column.getRenderer();

        Item item = cell.getItem();
        Object modelValue = item.getItemProperty(cell.getPropertyId())
                .getValue();

        data.put(grid.getColumnIdByPropertyId(cell.getPropertyId()),
                AbstractRenderer.encodeValue(modelValue, renderer, converter,
                        grid.getLocale()));
    }
}