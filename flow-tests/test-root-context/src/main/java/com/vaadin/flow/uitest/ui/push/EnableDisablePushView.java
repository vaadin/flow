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

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;

@Route("com.vaadin.flow.uitest.ui.push.EnableDisablePushView")
public class EnableDisablePushView extends AbstractTestViewWithLog {

    private int c = 0;

    private final Timer timer = new Timer(true);

    private UI ui;

    private final class CounterTask extends TimerTask {

        @Override
        public void run() {

            try {
                while (true) {
                    TimeUnit.MILLISECONDS.sleep(500);

                    ui.access(() -> {
                        log("Counter = " + c++);
                        if (c == 3) {
                            log("Disabling polling, enabling push");
                            ui.getPushConfiguration()
                                    .setPushMode(PushMode.AUTOMATIC);
                            ui.setPollInterval(-1);
                            log("Polling disabled, push enabled");
                        }
                    });
                    if (c == 3) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
            } catch (UIDetachedException e) {
            }
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = getUI().get();

        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        log("Push enabled");

        add(createButton("Disable push", "disable-push", () -> {
            log("Disabling push");
            ui.getPushConfiguration().setPushMode(PushMode.DISABLED);
            log("Push disabled");
        }));

        add(createButton("Enable push", "enable-push", () -> {
            log("Enabling push");
            ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
            log("Push enabled");
        }));

        add(createButton("Disable polling", "disable-polling", () -> {
            log("Disabling poll");
            ui.setPollInterval(-1);
            log("Poll disabled");
        }));

        add(createButton("Enable polling", "enable-polling", () -> {
            log("Enabling poll");
            ui.setPollInterval(1000);
            log("Poll enabled");
        }));

        add(createButton("Disable push, re-enable from background thread",
                "thread-re-enable-push", () -> {
                    log("Disabling push, enabling polling");
                    ui.getPushConfiguration().setPushMode(PushMode.DISABLED);
                    ui.setPollInterval(1000);
                    timer.schedule(new CounterTask(), new Date());
                    log("Push disabled, polling enabled");
                }));

    }

    private NativeButton createButton(String caption, String id,
            Runnable action) {
        NativeButton button = new NativeButton(caption);
        button.setId(id);
        button.addClickListener(event -> action.run());
        return button;
    }

}
