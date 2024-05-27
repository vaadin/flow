package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.mpr.LegacyWrapper;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class LegacyComponent extends Div {

    public LegacyComponent() {
        VerticalLayout legacyLayout = new VerticalLayout();
        TextField field = new TextField("My legacy field");
        Button legacyButton = new Button("legacy button", click -> {
            Notification.show("Hello from V8: " + field.getValue());
        });
        legacyLayout.addComponents(field, legacyButton);

        LegacyWrapper legacyWrapper = new LegacyWrapper(legacyLayout);
        add(legacyWrapper);
    }
}
