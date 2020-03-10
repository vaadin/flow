package com.vaadin.flow.uitest.ui.push;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Push(value = PushMode.MANUAL, transport = Transport.LONG_POLLING)
public class ManualLongPollingPushUI extends AbstractTestUIWithLog {

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);
        NativeButton manualPush = new NativeButton("Manual push after 1s",
                event -> {
                    executor.submit(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        access(() -> {
                            log("Logged after 1s, followed by manual push");
                            push();
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
                access(() -> {
                    log("First message logged after 1s, followed by manual push");
                    push();
                    log("Second message logged after 1s, followed by manual push");
                    push();
                });
            });
        });
        manualPush.setId("double-manual-push");
        add(manualPush);
    }

}
