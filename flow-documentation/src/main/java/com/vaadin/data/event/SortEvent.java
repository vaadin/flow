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
package com.vaadin.data.event;

import java.util.List;

import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.Component;

@CodeFor("data-provider/tutorial-flow-data-provider.asciidoc")
public class SortEvent extends com.vaadin.flow.data.event.SortEvent {

    public interface SortNotifier<T extends Component, S extends SortOrder<?>>
            extends
            com.vaadin.flow.data.event.SortEvent.SortNotifier<Component, SortOrder<?>> {

    }

    public SortEvent(Component source, List sortOrder, boolean fromClient) {
        super(source, sortOrder, fromClient);
    }

}
