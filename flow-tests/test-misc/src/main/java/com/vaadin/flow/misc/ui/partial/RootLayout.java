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
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLayout;

@PreserveOnRefresh(partialMatch = true)
public class RootLayout extends Div implements RouterLayout {

    public static final String ROOT_EVENT_LOG_ID = "root-event-log";
    public static final String ROOT_RESET_ID = "root-reset-log";

    private static int eventCounter = 0;

    private final Div log = new Div();

    public RootLayout() {
        log.setText(++eventCounter + ": " + getClass().getSimpleName()
                + ": constructor");
        log.setId(ROOT_EVENT_LOG_ID);
        NativeButton reset = new NativeButton("Reset count",
                e -> eventCounter = 0);
        reset.setId(ROOT_RESET_ID);
        add(log, reset);
    }
}
