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
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@CustomPush
@Route("com.vaadin.flow.uitest.ui.push.MakeComponentVisibleWithPushView")
public class MakeComponentVisibleWithPushView extends Div {

    private Div rootLayout;
    private Input input;
    private SearchThread searchThread;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        /*
         * Read push settings from the UI instead of the the navigation target /
         * router layout to preserve the structure of these legacy testing UIs
         */
        CustomPush push = getClass().getAnnotation(CustomPush.class);
        UI ui = attachEvent.getUI();
        ui.getPushConfiguration().setPushMode(push.value());
        ui.getPushConfiguration().setTransport(push.transport());

        rootLayout = new Div();
        add(rootLayout);

        input = new Input();
        input.setVisible(false);
        input.setValue("foo");
        input.setId("input");
        rootLayout.add(input);

        NativeButton doUpdateButton = new NativeButton("Do Update",
                event -> doUpdate());
        doUpdateButton.setId("update");

        rootLayout.add(doUpdateButton);
    }

    private void doUpdate() {

        cancelSuggestThread();

        input.setVisible(false);

        UI ui = UI.getCurrent();
        searchThread = new SearchThread(ui);
        searchThread.start();
    }

    class SearchThread extends Thread {
        private UI ui;

        public SearchThread(UI ui) {
            this.ui = ui;
        }

        @Override
        public void run() {

            if (!searchThread.isInterrupted()) {
                ui.access(() -> {
                    input.setValue(input.getValue() + "bar");
                    input.setVisible(true);
                });
            }
        }

    }

    private void cancelSuggestThread() {

        if ((searchThread != null) && !searchThread.isInterrupted()) {
            searchThread.interrupt();
            searchThread = null;
        }
    }

}
