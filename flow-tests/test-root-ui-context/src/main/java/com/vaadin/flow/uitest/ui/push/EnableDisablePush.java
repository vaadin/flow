package com.vaadin.flow.uitest.ui.push;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.communication.PushMode;

public class EnableDisablePush extends UI {

    private int c = 0;

    private final Timer timer = new Timer(true);

    private int logCount;

    private final class CounterTask extends TimerTask {

        @Override
        public void run() {

            try {
                while (true) {
                    TimeUnit.MILLISECONDS.sleep(500);

                    access(() -> {
                        log("Counter = " + c++);
                        if (c == 3) {
                            log("Disabling polling, enabling push");
                            getPushConfiguration()
                                    .setPushMode(PushMode.AUTOMATIC);
                            setPollInterval(-1);
                            log("Polling disabled, push enabled");
                        }
                    });
                    if (c == 3) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
            } catch (UIDetachedException e) {
            }
        }
    }

    @Override
    protected void init(VaadinRequest request) {

        getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        log("Push enabled");

        add(createButton("Disable push", "disable-push", () -> {
            log("Disabling push");
            getPushConfiguration().setPushMode(PushMode.DISABLED);
            log("Push disabled");
        }));

        add(createButton("Enable push", "enable-push", () -> {
            log("Enabling push");
            getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
            log("Push enabled");
        }));

        add(createButton("Disable polling", "disable-polling", () -> {
            log("Disabling poll");
            setPollInterval(-1);
            log("Poll disabled");
        }));

        add(createButton("Enable polling", "enable-polling", () -> {
            log("Enabling poll");
            setPollInterval(1000);
            log("Poll enabled");
        }));

        add(createButton("Disable push, re-enable from background thread",
                "thread-re-enable-push", () -> {
                    log("Disabling push, enabling polling");
                    getPushConfiguration().setPushMode(PushMode.DISABLED);
                    setPollInterval(1000);
                    timer.schedule(new CounterTask(), new Date());
                    log("Push disabled, polling enabled");
                }));

    }

    private NativeButton createButton(String caption, String id,
            Runnable action) {
        NativeButton button = new NativeButton(caption);
        button.setId(id);
        button.addClickListener(event -> action.run());
        return button;
    }

    private void log(String msg) {
        Div div = new Div();
        div.addClassName("log");
        logCount++;
        div.setText(logCount + ". " + msg);
    }

}