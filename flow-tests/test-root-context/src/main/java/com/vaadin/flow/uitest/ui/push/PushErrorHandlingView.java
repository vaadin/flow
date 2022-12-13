package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.communication.PushMode;

@Route("com.vaadin.flow.uitest.ui.push.PushErrorHandlingView")
public class PushErrorHandlingView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        VaadinSession.getCurrent().setErrorHandler(event -> {
            Div div = new Div();
            div.addClassName("error");
            div.setText("An error! " + event.getThrowable().getClass());
            add(div);
        });

        final NativeButton button = new NativeButton("Click for NPE!",
                event -> {
                    ((String) null).length(); // Null-pointer exception
                });
        button.setId("npeButton");
        add(button);

    }

}
