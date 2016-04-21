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
package com.vaadin.ui;

/**
 * Event fired after a {@link Component} is attached to the UI.
 * <p>
 * When a hierarchy of components is being attached, this event is fired
 * child-first.
 */
public class AttachEvent extends ComponentEvent {

    /**
     * Creates a new attach event with the given component as source.
     * 
     * @param source
     *            the component that was attached
     */
    public AttachEvent(Component source) {
        super(source, false);
    }

}
