package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.KeyDownEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.KeyboardEventView",
        layout = ViewTestLayout.class)
public class KeyboardEventView extends Div {
    private Input input = new Input();

    public KeyboardEventView() {
        input.setId("input");
        Paragraph paragraph = new Paragraph();
        paragraph.setId("paragraph");

        ComponentUtil.addListener(input, KeyDownEvent.class, event -> {
            /*
                for each event, sets a string "keyvalue:codevalue;" to the
                paragraph. For 'Q' the string would be
                    "Q:KeyQ"
             */
            String keyText = String.join(",", event.getKey().getKeys());
            String codeText = (event.getCode().isPresent() ?
                    String.join(",", event.getCode().get().getKeys()) :
                    "");
            paragraph.setText(keyText + ":" + codeText);
        });

        add(input, paragraph);
    }
}
