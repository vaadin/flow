package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "com.vaadin.flow.uitest.ui.InertComponentView")
public class InertComponentView extends Div {

    private static int boxCounter;

    public InertComponentView() {
        add(new Box(false));
        // add(new RouterLink("Link to another view", ModalDialogView.class));
        add(new NativeButton("New box",
                event -> getUI().ifPresent(ui -> ui.add(new Box(false)))));
        add(new NativeButton("New Inert Box",
                event -> getUI().ifPresent(ui -> ui.addModal(new Box(false)))));
    }

    private static class Box extends Div {
        public Box(boolean inert) {
            add(new Text(boxCounter + " " + (inert ? "Inert" : "Not inert")
                    + " Box"));
            add(new NativeButton("Remove",
                    event -> getElement().removeFromParent()));
            add(new NativeButton("New box",
                    event -> getUI().ifPresent(ui -> ui.add(new Box(false)))));
            add(new NativeButton("New Inert Box", event -> getUI()
                    .ifPresent(ui -> ui.addModal(new Box(false)))));

            getStyle().set("border", "1px solid pink");
        }
    }
}
