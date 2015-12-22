package com.vaadin.elements.core.grid;

import java.io.Serializable;

import com.vaadin.data.Container.Sortable;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.ConverterUtil;
import com.vaadin.elements.core.grid.headerfooter.HeaderRow;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.grid.GridColumnState;
import com.vaadin.shared.ui.grid.GridConstants;
import com.vaadin.ui.Field;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.Renderer;
import com.vaadin.ui.renderers.TextRenderer;

/**
 * A column in the grid. Can be obtained by calling
 * {@link Grid#getColumn(Object propertyId)}.
 */
public class Column implements Serializable {

    /**
     * The state of the column shared to the client
     */
    private final StateNode state;

    /**
     * The grid this column is associated with
     */
    private final Grid grid;

    /**
     * Backing property for column
     */
    private final Object propertyId;

    private Converter<?, Object> converter;

    /**
     * A check for allowing the {@link #Column(Grid, GridColumnState, Object)
     * constructor} to call {@link #setConverter(Converter)} with a
     * <code>null</code>, even if model and renderer aren't compatible.
     */
    private boolean isFirstConverterAssignment = true;

    // TODO Remove
    private Renderer<?> renderer = new TextRenderer();

    /**
     * Internally used constructor.
     *
     * @param grid
     *            The grid this column belongs to. Should not be null.
     * @param state
     *            the shared state of this column
     * @param propertyId
     *            the backing property id for this column
     */
    Column(Grid grid, StateNode state, Object propertyId) {
        this.grid = grid;
        this.state = state;
        this.propertyId = propertyId;
        internalSetRenderer(new TextRenderer());
    }

    /**
     * Returns the serializable state of this column that is sent to the client
     * side connector.
     *
     * @return the internal state of the column
     */
    StateNode getState() {
        return state;
    }

    /**
     * Returns the property id for the backing property of this Column
     *
     * @return property id
     */
    public Object getPropertyId() {
        return propertyId;
    }

    /**
     * Returns the caption of the header. By default the header caption is the
     * property id of the column.
     *
     * @return the text in the default row of header, null if no default row
     *
     * @throws IllegalStateException
     *             if the column no longer is attached to the grid
     */
    public String getHeaderCaption() throws IllegalStateException {
        checkColumnIsAttached();
        HeaderRow row = grid.getHeader().getDefaultRow();
        if (row != null) {
            return row.getCell(getPropertyId()).getText();
        }
        return null;
    }

    /**
     * Sets the caption of the header. This caption is also used as the hiding
     * toggle caption, unless it is explicitly set via
     * {@link #setHidingToggleCaption(String)}.
     *
     * @param caption
     *            the text to show in the caption
     * @return the column itself
     *
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     */
    public Column setHeaderCaption(String caption)
            throws IllegalStateException {
        checkColumnIsAttached();

        // FIXME This is not how it should really be done
        // The functionality should be implemented in
        // HeaderRow/HeaderCell.setText and other methods
        state.put("name", caption);

        HeaderRow row = grid.getHeader().getDefaultRow();
        if (row != null) {

            row.getCell(getPropertyId()).setText(caption);
        }
        return this;
    }

    /**
     * Gets the caption of the hiding toggle for this column.
     *
     * @since 7.5.0
     * @see #setHidingToggleCaption(String)
     * @return the caption for the hiding toggle for this column
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     */
    public String getHidingToggleCaption() throws IllegalStateException {
        checkColumnIsAttached();
        return state.get("hidingToggleCaption", (String) null);
    }

    /**
     * Sets the caption of the hiding toggle for this column. Shown in the
     * toggle for this column in the grid's sidebar when the column is
     * {@link #isHidable() hidable}.
     * <p>
     * The default value is <code>null</code>, and in that case the column's
     * {@link #getHeaderCaption() header caption} is used.
     * <p>
     * <em>NOTE:</em> setting this to empty string might cause the hiding toggle
     * to not render correctly.
     *
     * @since 7.5.0
     * @param hidingToggleCaption
     *            the text to show in the column hiding toggle
     * @return the column itself
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     */
    public Column setHidingToggleCaption(String hidingToggleCaption)
            throws IllegalStateException {
        // FIXME does not work
        checkColumnIsAttached();
        state.put("hidingToggleCaption", hidingToggleCaption);
        grid.markAsDirty();
        return this;
    }

