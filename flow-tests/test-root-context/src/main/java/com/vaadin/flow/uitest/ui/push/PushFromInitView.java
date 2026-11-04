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
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Pushes from background threads while the view is being initialized and right
 * after it, which must not cause race conditions with the new connection.
 */
@CustomPush
@Route("com.vaadin.flow.uitest.ui.push.PushFromInitView")
public class PushFromInitView extends AbstractTestViewWithLog {

    public static final String LOG_DURING_INIT = "Logged from access run before init ends";
    public static final String LOG_AFTER_INIT = "Logged from background thread run after init has finished";

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();

        log("Logged in init");
        Thread beforeInitEnds = new Thread(
                () -> ui.access(() -> log(LOG_DURING_INIT)));
        beforeInitEnds.start();
        try {
            beforeInitEnds.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            ui.access(() -> log(LOG_AFTER_INIT));
        }).start();

        add(new NativeButton("Sync"));
    }
}
