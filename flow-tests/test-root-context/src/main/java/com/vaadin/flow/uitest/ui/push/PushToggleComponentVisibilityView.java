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
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Route("com.vaadin.flow.uitest.ui.push.PushToggleComponentVisibilityView")
public class PushToggleComponentVisibilityView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET_XHR);

        Div mainLayout = new Div();

        Div label = new Div();
        label.setText("Please wait");
        label.setId("label");
        label.setVisible(false);
        mainLayout.add(label);

        NativeButton button = new NativeButton("Hide me for 3 seconds");
        button.setId("hide");

        button.addClickListener(event1 -> {
            button.setVisible(false);
            label.setVisible(true);

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ui.access(() -> {
                    button.setVisible(true);
                    label.setVisible(false);
                    ui.push();
                });
            }).start();
        });
        mainLayout.add(button);

        add(mainLayout);
    }

}
