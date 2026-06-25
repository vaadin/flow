/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
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
