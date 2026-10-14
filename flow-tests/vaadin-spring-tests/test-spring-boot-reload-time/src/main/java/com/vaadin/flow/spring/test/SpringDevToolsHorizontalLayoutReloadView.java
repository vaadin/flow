/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("reload-layout-test")
@JavaScript("./benchmark.js")
public class SpringDevToolsHorizontalLayoutReloadView extends Div {

    public SpringDevToolsHorizontalLayoutReloadView() {
        Span result = new Span();
        result.setId("result");
        result.setVisible(false);

        NativeButton startTriggerButton = SpringDevToolsReloadUtils
                .createReloadTriggerButton();

        HorizontalLayout layout = new HorizontalLayout(
                new Button("Vaadin Button"), new TextField());
        add(startTriggerButton, result);
        add(new Html("<br/>"));
        add(layout);

        layout.getElement().addEventListener("componentready", event -> {

            System.out.println("result: "
                    + event.getEventData().getNumber("event.detail.result"));

            result.setText(String.format(
                    "Reload time by class change was [%s] ms",
                    event.getEventData().getNumber("event.detail.result")));
            result.setVisible(true);

        }).addEventData("event.detail.result");

        UI.getCurrent().getPage()
                .executeJs("window.benchmark.measureRender($0);", layout);
    }
}
