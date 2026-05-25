/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "", layout = MainView.class)
@PageTitle("Public View")
@AnonymousAllowed
public class PublicView extends Div {

    public static final String BACKGROUND_NAVIGATION_ID = "backgroundNavi";

    public PublicView() {
        getStyle().set("display", "flex").set("flex-direction", "column")
                .set("height", "100%");

        H1 header = new H1("Welcome to the Java Bank of Vaadin");
        header.setId("header");
        header.getStyle().set("text-align", "center");
        add(header);
        Image image = new Image("public/images/bank.jpg", "Bank");
        image.getStyle().set("max-width", "100%").set("min-height", "0");
        add(image);
        add(new Paragraph(
                "We are very great and have great amounts of money."));

        NativeButton backgroundNavigation = new NativeButton(
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
        add(backgroundNavigation);
    }

}
