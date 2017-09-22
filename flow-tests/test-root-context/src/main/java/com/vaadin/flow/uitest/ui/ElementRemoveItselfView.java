package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.ui.html.Label;

public class ElementRemoveItselfView extends AbstractDivView {

    private Element layout = new Element("div");
    private Element button = new Element("button");

    public ElementRemoveItselfView() {
        button.setText("Remove me");
        button.setAttribute("id", "remove-me");

        layout.appendChild(button);
        button.addEventListener("click", evt -> {
            layout.removeAllChildren();
            Label label = new Label("All removed!");
            label.setId("all-removed");
            add(label);
        });
        getElement().appendChild(layout);
    }
}
