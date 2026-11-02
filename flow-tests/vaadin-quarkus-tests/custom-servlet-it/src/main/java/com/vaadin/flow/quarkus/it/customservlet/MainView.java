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
package com.vaadin.flow.quarkus.it.customservlet;

import java.util.concurrent.CompletableFuture;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * A view with one server round-trip and one Push update.
 */
@Route("")
public class MainView extends Div {

    public static final String CLICK_BUTTON_ID = "click";
    public static final String CLICK_RESULT_ID = "click-result";
    public static final String CLICKED_TEXT = "clicked";

    public static final String PUSH_BUTTON_ID = "push";
    public static final String PUSH_RESULT_ID = "push-result";
    public static final String PUSHED_TEXT = "pushed";

    public MainView() {
        Span clickResult = new Span();
        clickResult.setId(CLICK_RESULT_ID);
        NativeButton click = new NativeButton("Click",
                event -> clickResult.setText(CLICKED_TEXT));
        click.setId(CLICK_BUTTON_ID);

        Span pushResult = new Span();
        pushResult.setId(PUSH_RESULT_ID);
        NativeButton push = new NativeButton("Push", event -> {
            UI ui = event.getSource().getUI().orElseThrow();
            // Update the UI outside of the request, so the change can only
            // reach the browser through Push.
            CompletableFuture.runAsync(
                    () -> ui.access(() -> pushResult.setText(PUSHED_TEXT)));
        });
        push.setId(PUSH_BUTTON_ID);

        add(new Div(click, clickResult), new Div(push, pushResult));
    }
}
