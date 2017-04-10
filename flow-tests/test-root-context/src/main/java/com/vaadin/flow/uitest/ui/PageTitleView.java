package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.Input;
import com.vaadin.flow.router.LocationChangeEvent;

public class PageTitleView extends AbstractDivView {

    @Override
    public void onLocationChange(LocationChangeEvent event) {

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