    /**
     * Returns the width (in pixels). By default a column is 100px wide.
     *
     * @return the width in pixels of the column
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     */
    public double getWidth() throws IllegalStateException {
        checkColumnIsAttached();
        return state.get("width", GridConstants.DEFAULT_COLUMN_WIDTH_PX);
    }

    /**
     * Sets the width (in pixels).
     * <p>
     * This overrides any configuration set by any of
     * {@link #setExpandRatio(int)}, {@link #setMinimumWidth(double)} or
     * {@link #setMaximumWidth(double)}.
     *
     * @param pixelWidth
     *            the new pixel width of the column
     * @return the column itself
     *
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     * @throws IllegalArgumentException
     *             thrown if pixel width is less than zero
     */
    public Column setWidth(double pixelWidth)
            throws IllegalStateException, IllegalArgumentException {
        checkColumnIsAttached();
        if (pixelWidth < 0) {
            throw new IllegalArgumentException(
                    "Pixel width should be greated than 0 (in " + toString()
                            + ")");
        }
        state.put("width", pixelWidth);
        grid.markAsDirty();
        return this;
    }

    /**
     * Marks the column width as undefined meaning that the grid is free to
     * resize the column based on the cell contents and available space in the
     * grid.
     *
     * @return the column itself
     */
    public Column setWidthUndefined() {
        checkColumnIsAttached();
        state.put("width", -1);
        grid.markAsDirty();
        return this;
    }

    /**
     * Checks if column is attached and throws an {@link IllegalStateException}
     * if it is not
     *
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     */
    protected void checkColumnIsAttached() throws IllegalStateException {
        // FIXME
        // if (grid.getColumnByColumnId(state.id) == null) {
        // throw new IllegalStateException("Column no longer exists.");
        // }
    }

    /**
     * Sets this column as the last frozen column in its grid.
     *
     * @return the column itself
     *
     * @throws IllegalArgumentException
     *             if the column is no longer attached to any grid
     * @see Grid#setFrozenColumnCount(int)
     */
    public Column setLastFrozenColumn() {
        checkColumnIsAttached();
        grid.setFrozenColumnCount(grid.getColumns().indexOf(this) + 1);
        return this;
    }

    /**
     * Sets the renderer for this column.
     * <p>
     * If a suitable converter isn't defined explicitly, the session converter
     * factory is used to find a compatible converter.
     *
     * @param renderer
     *            the renderer to use
     * @return the column itself
     *
     * @throws IllegalArgumentException
     *             if no compatible converter could be found
     *
     * @see VaadinSession#getConverterFactory()
     * @see ConverterUtil#getConverter(Class, Class, VaadinSession)
     * @see #setConverter(Converter)
     */
    public Column setRenderer(Renderer<?> renderer) {
        if (!internalSetRenderer(renderer)) {
            throw new IllegalArgumentException(
                    "Could not find a converter for converting from the model type "
                            + getModelType()
                            + " to the renderer presentation type "
                            + renderer.getPresentationType() + " (in "
                            + toString() + ")");
        }
        return this;
    }

    /**
     * Sets the renderer for this column and the converter used to convert from
     * the property value type to the renderer presentation type.
     *
     * @param renderer
     *            the renderer to use, cannot be null
     * @param converter
     *            the converter to use
     * @return the column itself
     *
     * @throws IllegalArgumentException
     *             if the renderer is already associated with a grid column
     */
    public <T> Column setRenderer(Renderer<T> renderer,
            Converter<? extends T, ?> converter) {
        if (renderer.getParent() != null) {
            throw new IllegalArgumentException(
                    "Cannot set a renderer that is already connected to a grid column (in "
                            + toString() + ")");
        }

        if (getRenderer() != null) {
            // grid.removeExtension(getRenderer());
        }

        grid.addRenderer(renderer);
        // FIXME
        // state.rendererConnector = renderer;
        setConverter(converter);
        return this;
    }

