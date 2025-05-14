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
package com.vaadin.flow.spring.test.routescope;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

@Route(value = "div-in-layout", layout = Layout.class)
@RouteScope
@RouteScopeOwner(Layout.class)
@Component
public class DivInLayout extends Div {

    @Autowired
    private ApplicationContext context;

    private Div serviceInfo;

    public DivInLayout() {
        // the component is in the route scope so the text should be the same
        // until route scope is active.
        Div div = createInfo("div-id");
        div.setText(UUID.randomUUID().toString());
        add(div);

        serviceInfo = createInfo("service-info");
        add(serviceInfo);
    }

    private Div createInfo(String id) {
        Div div = new Div();
        div.setId(id);
        div.getElement().getStyle().set("display", "block");
        return div;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        serviceInfo.setText(context.getBean(MyService.class).getValue());
    }

}
