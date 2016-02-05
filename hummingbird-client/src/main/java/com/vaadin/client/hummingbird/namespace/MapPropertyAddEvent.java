/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird.namespace;

import com.vaadin.client.hummingbird.reactive.ReactiveChangeEvent;

/**
 * Event fired when a property is added to a {@link MapNamespace}.
 *
 * @since
 * @author Vaadin Ltd
 */
public class MapPropertyAddEvent extends ReactiveChangeEvent {

    private MapProperty property;

    /**
     * Creates a new property add event.
     *
     * @param source
     *            the changed map namespace
     * @param property
     *            the newly added property
     */
    public MapPropertyAddEvent(MapNamespace source, MapProperty property) {
        super(source);
        this.property = property;
    }

    @Override
    public MapNamespace getSource() {
        return (MapNamespace) super.getSource();
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
