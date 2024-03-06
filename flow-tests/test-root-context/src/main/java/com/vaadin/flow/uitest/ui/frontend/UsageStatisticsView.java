/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.frontend;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.template.HiddenTemplateView;

// Devmode detector detects bundling based on whether polymer-element.html is loaded
@NpmPackage(value = "@vaadin/vaadin-development-mode-detector", version = "1.1.0")
@JsModule("@vaadin/vaadin-development-mode-detector/vaadin-development-mode-detector.js")
@Route(value = "com.vaadin.flow.uitest.ui.frontend.UsageStatisticsView", layout = ViewTestLayout.class)
public class UsageStatisticsView extends Div {
    public UsageStatisticsView() {
        NativeButton print = new NativeButton(
                "Print usage statistics to the console", e -> {
                    getUI().get().getPage().executeJs(
                            "var basket = localStorage.getItem('vaadin.statistics.basket'); if (basket) basket = JSON.parse(basket); console.log(basket)");
                });
        NativeButton clear = new NativeButton("Clear usage statistics", e -> {
            getUI().get().getPage().executeJs(
                    "localStorage.removeItem('vaadin.statistics.basket')");
        });
        NativeButton push = new NativeButton("Enable push", e -> getUI().get()
                .getPushConfiguration().setPushMode(PushMode.AUTOMATIC));
        NativeButton template = new NativeButton("Use PolymerTemplate", e -> {
            add(new HiddenTemplateView());
        });

        add(new Text(
                "View for manually testing usage statistics gathering for Flow features."
                        + " After a feature has been used, the page should be reloaded before verifying that usage info has been gathered."),
                new Div(print, clear, push, template));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        attachEvent.getUI().getPage().executeJs(
                "window.Vaadin.runIfDevelopmentMode('vaadin-usage-statistics');");
    }
}
