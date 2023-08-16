package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("reload-test")
@JavaScript("./benchmark.js")
public class SpringDevToolsReloadView extends Div {

    public SpringDevToolsReloadView() {
        Span result = new Span();
        result.setId("result");

        NativeButton startTriggerButton = new NativeButton("Click to Start",
                event -> {
                    UI.getCurrent().getPage()
                            .executeJs("window.benchmark.start()");
                    Application.triggerReload();
                });
        startTriggerButton.setId("start-button");

        add(startTriggerButton, result);

        startTriggerButton.getElement()
                .addEventListener("componentready", event -> {

                    System.out.println("result: " + event.getEventData()
                            .getNumber("event.detail.result"));

                    result.setText(String.format(
                            "Reload time by class change was [%s] ms",
                            event.getEventData()
                                    .getNumber("event.detail.result")));

                }).addEventData("event.detail.result");

        UI.getCurrent().getPage().executeJs(
                "window.benchmark.measureRender($0);", startTriggerButton);
    }
}
