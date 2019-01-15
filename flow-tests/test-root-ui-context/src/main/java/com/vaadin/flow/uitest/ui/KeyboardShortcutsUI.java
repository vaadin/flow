package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.server.VaadinRequest;

public class KeyboardShortcutsUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);

        Div parent = new Div();
        parent.getElement().setAttribute("style",
                "width:80%; height:80%; background-color:lightblue; ");

        embed(parent, this, "1").withAlt().on('F').preventDefault();

        Div focusableParent = new Div();
        focusableParent.getElement().setAttribute("style",
                "width:100%; height:50%; background-color:lightgreen; ");
        // ComponentUtil.makeFocusable(focusableParent);

        embed(focusableParent, parent, "2").withAlt().on('F').preventDefault()
                .stopPropagation();

        Div focusableParent2 = new Div();
        focusableParent2.getElement().setAttribute("style",
                "width:100%; height:50%; background-color:pink; ");
//        ComponentUtil.makeFocusable(focusableParent2);

        embed(focusableParent2, focusableParent, "3").withAlt().on('F');

        Shortcuts.exec(this::crudView).on('A').stopPropagation()
                .preventDefault();
    }

    private void crudView() {
        System.out.println("!!! crudView");
    }

    private ShortcutRegistration embed(Div div, HasComponents parent,
                                       String tag) {
        Label label = new Label();
        Input input = new Input();

        div.add(input);
        div.add(label);

        parent.add(new Hr());
        parent.add(div);

        ComponentUtil.addListener(div, KeyDownEvent.class, event -> {
            String message = tag + ": " + event.getKey().getKeys().get(0) +
                    ", " + event.getModifiers();
            System.out.println(message);
            label.setText(message);
        });

        return Shortcuts.addShortcut(
                () -> System.out.println(
                        "Shortcut for tag " + tag + " invoked!"),
                div);
    }
}

