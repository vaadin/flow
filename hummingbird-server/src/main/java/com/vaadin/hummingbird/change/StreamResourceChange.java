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
package com.vaadin.hummingbird.change;

import java.util.Collection;
import java.util.Collections;

import com.vaadin.server.communication.StreamResourceReference;

/**
 * @author Vaadin Ltd
 *
 */
public class StreamResourceChange {

    private final Collection<StreamResourceReference> added;

    private final Collection<StreamResourceReference> removed;

    public StreamResourceChange(Collection<StreamResourceReference> added,
            Collection<StreamResourceReference> removed) {
        this.added = added;
        this.removed = removed;
    }

    public Collection<StreamResourceReference> getRegisteredResources() {
        return Collections.unmodifiableCollection(added);
    }

    public Collection<StreamResourceReference> getUnregisteredResources() {
        return Collections.unmodifiableCollection(removed);
    }
}
