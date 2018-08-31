package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ErrorHandlingView", layout = ViewTestLayout.class)
public class ErrorHandlingView extends AbstractDivView {
    public ErrorHandlingView() {
        NativeButton errorButton = new NativeButton(
                "Throw exception in click handler", e -> {
                    throw new IllegalStateException(
                            "Intentional fail in click handler");
                });
        errorButton.setId("errorButton");
        add(errorButton);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getSession().setErrorHandler(e -> {
            Div div = new Div(
                    new Text("An error occurred: " + e.getThrowable()));
            div.addClassName("error");
            add(div);
        });
    }

}
