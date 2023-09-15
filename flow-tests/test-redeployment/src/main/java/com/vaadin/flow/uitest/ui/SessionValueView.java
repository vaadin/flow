package com.vaadin.flow.uitest.ui;

import java.util.UUID;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

@Route(value = "com.vaadin.flow.uitest.ui.SessionValueView")
public class SessionValueView extends AbstractReloadView {

    // Test that session values outside VaadinSession are preserved on reload
    public SessionValueView() {

        addTriggerButton();

        WrappedSession session = VaadinSession.getCurrent().getSession();
        String customAttribute = (String) session
                .getAttribute("custom-attribute");
        if (customAttribute == null) {
            customAttribute = UUID.randomUUID().toString();
            session.setAttribute("custom-attribute", customAttribute);
        }
        Div div = new Div();
        div.setId("customAttribute");
        div.setText("The custom value in the session is: " + customAttribute);
        add(div);

        addViewId();
    }
}
