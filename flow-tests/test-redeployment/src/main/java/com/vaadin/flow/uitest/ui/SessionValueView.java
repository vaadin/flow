package com.vaadin.flow.uitest.ui;

import java.util.UUID;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

@Route(value = "com.vaadin.flow.uitest.ui.SessionValueView")
public class SessionValueView extends Div {

    // This ensures the view is not serializable
    private Object preventSerialization = new Object();

    private UUID viewId = UUID.randomUUID();

    // Test that session values outside VaadinSession are preserved on reload
    public SessionValueView() {

        final NativeButton triggerButton = new NativeButton("Trigger reload",
                event -> Application.triggerReload());
        triggerButton.setId(WebpackDevServerPortView.TRIGGER_RELOAD_ID);
        add(triggerButton);

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

        div = new Div();
        div.setText("The view id is: " + viewId.toString());
        add(div);

    }
}
