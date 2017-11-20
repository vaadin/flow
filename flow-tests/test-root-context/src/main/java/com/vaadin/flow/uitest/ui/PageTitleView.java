package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.html.Input;

@Route(value = "com.vaadin.flow.uitest.ui.PageTitleView", layout = ViewTestLayout.class)
public class PageTitleView extends AbstractDivView {

    @Override
    public void beforeNavigation(BeforeNavigationEvent event) {

        Input input = new Input();
        input.setId("input");
        input.clear();

        Div updateButton = new Div();
        updateButton.setId("button");
        updateButton.setText("Update page title");
        updateButton.addClickListener(e -> {
            getPage().setTitle(input.getValue());
        });

        Div overrideButton = new Div();
        overrideButton.setId("override");
        overrideButton.setText("Triggers two updates");
        overrideButton.addClickListener(e -> {
            getPage().setTitle(input.getValue());
            getPage().setTitle("OVERRIDDEN");
        });

        add(input, updateButton, overrideButton);
    }

}
