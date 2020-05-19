/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;

@Route(value = "serverpostponeview", layout = MainLayout.class)
public class ServerSidePostponeView extends Div implements BeforeLeaveObserver {

    NativeButton proceedButton;
    BeforeLeaveEvent lastEvent;

    public ServerSidePostponeView() {
        add(new Text("Server view postpone"));
        setId("serverPostponeView");

        final NativeButton serverViewButton = new NativeButton(
                "Other Server View", buttonClickEvent -> {
                    final UI ui = buttonClickEvent.getSource().getUI().get();

                    ui.navigate(ServerSideView.class);
                });
        serverViewButton.setId("goToServerView");
        add(serverViewButton);

        final NativeButton clientViewButton = new NativeButton("Client View",
                buttonClickEvent -> {
                    final UI ui = buttonClickEvent.getSource().getUI().get();

                    ui.navigate("client-view");
                });
        clientViewButton.setId("goToClientView");
        add(clientViewButton);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        event.postpone();
        lastEvent = event;

        if (proceedButton == null) {
            proceedButton = new NativeButton("Proceed",
                    buttonClickEvent -> {
                        lastEvent.getContinueNavigationAction().proceed();
                    });
            proceedButton.setId("proceedAfterPostpone");
            add(proceedButton);
        }
    }
}
