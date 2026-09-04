/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * Stands in for an overlay-based component such as a dialog: opening moves the
 * content into an overlay element, disconnecting and reconnecting it.
 */
@JsModule("./TeleportingContainer.ts")
@Tag("teleporting-container")
public class TeleportingContainer extends Component implements HasComponents {

    /**
     * Moves the content into the overlay element. The call is executed after
     * the DOM changes of the same server round trip have been applied, so
     * content added in that round trip is moved right after being attached.
     */
    public void open() {
        getElement().callJsFunction("open");
    }

}
