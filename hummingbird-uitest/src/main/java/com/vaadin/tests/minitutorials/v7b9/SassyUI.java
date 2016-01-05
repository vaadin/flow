package com.vaadin.tests.minitutorials.v7b9;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@StyleSheet("vaadin://themes/sassy/styles.css")
public class SassyUI extends UI {
    @Override
    public void init(VaadinRequest request) {
        Button b = new Button("Reindeer");
        ComponentContainer layout = new VerticalLayout();
        layout.addComponent(b);

        b = new Button("important");
        b.addStyleName("important");
        layout.addComponent(b);

        b = new Button("More important");
        b.setPrimaryStyleName("my-button");
        layout.addComponent(b);

        setContent(layout);
    }
}
