package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

import java.util.concurrent.locks.Lock;

@Route("push")
public class PushView extends Div {

    private final class SayWorld implements Runnable {
        private final UI ui;

        private SayWorld(UI ui) {
            this.ui = ui;
        }

        @Override
        public void run() {
            // We can acquire the lock after the request started this thread is processed
            // Needed to make sure that this is sent as a push message
            Lock lockInstance = ui.getSession().getLockInstance();
            lockInstance.lock();
            lockInstance.unlock();

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
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);

        // Fallback transport is forced to websocket so that we either get a
        // websocket connection or no push connection at all
        ui.getPushConfiguration().setFallbackTransport(Transport.WEBSOCKET);
        add(new NativeButton("Say hello", e -> {
            add(new Paragraph("Hello"));
            new Thread(new SayWorld(ui)).start();
        }));
    }
}
