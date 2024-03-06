/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ExtendedClientDetailsView", layout = ViewTestLayout.class)
public class ExtendedClientDetailsView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div screenWidth = createDiv("sw");
        Div screenHeight = createDiv("sh");
        Div windowInnerWidth = createDiv("ww");
        Div windowInnerHeight = createDiv("wh");
        Div bodyElementWidth = createDiv("bw");
        Div bodyElementHeight = createDiv("bh");
        Div devicePixelRatio = createDiv("pr");
        Div touchDevice = createDiv("td");

        // the sizing values cannot be set with JS but pixel ratio and touch
        // support can be faked
        NativeButton setValuesButton = new NativeButton("Set test values",
                event -> {
                    getUI().ifPresent(ui -> ui.getPage()
                            .executeJs("{" + "window.devicePixelRatio = 2.0;"
                                    + "navigator.msMaxTouchPoints = 1;" + "}"));
                });
        setValuesButton.setId("set-values");

        NativeButton fetchDetailsButton = new NativeButton(
                "Fetch client details", event -> {
                    getUI().ifPresent(ui -> ui.getPage()
                            .retrieveExtendedClientDetails(details -> {
                                screenWidth
                                        .setText("" + details.getScreenWidth());
                                screenHeight.setText(
                                        "" + details.getScreenHeight());
                                windowInnerWidth.setText(
                                        "" + details.getWindowInnerWidth());
                                windowInnerHeight.setText(
                                        "" + details.getWindowInnerHeight());
                                bodyElementWidth.setText(
                                        "" + details.getBodyClientWidth());
                                bodyElementHeight.setText(
                                        "" + details.getBodyClientHeight());
                                devicePixelRatio.setText(
                                        "" + details.getDevicePixelRatio());
                                touchDevice
                                        .setText("" + details.isTouchDevice());
                            }));
                });
        fetchDetailsButton.setId("fetch-values");

        add(setValuesButton, fetchDetailsButton);
    }

    private Div createDiv(String id) {
        Div div = new Div();
        div.setId(id);
        add(div);
        return div;
    }
}