    /**
     * Sets the converter used to convert from the property value type to the
     * renderer presentation type.
     *
     * @param converter
     *            the converter to use, or {@code null} to not use any
     *            converters
     * @return the column itself
     *
     * @throws IllegalArgumentException
     *             if the types are not compatible
     */
    public Column setConverter(Converter<?, ?> converter)
            throws IllegalArgumentException {
        Class<?> modelType = getModelType();
        if (converter != null) {
            if (!converter.getModelType().isAssignableFrom(modelType)) {
                throw new IllegalArgumentException(
                        "The converter model type " + converter.getModelType()
                                + " is not compatible with the property type "
                                + modelType + " (in " + toString() + ")");

            } else if (!getRenderer().getPresentationType()
                    .isAssignableFrom(converter.getPresentationType())) {
                throw new IllegalArgumentException(
                        "The converter presentation type "
                                + converter.getPresentationType()
                                + " is not compatible with the renderer presentation type "
                                + getRenderer().getPresentationType() + " (in "
                                + toString() + ")");
            }
        }

        else {
            /*
             * Since the converter is null (i.e. will be removed), we need to
             * know that the renderer and model are compatible. If not, we can't
             * allow for this to happen.
             *
             * The constructor is allowed to call this method with null without
             * any compatibility checks, therefore we have a special case for
             * it.
             */

            Class<?> rendererPresentationType = getRenderer()
                    .getPresentationType();
            if (!isFirstConverterAssignment
                    && !rendererPresentationType.isAssignableFrom(modelType)) {
                throw new IllegalArgumentException("Cannot remove converter, "
                        + "as renderer's presentation type "
                        + rendererPresentationType.getName() + " and column's "
                        + "model " + modelType.getName() + " type aren't "
                        + "directly compatible with each other (in "
                        + toString() + ")");
            }
        }

        isFirstConverterAssignment = false;

        @SuppressWarnings("unchecked")
        Converter<?, Object> castConverter = (Converter<?, Object>) converter;
        this.converter = castConverter;

        return this;
    }

    /**
     * Returns the renderer instance used by this column.
     *
     * @return the renderer
     */
    public Renderer<?> getRenderer() {
        // FIXME
        return renderer;
        // return getState().rendererConnector;
    }

    /**
     * Returns the converter instance used by this column.
     *
     * @return the converter
     */
    public Converter<?, ?> getConverter() {
        return converter;
    }

    private <T> boolean internalSetRenderer(Renderer<T> renderer) {

        Converter<? extends T, ?> converter;
        if (isCompatibleWithProperty(renderer, getConverter())) {
            // Use the existing converter (possibly none) if types
            // compatible
            converter = (Converter<? extends T, ?>) getConverter();
        } else {
            converter = ConverterUtil.getConverter(
                    renderer.getPresentationType(), getModelType(),
                    getSession());
        }
        setRenderer(renderer, converter);
        return isCompatibleWithProperty(renderer, converter);
    }

    private VaadinSession getSession() {
        UI ui = grid.getUI();
        return ui != null ? ui.getSession() : null;
    }

    private boolean isCompatibleWithProperty(Renderer<?> renderer,
            Converter<?, ?> converter) {
        Class<?> type;
        if (converter == null) {
            type = getModelType();
        } else {
            type = converter.getPresentationType();
        }
        return renderer.getPresentationType().isAssignableFrom(type);
    }

    private Class<?> getModelType() {
        return grid.getContainerDataSource().getType(propertyId);
    }

    /**
     * Sets whether the column should be sortable by the user. The grid can be
     * sorted by a sortable column by clicking or tapping the column's default
     * header. Programmatic sorting using the Grid.sort methods is not affected
     * by this setting.
     *
     * @param sortable
     *            <code>true</code> if the user should be able to sort the
     *            column, false otherwise
     * @return the column itself
     */
    public Column setSortable(boolean sortable) {
        checkColumnIsAttached();

        if (sortable) {
            if (!(grid.getContainerDataSource() instanceof Sortable)) {
                throw new IllegalStateException("Can't set column " + toString()
                        + " sortable. The Container of Grid does not implement Sortable");
            } else if (!((Sortable) grid.getContainerDataSource())
                    .getSortableContainerPropertyIds()
                    .contains(getPropertyId())) {
                throw new IllegalStateException("Can't set column " + toString()
                        + " sortable. Container doesn't support sorting by property "
                        + propertyId);
            }
        }

        state.put("sortable", sortable);
        return this;
    }

