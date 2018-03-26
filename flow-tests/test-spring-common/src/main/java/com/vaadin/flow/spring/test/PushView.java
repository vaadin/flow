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

    private final class SayWorld implements Runnable {
        private final UI ui;

        private SayWorld(UI ui) {
            this.ui = ui;
        }

        @Override
        public void run() {
            try {
                // Needed to make sure that this is sent as a push message
                Thread.sleep(100);
            } catch (InterruptedException e1) {
            }
            ui.access(() -> {
                Paragraph world = new Paragraph("World");
                world.setId("world");
                add(world);
            });
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();

        // Fallback transport is forced to websocket so that we either get a
        // websocket connection or no push connection at all
        ui.getPushConfiguration().setFallbackTransport(Transport.WEBSOCKET);
        add(new NativeButton("Say hello", e -> {
            add(new Paragraph("Hello"));
            new Thread(new SayWorld(ui)).start();
        }));
    }
}
