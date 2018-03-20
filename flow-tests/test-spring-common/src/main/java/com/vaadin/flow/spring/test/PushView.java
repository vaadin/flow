package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

@Route("push")
@Push(transport = Transport.WEBSOCKET)
public class PushView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.getPushConfiguration().setFallbackTransport(Transport.WEBSOCKET);
        add(new NativeButton("Say hello", e -> {
            add(new Paragraph("Hello"));
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
                ui.access(() -> {
                    add(new Paragraph("World"));
                });
            }).start();
        }));
    }
}
