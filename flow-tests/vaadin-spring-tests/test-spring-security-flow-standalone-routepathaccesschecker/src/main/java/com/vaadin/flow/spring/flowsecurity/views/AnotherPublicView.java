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
package com.vaadin.flow.spring.flowsecurity.views;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

@Route(value = "another", layout = MainView.class)
@RouteAlias("hey/:name/welcome/:wild*")
@PageTitle("Another Public View")
public class AnotherPublicView extends FlexLayout
        implements BeforeEnterObserver {

    private final Span name;
    private final Span wild;

    public AnotherPublicView() {
        setFlexDirection(FlexDirection.COLUMN);
        setHeightFull();

        H1 header = new H1("Another public view for testing");
        header.setId("header");
        header.getStyle().set("text-align", "center");
        add(header);
        add(new Anchor("hey/anchor/welcome/home", "Link to alias"));

        name = new Span();
        name.setId("p-name");

        wild = new Span();
        wild.setId("p-wild");
        add(name, wild);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        name.setText(event.getRouteParameters().get("name").orElse("-"));
        wild.setText(event.getRouteParameters().get("wild").orElse("-"));
    }

}
