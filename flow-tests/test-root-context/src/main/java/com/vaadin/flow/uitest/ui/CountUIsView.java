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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.CountUIsView")
public class CountUIsView extends Div {

    public CountUIsView() {
        Div count = new Div();
        add(count);
        count.setId("uis");

        // Don't show the UIs number right away on the component CTOR. Make it
        // explicit via action. At this point all UIs should be already
        // initialized
        NativeButton showUisNumber = new NativeButton("Show created UIs number",
                event -> count.setText(String.valueOf(
                        TestingServiceInitListener.getNotNavigatedUis())));
        add(showUisNumber);
    }
}
