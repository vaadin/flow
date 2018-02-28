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
 * Represents the context of a callback execution when
 * {@link UI#beforeClientResponse(com.vaadin.flow.component.Component, com.vaadin.flow.function.SerializableConsumer)}
 * is invoked.
 * 
 * @author Vaadin Ltd.
 *
 */
public class ExecutionContext implements Serializable {

    private final UI ui;
    private final boolean wasAttachedInPreviousRoundtrip;

    /**
     * Creates a new, immutable context.
     * 
     * @param ui
     *            The UI associated with the execution
     * @param wasAttachedInPreviousRoundtrip
     *            <code>true</code> if the node was previously attached when the
     *            roundtrip started, and <code>false</code> when the node was
     *            attached during the current roundtrip
     */
    public ExecutionContext(UI ui, boolean wasAttachedInPreviousRoundtrip) {
        assert ui != null;
        this.ui = ui;
        this.wasAttachedInPreviousRoundtrip = wasAttachedInPreviousRoundtrip;
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
     * Gets whether the node associated to the execution was attached to the
     * tree when the server roundtrip was started.
     * 
     * @return <code>true</code> if the node was previously attached when the
     *         roundtrip started, and <code>false</code> when the node was
     *         attached during the current roundtrip
     */
    public boolean wasAttachedInPreviousRoundtrip() {
        return wasAttachedInPreviousRoundtrip;
    }

}
