package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.springframework.context.event.EventListener;

@SpringComponent
public class ConfigureUIServiceInitListener
        implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> uiEvent.getUI()
                .add(new ComponentAddedViaInitListenerView()));
    }

    @EventListener
    public void myListener(ServiceInitEvent event) {
        var p = new Paragraph("Init Listener Component via EventListener");
        p.setId("event-listener");
        event.getSource().addUIInitListener(uiEvent -> uiEvent.getUI().add(p));
    }
}
