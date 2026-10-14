/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("reload-nativebutton-test")
@JavaScript("./benchmark.js")
public class SpringDevToolsNativeButtonReloadView extends Div {

    public SpringDevToolsNativeButtonReloadView() {
        Span result = new Span();
        result.setId("result");
        result.setVisible(false);

        NativeButton startTriggerButton = SpringDevToolsReloadUtils
                .createReloadTriggerButton();

        add(startTriggerButton, result);

        startTriggerButton.getElement()
                .addEventListener("componentready", event -> {

                    System.out.println("result: " + event.getEventData()
                            .getNumber("event.detail.result"));

                    result.setText(String.format(
                            "Reload time by class change was [%s] ms",
                            event.getEventData()
                                    .getNumber("event.detail.result")));
                    result.setVisible(true);

                }).addEventData("event.detail.result");

        UI.getCurrent().getPage().executeJs(
                "window.benchmark.measureRender($0);", startTriggerButton);
    }
}
