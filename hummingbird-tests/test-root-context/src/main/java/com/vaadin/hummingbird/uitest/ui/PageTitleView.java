package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.hummingbird.uitest.component.Div;
import com.vaadin.hummingbird.uitest.component.Input;

public class PageTitleView extends AbstractDivView {

    @Override
    public void onLocationChange(LocationChangeEvent event) {

        Input input = new Input();
        input.setId("input");
        input.setValue("");

        Div updateButton = new Div().setId("button")
                .setText("Update page title");
        updateButton.getElement().addEventListener("click", e -> {
            getPage().setTitle(input.getValue());
        });

        Div overrideButton = new Div().setId("override")
                .setText("Triggers two updates");
        overrideButton.getElement().addEventListener("click", e -> {
            getPage().setTitle(input.getValue());
            getPage().setTitle("OVERRIDDEN");
        });

        addComponent(input, updateButton, overrideButton);
    }

}
