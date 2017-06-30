package com.vaadin.ui;

import com.vaadin.annotations.DomEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

public interface HasClickListeners extends ComponentEventNotifier {

    default Registration addClickListener(
            ComponentEventListener<ClickEvent<?>> listener) {
        return addListener(ClickEvent.class, (ComponentEventListener) listener);
    }

    @DomEvent("click")
    public static class ClickEvent<C extends Component>
            extends ComponentEvent<C> {

        public ClickEvent(C source, boolean fromClient) {
            super(source, fromClient);
        }
    }

}
