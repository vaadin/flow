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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

@Route(value = "", layout = MainView.class)
@RouteAlias("home")
@PageTitle("Public View")
@Menu(order = 1)
public class PublicView extends FlexLayout {

    public static final String BACKGROUND_NAVIGATION_ID = "backgroundNavi";
    public static final String ANCHOR_NAVIGATION_ID = "anchorNavi";
    public static final String ANCHOR_ALIAS_NAVIGATION_ID = "anchorAliasNavi";
    public static final String FORWARD_NAVIGATION_ID = "forwardNavi";
    public static final String REROUTE_NAVIGATION_ID = "rerouteNavi";

    public PublicView() {
        setFlexDirection(FlexDirection.COLUMN);
        setHeightFull();

        H1 header = new H1("Welcome to the Java Bank of Vaadin");
        header.setId("header");
        header.getStyle().set("text-align", "center");
        add(header);
        Image image = new Image("public/images/bank.jpg", "Bank");
        image.getStyle().set("max-width", "100%").set("min-height", "0");
        add(image);
        add(new Paragraph(
                "We are very great and have great amounts of money."));

        Button backgroundNavigation = new Button(
                "Navigate to admin view in 1 second", e -> {
                    UI ui = e.getSource().getUI().get();
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                        }
                        ui.access(() -> {
                            ui.navigate(AdminView.class);
                        });

                    }).start();
                });
        backgroundNavigation.setId(BACKGROUND_NAVIGATION_ID);

        Anchor linkToAdmin = new Anchor("/admin", "Anchor to admin");
        linkToAdmin.setId(ANCHOR_NAVIGATION_ID);
        Anchor linkToAdminAlias = new Anchor("/alias-for-admin",
                "Anchor to admin alias");
        linkToAdminAlias.setId(ANCHOR_ALIAS_NAVIGATION_ID);

        RouterLink forwardToAdmin = new RouterLink("Forward to admin",
                PassThroughView.class, new RouteParameters("type", "forward"));
        forwardToAdmin.setId(FORWARD_NAVIGATION_ID);
        RouterLink rerouteToAdmin = new RouterLink("Forward to admin",
                PassThroughView.class, new RouteParameters("type", "reroute"));
        rerouteToAdmin.setId(REROUTE_NAVIGATION_ID);

        HorizontalLayout additionalNavigation = new HorizontalLayout();
        additionalNavigation.add(backgroundNavigation);
        additionalNavigation.add(linkToAdmin);
        additionalNavigation.add(linkToAdminAlias);
        additionalNavigation.add(forwardToAdmin);
        additionalNavigation.add(rerouteToAdmin);
        add(additionalNavigation);
    }

}
