/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.grid;

import com.vaadin.function.SerializableRunnable;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.ClientDelegate;
import com.vaadin.ui.common.HtmlImport;

/**
 * Server side implementation for the flow specific grid selection column.
 *
 * @author Vaadin Ltd.
 */
@Tag("vaadin-grid-flow-selection-column")
@HtmlImport("frontend://vaadin-grid-flow-selection-column.html")
public class GridSelectionColumn extends Component {

    private final SerializableRunnable selectAllCallback;
    private final SerializableRunnable deselectAllCallback;

    /**
     * Constructs a new grid selection column configured to use the given
     * callbacks whenever the select all checkbox is toggled on the client side.
     *
     * @param selectAllCallback
     *            the runnable to run when the select all checkbox has been
     *            checked
     * @param deselectAllCallback
     *            the runnable to run when the select all checkbox has been
     *            unchecked
     */
    public GridSelectionColumn(SerializableRunnable selectAllCallback,
            SerializableRunnable deselectAllCallback) {
        this.selectAllCallback = selectAllCallback;
        this.deselectAllCallback = deselectAllCallback;
    }

    /**
     * Sets the checked state of the select all checkbox on the client.
     *
     * @param selectAll
     *            the new state of the select all checkbox
     */
    public void setSelectAllCheckboxState(boolean selectAll) {
        getElement().setProperty("selectAll", selectAll);
    }

    /**
     * Sets the visibility of the select all checkbox on the client.
     *
     * @param visible
     *            whether to display the select all checkbox or hide it
     */
    public void setSelectAllCheckBoxVisibility(boolean visible) {
        getElement().setProperty("selectAllHidden", !visible);
    }

    @ClientDelegate
    private void selectAll() {
        selectAllCallback.run();
    }

    @ClientDelegate
    private void deselectAll() {
        deselectAllCallback.run();
    }
}
