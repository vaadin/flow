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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Route("com.vaadin.flow.uitest.ui.push.ManualLongPollingPushView")
public class ManualLongPollingPushView extends AbstractTestViewWithLog {

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.getPushConfiguration().setPushMode(PushMode.MANUAL);
        ui.getPushConfiguration().setTransport(Transport.LONG_POLLING);
        NativeButton manualPush = new NativeButton("Manual push after 1s",
                event -> {
                    executor.submit(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ui.access(() -> {
                            log("Logged after 1s, followed by manual push");
                            ui.push();
                        });
                    });
                });
        manualPush.setId("manaul-push");
        add(manualPush);

        manualPush = new NativeButton("Double manual push after 1s", event -> {
            executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ui.access(() -> {
                    log("First message logged after 1s, followed by manual push");
                    ui.push();
                    log("Second message logged after 1s, followed by manual push");
                    ui.push();
                });
            });
        });
        manualPush.setId("double-manual-push");
        add(manualPush);
    }

}
