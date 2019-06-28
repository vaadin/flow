package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.KeyDownEvent;
import com.vaadin.flow.component.KeyUpEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.KeyboardEventView",
        layout = ViewTestLayout.class)
public class KeyboardEventView extends Div {
    private Input input = new Input();
    private NativeButton sendInvalidKeyUp = new NativeButton();

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

        Paragraph keyUpParagraph = new Paragraph();
        keyUpParagraph.setId("keyUpParagraph");
        ComponentUtil.addListener(input, KeyUpEvent.class,
                event -> keyUpParagraph
                        .setText(String.join(",", event.getKey().getKeys())));

        sendInvalidKeyUp.setId("sendInvalidKeyUp");
        sendInvalidKeyUp.addClickListener(event -> {
            getUI().ifPresent(ui -> ui.getPage().executeJs(
                    "$0.dispatchEvent(new KeyboardEvent('keyup', {}))",
                    input.getElement()));
        });
        add(sendInvalidKeyUp, keyUpParagraph);
        UI.getCurrent().getSession().setErrorHandler(event -> keyUpParagraph
                .setText(event.getThrowable().getMessage()));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        UI.getCurrent().getSession().setErrorHandler(new DefaultErrorHandler());
    }
}
