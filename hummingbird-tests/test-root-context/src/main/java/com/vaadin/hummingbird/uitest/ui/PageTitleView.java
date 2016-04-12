package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Input;
import com.vaadin.hummingbird.router.LocationChangeEvent;

public class PageTitleView extends AbstractDivView {

    @Override
    public void onLocationChange(LocationChangeEvent event) {

        Input input = new Input();
        input.setId("input");
        input.setValue("");

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
