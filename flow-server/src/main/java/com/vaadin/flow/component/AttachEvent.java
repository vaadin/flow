/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component;

import com.vaadin.flow.component.internal.AbstractAttachDetachEvent;

/**
 * Event fired after a {@link Component} is attached to the UI.
 * <p>
 * When a hierarchy of components is being attached, this event is fired
 * child-first.
 *
 * @since 1.0
 */
public class AttachEvent extends AbstractAttachDetachEvent {

    private boolean initialAttach;

    /**
     * Creates a new attach event with the given component as source.
     *
     * @param source
     *            the component that was attached
     * @param initialAttach
     *            indicates whether this is the first time the component
     *            (element) has been attached
     */
    public AttachEvent(Component source, boolean initialAttach) {
        super(source);
        this.initialAttach = initialAttach;
    }

    /**
     * Checks whether this is the first time the component has been attached.
     *
     * @return <code>true</code> if this it the first time the component has
     *         been attached, <code>false</code> otherwise
     */
    public boolean isInitialAttach() {
        return initialAttach;
    }

}
