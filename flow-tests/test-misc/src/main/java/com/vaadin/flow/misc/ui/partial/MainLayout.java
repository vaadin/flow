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

package com.vaadin.flow.misc.ui.partial;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;

@ParentLayout(RootLayout.class)
public class MainLayout extends Div implements RouterLayout {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String RESET_ID = "reset-log";

    private static int eventCounter = 0;

    private final Div log = new Div();

    public MainLayout() {
        log.setText(++eventCounter + ": " + getClass().getSimpleName()
                + ": constructor");
        log.setId(EVENT_LOG_ID);
        NativeButton reset = new NativeButton("Reset count",
                e -> eventCounter = 0);
        reset.setId(RESET_ID);
        add(log, reset);
    }
}
