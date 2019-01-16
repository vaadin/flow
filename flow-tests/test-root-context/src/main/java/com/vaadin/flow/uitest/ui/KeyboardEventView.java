package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.KeyDownEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.KeyboardEventView", layout = ViewTestLayout.class)
public class KeyboardEventView extends Div {
    private Input input = new Input();

    public KeyboardEventView() {
        input.setId("input");
        Paragraph paragraph = new Paragraph();
        paragraph.setId("paragraph");

        ComponentUtil.addListener(input, KeyDownEvent.class, event -> {
            /*
                for each event, adds a string "keyvalue:codevalue;" to the
                paragraph. For 'Q' the string would be
                    "Q:KeyQ;"
             */
            String keyText = String.join(",", event.getKey().getKeys());
            String codeText = (event.getCode().isPresent() ?
                    String.join(",", event.getCode().get().getKeys()) :
                    "");
            String str = paragraph.getText();
            String full = str + keyText + ":" + codeText + ";";
            paragraph.setText(full);
            System.out.println("Set <p> to " + full);
        });

        add(input, paragraph);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        input.focus();

        Page page = UI.getCurrent().getPage();

        page.executeJavaScript("var element = document.getElementById(\"input\");" +
                // dispatch keyboard event with code
                "var e1 = new KeyboardEvent(\"keydown\", {bubbles : true, " +
                    "cancelable : true, key : \"Q\", char : \"Q\", shiftKey : " +
                    "true, code: \"KeyQ\"});" +
                "element.dispatchEvent(e1);" +
                // dispatch keyboard event without code
                "var e2 = new KeyboardEvent(\"keydown\", {bubbles : true, " +
                    "cancelable : true, key : \"Q\", char : \"Q\", " +
                    "shiftKey : true});" +
                "element.dispatchEvent(e2);" +
                // dispatch keyboard event with empty code
                "var e2 = new KeyboardEvent(\"keydown\", {bubbles : true, " +
                "cancelable : true, key : \"Q\", char : \"Q\", " +
                "shiftKey : true, code: \"\"});" +
                "element.dispatchEvent(e2);");
    }
}
