/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.component.dnd.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Internal class for drag and drop related utility methods. This class is not
 * meant for external usage and can be removed at any point.
 * 
 * @author Vaadin Ltd
 * @since 2.0
 */
public class DnDUtil {

    public static final String DND_CONNECTOR = "frontend://dndConnector";

    private DnDUtil() {
        // no instances from this class
    }

    /**
     * Includes the dnd connector when the component is attached to a UI.
     * 
     * @param component
     *            the component that should be attached
     */
    public static void addDndConnectorWhenComponentAttached(
            Component component) {
        component.getElement().getNode().runWhenAttached(ui -> ui.getPage()
                .addJavaScript(DND_CONNECTOR, LoadMode.EAGER));
    }
}
