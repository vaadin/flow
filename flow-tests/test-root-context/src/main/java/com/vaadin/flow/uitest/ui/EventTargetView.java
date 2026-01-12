/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.EventTargetView", layout = ViewTestLayout.class)
public class EventTargetView extends AbstractEventDataView {

    public static final String TARGET_ID = "target";

    public EventTargetView() {
        final Div eventTarget = new Div();
        eventTarget.setId(TARGET_ID);
        addComponentAtIndex(1, new H3("Event.target reported for any child."));
        addComponentAtIndex(2, eventTarget);

        getElement().addEventListener("click", event -> {
            eventTarget.setText(event.getEventTarget()
                    .map(element -> element.getText()).orElse(EMPTY_VALUE));
        }).mapEventTargetElement();

        createComponents();
    }

}
