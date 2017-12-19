/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.demo.views;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcons;
import com.vaadin.flow.component.orderedlayout.FlexLayout.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.flow.router.Route;

/**
 * View for {@link Icon} demo.
 */
@Route(value = "icon", layout = MainLayout.class)
@ComponentDemo(name = "Icon")
@StyleSheet("styles.css")
public class IconView extends DemoView {

    @Override
    protected void initView() {
        createBasicIconsView();
        createStyledIconView();
        createAllIconsView();
    }

    private void createBasicIconsView() {
        // begin-source-example
        // source-example-heading: Two ways to create a new icon
        Icon edit = new Icon(VaadinIcons.EDIT);
        Icon close = VaadinIcons.CLOSE.create();
        // end-source-example

        edit.getStyle().set("marginRight", "5px");
        addCard("Two ways to create a new icon",
                new HorizontalLayout(edit, close));

        edit.setId("edit-icon");
        close.setId("close-icon");
    }

    private void createStyledIconView() {
        // begin-source-example
        // source-example-heading: Styling an icon
        Icon logo = new Icon(VaadinIcons.VAADIN_H);
        logo.setSize("100px");
        logo.setColor("orange");
        // end-source-example

        addCard("Styling an icon", logo);

        logo.setId("logo-icon");
    }

    private void createAllIconsView() {
        HorizontalLayout iconLayout = new HorizontalLayout();
        iconLayout.addClassName("all-icons-layout");
        iconLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        for (VaadinIcons icon : VaadinIcons.values()) {
            VerticalLayout iconWithName = new VerticalLayout(icon.create(),
                    new Label(icon.name()));
            iconWithName.setSizeUndefined();
            iconWithName
                    .setDefaultHorizontalComponentAlignment(Alignment.CENTER);
            iconWithName.addClassName("icon-with-name");
            iconLayout.add(iconWithName);
        }

        addCard("All available icons", iconLayout);
    }
}
