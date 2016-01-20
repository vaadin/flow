package com.vaadin.elements.core.grid;

import com.vaadin.data.DataGenerator;
import com.vaadin.ui.AbstractClientConnector;
import com.vaadin.ui.GridJSDataProvider;

/**
 * An abstract base class for server-side Grid extensions.
 * <p>
 * Note: If the extension is an instance of {@link DataGenerator} it will
 * automatically register itself to {@link GridJSDataProvider} of extended Grid.
 * On remove this registration is automatically removed.
 *
 * @since 7.5
 */
public abstract class AbstractGridExtension {

    private Grid parent;

    /**
     * Constructs a new Grid extension.
     */
    public AbstractGridExtension() {
        super();
    }

    /**
     * Constructs a new Grid extension and extends given Grid.
     *
     * @param grid
     *            a grid instance
     */
    public AbstractGridExtension(Grid grid) {
        super();
        extend(grid);
    }

    protected void extend(AbstractClientConnector target) {
        parent = (Grid) target;
        // super.extend(target);

        // if (this instanceof DataGenerator) {
        // getParent().datasourceExtension
        // .addDataGenerator((DataGenerator) this);
        // }
    }

    public void remove() {
        parent = null;
        // if (this instanceof DataGenerator) {
        // getParent().datasourceExtension
        // .removeDataGenerator((DataGenerator) this);
        // }

        // super.remove();
    }

    // /**
    // * Gets the item id for a row key.
    // * <p>
    // * A key is used to identify a particular row on both a server and a
    // * client. This method can be used to get the item id for the row key
    // * that the client has sent.
    // *
    // * @param rowKey
    // * the row key for which to retrieve an item id
    // * @return the item id corresponding to {@code key}
    // */
    // protected Object getItemId(String rowKey) {
    // return getParent().getKeyMapper().get(rowKey);
    // }

    /**
     * Gets the column for a column id.
     * <p>
     * An id is used to identify a particular column on both a server and a
     * client. This method can be used to get the column for the column id that
     * the client has sent.
     *
     * @param columnId
     *            the column id for which to retrieve a column
     * @return the column corresponding to {@code columnId}
     */
    protected Column getColumn(String columnId) {
        return getParent().getColumnByColumnId(columnId);
    }

    /**
     * Gets the parent Grid of the renderer.
     *
     * @return parent grid
     * @throws IllegalStateException
     *             if parent is not Grid
     */
    public Grid getParent() {
        return parent;
        // if (getParent() instanceof Grid) {
        // Grid grid = (Grid) getParent();
        // return grid;
        // } else if (getParent() == null) {
        // throw new IllegalStateException(
        // "Renderer is not attached to any parent");
        // } else {
        // throw new IllegalStateException(
        // "Renderers can be used only with Grid. Extended "
        // + getParent().getClass().getSimpleName()
        // + " instead");
        // }
    }

    /**
     * Resends the row data for given item id to the client.
     *
     * @since
     * @param itemId
     *            row to refresh
     */
    protected void refreshRow(Object itemId) {
        // getParent().datasourceExtension.updateRowData(itemId);
    }
}