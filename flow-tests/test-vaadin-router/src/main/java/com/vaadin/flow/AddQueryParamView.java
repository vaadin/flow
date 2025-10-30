/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.flow;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.AddQueryParamView")
public class AddQueryParamView extends Div {

    public static final String PARAM_BUTTON_ID = "setParameter";
    public static final String QUERY_ID = "query";

    public AddQueryParamView() {
        NativeButton button = new NativeButton("Add URL Parameter", e -> {
            updateUrlRequestParameter("test", "HELLO!");
        });
        button.setId(PARAM_BUTTON_ID);
        add(button);
    }

    public void updateUrlRequestParameter(String key, String value) {
        Page page = UI.getCurrent().getPage();
        page.fetchCurrentURL(url -> {
            String newLocation = url + "?" + key + "=" + value;
            page.getHistory().replaceState(null, newLocation);
            Div div = new Div(newLocation);
            div.setId(QUERY_ID);
            add(div);
        });
    }
}
