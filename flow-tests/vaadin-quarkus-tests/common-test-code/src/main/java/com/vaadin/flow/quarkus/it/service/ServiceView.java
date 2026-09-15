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
package com.vaadin.flow.quarkus.it.service;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("service")
public class ServiceView extends Div {

    public static final String EXPIRE = "EXPIRE";
    public static final String ACTION = "ACTION";
    public static final String FAIL = "FAIL";

    @Inject
    ServiceBean bean;

    @PostConstruct
    private void init() {
        NativeButton expireBtn = new NativeButton("expire",
                event -> VaadinSession.getCurrent().getSession().invalidate());
        expireBtn.setId(EXPIRE);

        NativeButton actionButton = new NativeButton("an action", event -> {
        });
        actionButton.setId(ACTION);

        NativeButton failBtn = new NativeButton("fail", event -> {
            if (true) {
                throw new NullPointerException();
            }
        });
        failBtn.setId(FAIL);

        add(expireBtn, actionButton, failBtn);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Div div = new Div();
        div.setId("service-id");
        div.setText(bean.getId());
        add(div);
    }
}
