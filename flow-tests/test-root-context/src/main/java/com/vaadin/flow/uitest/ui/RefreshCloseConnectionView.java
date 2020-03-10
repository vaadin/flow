package com.vaadin.flow.uitest.ui;

import java.util.Objects;
import java.util.Set;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Push
@PreserveOnRefresh
@Route("com.vaadin.flow.uitest.ui.RefreshCloseConnectionView")
public class RefreshCloseConnectionView extends Div
        implements BeforeEnterObserver {

    private String param = "";

    private int count;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        /*
         * Workaround for "restartApplication" parameter usage: I wasn't able to
         * get it working properly.
         *
         * This code shows "init" of first load and it logs more messages on
         * view refresh (even though the view preserves its state in refresh).
         *
         * It's so complicated because IT should be able to completely "reset"
         * the state of the view between tests execution. To be able to do that
         * a parameter is used: if parameter is different then view is
         * recreated.
         */
        Set<String> params = event.getLocation().getQueryParameters()
                .getParameters().keySet();
        String newParam = null;
        if (!params.isEmpty()) {
            newParam = params.iterator().next();
        }
        boolean isInitial = false;
        if (Objects.equals(param, newParam)) {
            if (count == 0) {
                isInitial = true;
            }
            count++;
        } else {
            removeAll();
            param = newParam;
            count = 1;
            isInitial = true;
        }
        UI ui = event.getUI();
        if (isInitial) {
            log("Init");
        } else {
            if (ui.getInternals().getPushConnection().isConnected()) {
                log("Still connected");
            }
            log("Refresh");
            new Thread() {
                @Override
                public void run() {
                    ui.accessSynchronously(() -> log("Push"));
                }
            }.start();
        }
    }

    private void log(String msg) {
        Div div = new Div();
        div.setText(msg);
        div.addClassName("log");
        add(div);
    }

}
