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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Sends messages containing the | character, which Atmosphere uses as the
 * message delimiter, both as responses and as pushes from the server.
 */
@CustomPush
@Route("com.vaadin.flow.uitest.ui.push.BarInUidlView")
public class BarInUidlView extends AbstractTestViewWithLog {

    public static final String CLICKED_TEXT = "Thank you for clicking | bar";
    public static final String PUSHED_TEXT = "Pushed | bar";

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();

        NativeButton click = new NativeButton("Click Me",
                event -> addMessage(CLICKED_TEXT));
        click.setId("click");

        NativeButton push = new NativeButton("Push",
                event -> new Thread(
                        () -> ui.access(() -> addMessage(PUSHED_TEXT)))
                        .start());
        push.setId("push");

        add(click, push);
    }

    private void addMessage(String text) {
        Div message = new Div(text);
        message.addClassName("message");
        add(message);
    }
}