    /**
     * Returns whether the user is able to sort the grid by this column.
     *
     * @return true if the column is sortable by the user, false otherwise
     */
    public boolean isSortable() {
        return state.get("sortable", true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[propertyId:" + getPropertyId()
                + "]";
    }

    /**
     * Sets the ratio with which the column expands.
     * <p>
     * By default, all columns expand equally (treated as if all of them had an
     * expand ratio of 1). Once at least one column gets a defined expand ratio,
     * the implicit expand ratio is removed, and only the defined expand ratios
     * are taken into account.
     * <p>
     * If a column has a defined width ({@link #setWidth(double)}), it overrides
     * this method's effects.
     * <p>
     * <em>Example:</em> A grid with three columns, with expand ratios 0, 1 and
     * 2, respectively. The column with a <strong>ratio of 0 is exactly as wide
     * as its contents requires</strong>. The column with a ratio of 1 is as
     * wide as it needs, <strong>plus a third of any excess space</strong>,
     * because we have 3 parts total, and this column reserves only one of
     * those. The column with a ratio of 2, is as wide as it needs to be,
     * <strong>plus two thirds</strong> of the excess width.
     *
     * @param expandRatio
     *            the expand ratio of this column. {@code 0} to not have it
     *            expand at all. A negative number to clear the expand value.
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     * @see #setWidth(double)
     */
    public Column setExpandRatio(int expandRatio) throws IllegalStateException {
        checkColumnIsAttached();

        getState().put("flex", expandRatio);
        return this;
    }

    /**
     * Returns the column's expand ratio.
     *
     * @return the column's expand ratio
     * @see #setExpandRatio(int)
     */
    public int getExpandRatio() {
        return getState().get("flex", GridConstants.DEFAULT_EXPAND_RATIO);
    }

    /**
     * Clears the expand ratio for this column.
     * <p>
     * Equal to calling {@link #setExpandRatio(int) setExpandRatio(-1)}
     *
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     */
    public Column clearExpandRatio() throws IllegalStateException {
        return setExpandRatio(-1);
    }

    /**
     * Sets the minimum width for this column.
     * <p>
     * This defines the minimum guaranteed pixel width of the column
     * <em>when it is set to expand</em>.
     *
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     * @see #setExpandRatio(int)
     */
    public Column setMinimumWidth(double pixels) throws IllegalStateException {
        checkColumnIsAttached();

        final double maxwidth = getMaximumWidth();
        if (pixels >= 0 && pixels > maxwidth && maxwidth >= 0) {
            throw new IllegalArgumentException("New minimum width (" + pixels
                    + ") was greater than maximum width (" + maxwidth + ")");
        }
        getState().put("minWidth", pixels);
        return this;
    }

    /**
     * Return the minimum width for this column.
     *
     * @return the minimum width for this column
     * @see #setMinimumWidth(double)
     */
    public double getMinimumWidth() {
        return getState().get("minWidth", GridConstants.DEFAULT_MIN_WIDTH);
    }

    /**
     * Sets the maximum width for this column.
     * <p>
     * This defines the maximum allowed pixel width of the column
     * <em>when it is set to expand</em>.
     *
     * @param pixels
     *            the maximum width
     * @throws IllegalStateException
     *             if the column is no longer attached to any grid
     * @see #setExpandRatio(int)
     */
    public Column setMaximumWidth(double pixels) {
        checkColumnIsAttached();

        final double minwidth = getMinimumWidth();
        if (pixels >= 0 && pixels < minwidth && minwidth >= 0) {
            throw new IllegalArgumentException("New maximum width (" + pixels
                    + ") was less than minimum width (" + minwidth + ")");
        }

        getState().put("maxWidth", pixels);
        return this;
    }

    /**
     * Returns the maximum width for this column.
     *
     * @return the maximum width for this column
     * @see #setMaximumWidth(double)
     */
    public double getMaximumWidth() {
        return getState().get("maxWidth", GridConstants.DEFAULT_MAX_WIDTH);
    }

    /**
     * Sets whether the properties corresponding to this column should be
     * editable when the item editor is active. By default columns are editable.
     * <p>
     * Values in non-editable columns are currently not displayed when the
     * editor is active, but this will probably change in the future. They are
     * not automatically assigned an editor field and, if one is manually
     * assigned, it is not used. Columns that cannot (or should not) be edited
     * even in principle should be set non-editable.
     *
     * @param editable
     *            {@code true} if this column should be editable, {@code false}
     *            otherwise
     * @return this column
     *
     * @throws IllegalStateException
     *             if the editor is currently active
     *
     * @see Grid#editItem(Object)
     * @see Grid#isEditorActive()
     */
    public Column setEditable(boolean editable) {
        checkColumnIsAttached();
        if (grid.isEditorActive()) {
            throw new IllegalStateException(
                    "Cannot change column editable status while the editor is active");
        }
        // TODO ensure editable == false => editable removed
        getState().put("editable", editable);
        grid.markAsDirty();
        return this;
    }

    /**
     * Returns whether the properties corresponding to this column should be
     * editable when the item editor is active.
     *
     * @return {@code true} if this column is editable, {@code false} otherwise
     *
     * @see Grid#editItem(Object)
     * @see #setEditable(boolean)
     */

    public boolean isEditable() {
        return getState().get("editable", false);
    }

    /**
     * Sets the field component used to edit the properties in this column when
     * the item editor is active. If an item has not been set, then the binding
     * is postponed until the item is set using {@link #editItem(Object)}.
     * <p>
     * Setting the field to <code>null</code> clears any previously set field,
     * causing a new field to be created the next time the item editor is
     * opened.
     *
     * @param editor
     *            the editor field
     * @return this column
     */
    public Column setEditorField(Field<?> editor) {
        grid.setEditorField(getPropertyId(), editor);
        return this;
    }

    /**
     * Returns the editor field used to edit the properties in this column when
     * the item editor is active. Returns null if the column is not
     * {@link Column#isEditable() editable}.
     * <p>
     * When {@link #editItem(Object) editItem} is called, fields are
     * automatically created and bound for any unbound properties.
     * <p>
     * Getting a field before the editor has been opened depends on special
     * support from the {@link FieldGroup} in use. Using this method with a
     * user-provided <code>FieldGroup</code> might cause
     * {@link com.vaadin.data.fieldgroup.FieldGroup.BindException BindException}
     * to be thrown.
     *
     * @return the bound field; or <code>null</code> if the respective column is
     *         not editable
     *
     * @throws IllegalArgumentException
     *             if there is no column for the provided property id
     * @throws FieldGroup.BindException
     *             if no field has been configured and there is a problem
     *             building or binding
     */
    public Field<?> getEditorField() {
        return grid.getEditorField(getPropertyId());
    }

    /**
     * Hides or shows the column. By default columns are visible before
     * explicitly hiding them.
     *
     * @since 7.5.0
     * @param hidden
     *            <code>true</code> to hide the column, <code>false</code> to
     *            show
     * @return this column
     */
    public Column setHidden(boolean hidden) {
        if (hidden != isHidden()) {
            getState().put("hidden", hidden);
            grid.fireColumnVisibilityChangeEvent(this, hidden, false);
        }
        return this;
    }

    /**
     * Is this column hidden. Default is {@code false}.
     *
     * @since 7.5.0
     * @return <code>true</code> if the column is currently hidden,
     *         <code>false</code> otherwise
     */
    public boolean isHidden() {
        return getState().get("hidden", false);
    }

    /**
     * Set whether it is possible for the user to hide this column or not.
     * Default is {@code false}.
     * <p>
     * <em>Note:</em> it is still possible to hide the column programmatically
     * using {@link #setHidden(boolean)}
     *
     * @since 7.5.0
     * @param hidable
     *            <code>true</code> iff the column may be hidable by the user
     *            via UI interaction
     * @return this column
     */
    public Column setHidable(boolean hidable) {
        getState().put("hidable", hidable);
        return this;
    }

    /**
     * Is it possible for the the user to hide this column. Default is
     * {@code false}.
     * <p>
     * <em>Note:</em> the column can be programmatically hidden using
     * {@link #setHidden(boolean)} regardless of the returned value.
     *
     * @since 7.5.0
     * @return <code>true</code> if the user can hide the column,
     *         <code>false</code> if not
     */
    public boolean isHidable() {
        return getState().get("hidable", false);
    }

}