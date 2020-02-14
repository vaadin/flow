/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "view-with-server-view-button", layout = MainLayout.class)
public class ViewWithServerViewButton extends Div {
    public ViewWithServerViewButton() {
        setId("viewWithServerViewButton");
        NativeButton serverViewButton = new NativeButton("Server view",
                e -> UI.getCurrent().navigate("serverview"));
        serverViewButton.setId("serverViewButton");
        add(serverViewButton);
    }
}
