/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;

@Push
@Route("com.vaadin.flow.uitest.ui.PushFallbackView")
public class PushFallbackView extends Div {

    public static final String CONTAINER_ID = "push-fallback-container-id";
    public static final String PUSH_BUTTON_ID = "push-fallback-button-id";
    public static final int MESSAGE_COUNT = 10;

    public PushFallbackView() {
        Div pushContainer = new Div();
        pushContainer.setId(CONTAINER_ID);

        NativeButton pushButton = new NativeButton("Push with long-polling", click -> {
            Runnable runnable = () -> {
                for (int i = 0; i < MESSAGE_COUNT; i++) {
                    final int counter = i;
                    click.getSource().getUI().ifPresent(
                            ui -> ui.access(
                                    () -> {
                                        Div div = new Div();
                                        div.setText("Push message " + counter);
                                        pushContainer.add(div);
                                    }));
                }
            };
            new Thread(runnable).start();
        });
        pushButton.setId(PUSH_BUTTON_ID);
        add(pushButton, pushContainer);
    }
}
