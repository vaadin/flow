/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.EventListenersView", layout = ViewTestLayout.class)
public class EventListenersView extends AbstractDivView {

    @Override
    protected void onShow() {
        AtomicInteger count = new AtomicInteger();
        NativeButton button = new NativeButton("Click me");
        button.setId("click");
        button.addClickListener(evt -> {
            int value = count.incrementAndGet();
            Label label = new Label(String.valueOf(value));
            label.addClassName("count");
            add(label);
            add(button);
        });
        add(button);
    }

}
