/*
 * Copyright 2000-2014 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.ui;

import java.util.Collection;

import com.vaadin.annotations.NotYetImplemented;
import com.vaadin.data.Container;

/**
 * Multiselect component with two lists: left side for available items and right
 * side for selected items.
 */
@SuppressWarnings("serial")
@NotYetImplemented()
public class TwinColSelect extends AbstractSelect {

    private int columns = 0;
    private int rows = 0;

    private String leftColumnCaption;
    private String rightColumnCaption;

    /**
     *
     */
    public TwinColSelect() {
        super();
        setMultiSelect(true);
    }

    /**
     * @param caption
     */
    public TwinColSelect(String caption) {
        super(caption);
        setMultiSelect(true);
    }

    /**
     * @param caption
     * @param dataSource
     */
    public TwinColSelect(String caption, Container dataSource) {
        super(caption, dataSource);
        setMultiSelect(true);
    }

    public int getRows() {
        return rows;
    }

    /**
     * Sets the number of rows in the editor. If the number of rows is set to 0,
     * the actual number of displayed rows is determined implicitly by the
     * adapter.
     * <p>
     * If a height if set (using {@link #setHeight(String)} or
     * {@link #setHeight(float, int)}) it overrides the number of rows. Leave
     * the height undefined to use this method. This is the opposite of how
     * {@link #setColumns(int)} work.
     *
     *
     * @param rows
     *            the number of rows to set.
     */
    public void setRows(int rows) {
        if (rows < 0) {
            rows = 0;
        }
        if (this.rows != rows) {
            this.rows = rows;
            markAsDirty();
        }
    }

    /**
     * @param caption
     * @param options
     */
    public TwinColSelect(String caption, Collection<?> options) {
        super(caption, options);
        setMultiSelect(true);
    }

    /**
     * Sets the text shown above the right column.
     *
     * @param caption
     *            The text to show
     */
    public void setRightColumnCaption(String rightColumnCaption) {
        this.rightColumnCaption = rightColumnCaption;
        markAsDirty();
    }

    /**
     * Returns the text shown above the right column.
     *
     * @return The text shown or null if not set.
     */
    public String getRightColumnCaption() {
        return rightColumnCaption;
    }

    /**
     * Sets the text shown above the left column.
     *
     * @param caption
     *            The text to show
     */
    public void setLeftColumnCaption(String leftColumnCaption) {
        this.leftColumnCaption = leftColumnCaption;
        markAsDirty();
    }

    /**
     * Returns the text shown above the left column.
     *
     * @return The text shown or null if not set.
     */
    public String getLeftColumnCaption() {
        return leftColumnCaption;
    }

}
