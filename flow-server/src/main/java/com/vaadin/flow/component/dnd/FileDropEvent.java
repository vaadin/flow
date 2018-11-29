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
package com.vaadin.flow.component.dnd;

import java.util.Collection;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * File drop event that contains the collection of files dropped on a file drop
 * target.
 *
 * @param <T>
 *            Type of the file drop target component.
 * @author Vaadin Ltd
 * @see FileDropHandler
 */
public class FileDropEvent<T extends Component> extends ComponentEvent<T> {

    private final Collection<Html5File> files;

    /**
     * Creates a file drop event.
     *
     * @param target
     *            The file drop target component.
     * @param files
     *            Collection of files.
     */
    public FileDropEvent(T target, Collection<Html5File> files) {
        super(target, true);

        this.files = files;
    }

    /**
     * Gets the collection of files dropped onto the file drop target component.
     *
     * @return Collection of files that were dropped onto the file drop target
     *         component.
     */
    public Collection<Html5File> getFiles() {
        return files;
    }
}
