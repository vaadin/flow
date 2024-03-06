/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.LoadingIndicatorView", layout = ViewTestLayout.class)
public class LoadingIndicatorView extends AbstractDivView {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        NativeButton disableButton = new NativeButton(
                "Disable default loading indicator theme and add custom");
        disableButton.setId("disable-theme");
        disableButton.addClickListener(clickEvent -> {
            clickEvent.getSource().getUI().get()
                    .getLoadingIndicatorConfiguration()
                    .setApplyDefaultTheme(false);
            clickEvent.getSource().getUI().get().getPage()
                    .addStyleSheet("/loading-indicator.css");
        });
        add(disableButton);
        add(divWithText("First delay: "
                + getLoadingIndicatorConfiguration().getFirstDelay()));
        add(divWithText("Second delay: "
                + getLoadingIndicatorConfiguration().getSecondDelay()));
        add(divWithText("Third delay: "
                + getLoadingIndicatorConfiguration().getThirdDelay()));

        int[] delays = new int[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
        for (int delay : delays) {
            add(createButton("Trigger event which takes " + delay + "ms",
                    "wait" + delay, e -> delay(delay)));
        }
    }

    private static Div divWithText(String text) {
        Div div = new Div();
        div.setText(text);
        return div;
    }

    private LoadingIndicatorConfiguration getLoadingIndicatorConfiguration() {
        return UI.getCurrent().getLoadingIndicatorConfiguration();
    }

    private void delay(int timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (InterruptedException e) {
        }
    }

}
