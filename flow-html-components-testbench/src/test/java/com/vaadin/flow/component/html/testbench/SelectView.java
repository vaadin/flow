package com.vaadin.flow.component.html.testbench;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

@Route("Select")
public class SelectView extends Div {

    public SelectView() {
        Div log = new Div();
        log.setId("log");

        Element select = new Element("select");
        for (int i = 1; i < 10; i++) {
            select.appendChild(
                    new Element("option").setAttribute("id", "id" + i)
                            .setAttribute("value", "value" + i)
                            .setText("Visible text " + i));
        }
        select.setAttribute("id", "input");
        select.addEventListener("change", e -> {
            log.setText("Value is '"
                    + e.getEventData().get("element.value").asText() + "'");
        }).synchronizeProperty("element.value");
        add(log);
        getElement().appendChild(select);
    }
}
