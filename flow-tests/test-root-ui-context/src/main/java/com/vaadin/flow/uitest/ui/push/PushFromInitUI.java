package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;

@Push
public class PushFromInitUI extends AbstractTestUIWithLog {

    public static final String LOG_DURING_INIT = "Logged from access run before init ends";
    public static final String LOG_AFTER_INIT = "Logged from background thread run after init has finished";

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);
        log("Logged in init");
        Thread t = new Thread(new RunBeforeInitEnds());
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        new Thread(new RunAfterInit()).start();
        add(new NativeButton("Sync"));
    }

    class RunBeforeInitEnds implements Runnable {
        @Override
        public void run() {
            access(() -> log(LOG_DURING_INIT));
        }
    }

    class RunAfterInit implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            access(() -> log(LOG_AFTER_INIT));
        }
    }

}