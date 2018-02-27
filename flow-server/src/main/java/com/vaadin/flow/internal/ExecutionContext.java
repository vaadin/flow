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
package com.vaadin.flow.internal;

import java.io.Serializable;

import com.vaadin.flow.component.UI;

/**
 * Class that holds the context of a callback execution of a call to
 * {@link UI#beforeClientResponse(com.vaadin.flow.component.Component, com.vaadin.flow.function.SerializableConsumer)}.
 * 
 * @author Vaadin Ltd.
 *
 */
public class ExecutionContext implements Serializable {

    private final UI ui;
    private final boolean wasAttached;

    /**
     * Creates a new, immutable context.
     * 
     * @param ui
     *            The UI associated with the execution
     * @param wasAttached
     *            <code>true</code> when the node associated with the execution
     *            was attached to the tree before the request was processed,
     *            <code>false</code> otherwise.
     */
    public ExecutionContext(UI ui, boolean wasAttached) {
        assert ui != null;
        this.ui = ui;
        this.wasAttached = wasAttached;
    }

    /**
     * Gets the UI associated with the execution.
     * 
     * @return the UI, not <code>null</code>
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Gets the attached status of the node associated with the execution when
     * the request was first received by the server.
     * 
     * @return <code>true</code> when the node associated with the execution was
     *         attached to the tree before the request was processed,
     *         <code>false</code> otherwise.
     */
    public boolean wasAttached() {
        return wasAttached;
    }

}
