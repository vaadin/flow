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

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

@Route(value = "serverforwardview", layout = MainLayout.class)
public class ServerSideForwardView extends Div
        implements BeforeEnterObserver, HasUrlParameter<Boolean> {

    NativeButton proceedButton;
    BeforeLeaveEvent lastEvent;

    Boolean parameter;

    public ServerSideForwardView() {
        add(new Text("Server view forward"));
        setId("serverForwardView");

        final NativeButton forwardViewButton = new NativeButton(
                "Open Server View which does forward to Client View",
                buttonClickEvent -> {
                    final UI ui = buttonClickEvent.getSource().getUI().get();

                    ui.navigate(ForwardView.class);
                });
        forwardViewButton.setId("goToServerForwardView");
        add(forwardViewButton);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter Boolean parameter) {
        this.parameter = parameter;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (Boolean.TRUE.equals(parameter)) {
            event.forwardTo("client-view");
        }

        parameter = null;
    }

    @Route(value = "serverforwardingview", layout = MainLayout.class)
    public static class ForwardView extends Div implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.forwardTo("client-view");
        }
    }
}
