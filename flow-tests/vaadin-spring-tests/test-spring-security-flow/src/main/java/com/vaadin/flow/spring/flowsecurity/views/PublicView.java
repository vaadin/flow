/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecurity.views;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "", layout = MainView.class)
@PageTitle("Public View")
@AnonymousAllowed
public class PublicView extends FlexLayout {

    public static final String BACKGROUND_NAVIGATION_ID = "backgroundNavi";

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
                    Runnable doNavigation = new DelegatingSecurityContextRunnable(
                            () -> ui.navigate(AdminView.class),
                            SecurityContextHolder.getContext());
                    Runnable navigateToAdmin = () -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                        }
                        ui.access(doNavigation::run);
                    };
                    new Thread(navigateToAdmin).start();
                });
        backgroundNavigation.setId(BACKGROUND_NAVIGATION_ID);
        add(backgroundNavigation);
    }

}
