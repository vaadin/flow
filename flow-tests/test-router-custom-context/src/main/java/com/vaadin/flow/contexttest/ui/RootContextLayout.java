package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

public class RootContextLayout extends Div implements RouterLayout {

    public static void setupPush() {
        String transportName = VaadinRequest.getCurrent().getParameter("transport");
        Transport transport = Transport.getByIdentifier(transportName);
        if (transport != null) {
            PushConfiguration pushConfiguration = UI.getCurrent().getPushConfiguration();
            pushConfiguration.setPushMode(PushMode.MANUAL);
            pushConfiguration.setTransport(transport);
            Transport fallbackTransport = transport == Transport.WEBSOCKET_XHR ? Transport.WEBSOCKET: transport;
            pushConfiguration.setFallbackTransport(fallbackTransport);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        setupPush();
    }

    @Route(value = "", layout = RootContextLayout.class)
    public static class RootSubLayout extends DependencyLayout {
        public RootSubLayout() {
            getElement().appendChild(ElementFactory.createDiv("Root Layout")
                    .setAttribute("id", "root"));
        }

    }
    @Route(value = "sub-context", layout = RootContextLayout.class)
    public static class SubContextLayout extends DependencyLayout implements HasUrlParameter<String> {

        public SubContextLayout() {
            getElement().appendChild(ElementFactory.createDiv("Sub Context Layout")
                    .setAttribute("id", "sub"));
        }

        @Override
        public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
            //ignored
        }
    }

}
