package com.vaadin.flow.uitest.ui.push;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.push.PushConfiguration")
public class PushConfiguration extends Div {

    private int counter = 0;
    private int counter2 = 0;
    private final Timer timer = new Timer(true);

    private final TimerTask task = new TimerTask() {

        @Override
        public void run() {
            getUI().get().access(() -> {
                counter2++;
                serverCounterLabel.setText("" + counter2);
            });
        }
    };
    private Div serverCounterLabel;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        add(new PushConfigurator(this));
        spacer();

        /*
         * Client initiated push.
         */
        Div clientCounterLabel = new Div();
        clientCounterLabel.setText("0");
        clientCounterLabel.setId("client-counter");
        NativeLabel label = new NativeLabel(
                "Client counter (click 'increment' to update):");
        label.setFor(clientCounterLabel);
        add(label, clientCounterLabel);

        add(new NativeButton("Increment", event -> {
            counter++;
            clientCounterLabel.setText("" + counter);
        }));

        spacer();

        serverCounterLabel = new Div();
        serverCounterLabel.setId("server-counter");
        serverCounterLabel.setText(String.valueOf(counter2));
        label = new NativeLabel(
                "Server counter (updates each 1s by server thread) :");
        label.setFor(serverCounterLabel);
        add(label, serverCounterLabel);

        add(new NativeButton("Reset", event -> {
            counter2 = 0;
            serverCounterLabel.setText("0");
        }));

        timer.scheduleAtFixedRate(task, new Date(), 1000);
    }

    private void spacer() {
        add(new Html("<hr/>"));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        timer.cancel();
    }
}
