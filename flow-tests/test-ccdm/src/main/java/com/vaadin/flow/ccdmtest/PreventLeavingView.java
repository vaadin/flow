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
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;

@Route(value = "prevent-leaving", layout = MainLayout.class)
public class PreventLeavingView extends Div implements BeforeLeaveObserver {
    private String preventRoute;

    public PreventLeavingView() {
        Input input = new Input();
        input.setId("preventRouteInput");
        add(input);
        NativeButton submitPreventRoute = new NativeButton(
                "Submit prevent route");
        submitPreventRoute.addClickListener(event -> {
            preventRoute = input.getValue();
            String preventedMessage = String
                    .format("preventing navigation to '%s'", preventRoute);
            Paragraph paragraph = new Paragraph(preventedMessage);
            paragraph.setClassName("prevented-route");
            add(paragraph);
        });
        submitPreventRoute.setId("preventRouteButton");
        add(submitPreventRoute);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (preventRoute != null
                && event.getLocation().getPath().equals(preventRoute)) {
            event.postpone();
        }
    }
}
