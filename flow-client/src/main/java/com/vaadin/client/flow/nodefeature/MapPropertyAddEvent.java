/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client.flow.nodefeature;

import com.vaadin.client.flow.reactive.ReactiveValueChangeEvent;

/**
 * Event fired when a property is added to a {@link NodeMap}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MapPropertyAddEvent extends ReactiveValueChangeEvent {

    private MapProperty property;

    /**
     * Creates a new property add event.
     *
     * @param source
     *            the changed map
     * @param property
     *            the newly added property
     */
    public MapPropertyAddEvent(NodeMap source, MapProperty property) {
        super(source);
        this.property = property;
    }

    @Override
    public NodeMap getSource() {
        return (NodeMap) super.getSource();
    }

    /**
     * Gets the added property.
     *
     * @return the added property
     */
    public MapProperty getProperty() {
        return property;
    }

}
