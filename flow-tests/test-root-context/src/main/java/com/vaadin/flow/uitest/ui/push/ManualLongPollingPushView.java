package com.vaadin.flow.uitest.ui.push;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Route("com.vaadin.flow.uitest.ui.push.ManualLongPollingPushView")
public class ManualLongPollingPushView extends AbstractTestViewWithLog {

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.getPushConfiguration().setPushMode(PushMode.MANUAL);
        ui.getPushConfiguration().setTransport(Transport.LONG_POLLING);
        NativeButton manualPush = new NativeButton("Manual push after 1s",
                event -> {
                    executor.submit(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ui.access(() -> {
                            log("Logged after 1s, followed by manual push");
                            ui.push();
                        });
                    });
                });
        manualPush.setId("manaul-push");
        add(manualPush);

        manualPush = new NativeButton("Double manual push after 1s", event -> {
            executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ui.access(() -> {
                    log("First message logged after 1s, followed by manual push");
                    ui.push();
                    log("Second message logged after 1s, followed by manual push");
                    ui.push();
                });
            });
        });
        manualPush.setId("double-manual-push");
        add(manualPush);
    }

}
