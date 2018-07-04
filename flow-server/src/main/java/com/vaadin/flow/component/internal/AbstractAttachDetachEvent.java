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
package com.vaadin.flow.component.internal;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * Internal helper for {@link AttachEvent} and {@link DetachEvent}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractAttachDetachEvent
        extends ComponentEvent<Component> {

    /**
     * Creates a new event with the given component as source.
     *
     * @param source
     *            the component that was attached or detached
     */
    public AbstractAttachDetachEvent(Component source) {
        super(source, false);
    }

    /**
     * Gets the UI the component is attached to.
     *
     * @return the UI this component is attached to
     */
    public UI getUI() {
        return getSource().getUI().get();
    }

    /**
     * Gets the session the component is attached to.
     *
     * @return the session this component is attached to
     */
    public VaadinSession getSession() {
        return getUI().getSession();
    }

}
