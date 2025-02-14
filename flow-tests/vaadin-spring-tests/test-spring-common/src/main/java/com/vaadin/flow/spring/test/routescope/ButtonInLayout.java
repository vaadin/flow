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
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

@Route(value = "button-in-layout", layout = Layout.class)
public class ButtonInLayout extends Div {

    private final ApplicationContext context;

    private final String componentId = UUID.randomUUID().toString();

    private final Div buttonScopedBeanCount;

    public ButtonInLayout(
            @Autowired @RouteScopeOwner(Layout.class) MyService service,
            @Autowired ApplicationContext context) {
        this.context = context;
        // the text should be preserved inside route scope
        NativeButton button = new NativeButton(service.getValue());
        button.setId("route-service");
        add(button);

        // the component id should be changed every time when component is
        // created
        Div div = new Div();
        div.setId("component-id");
        div.setText(componentId);
        add(div);

        div = new Div();
        div.setId("no-owner-bean");
        div.setText(context.getBean(NoOwnerBean.class).getValue());
        add(div);

        buttonScopedBeanCount = new Div();
        buttonScopedBeanCount.setId("button-scoped-bean-count");
        add(buttonScopedBeanCount);
    }

    @Override
    public void add(Component... components) {
        Stream.of(components).forEach(
                comp -> comp.getElement().getStyle().set("display", "block"));
        super.add(components);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        buttonScopedBeanCount.setText(String.valueOf(
                context.getBeansOfType(ButtonScopedBean.class).size()));
    }

}
