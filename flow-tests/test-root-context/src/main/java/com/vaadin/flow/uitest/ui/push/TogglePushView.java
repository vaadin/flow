package com.vaadin.flow.uitest.ui.push;

import java.util.Timer;
import java.util.TimerTask;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.communication.PushMode;

@Route("com.vaadin.flow.uitest.ui.push.TogglePushView")
public class TogglePushView extends Div {
    private final Div counterLabel = new Div();
    private int counter = 0;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        updateCounter();
        add(counterLabel);
        counterLabel.setId("counter");

        UI ui = attachEvent.getUI();
        ui.getPushConfiguration()
                .setPushMode("disabled".equals(VaadinRequest.getCurrent().getParameter("push"))
                        ? PushMode.DISABLED
                        : PushMode.AUTOMATIC);

        NativeButton pushSetting = new NativeButton();
        pushSetting.setId("push-setting");
        if (ui.getPushConfiguration().getPushMode().isEnabled()) {
            pushSetting.setText("Push enabled, click to disable");
            ComponentUtil.setData(pushSetting, Boolean.class, Boolean.FALSE);
        } else {
            pushSetting.setText("Push disabled, click to enable");
            ComponentUtil.setData(pushSetting, Boolean.class, Boolean.TRUE);
        }
        pushSetting.addClickListener(event -> {
            Boolean data = ComponentUtil.getData(pushSetting, Boolean.class);
            if (data) {
                ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
                pushSetting.setText("Push enabled, click to disable");
            } else {
                ui.getPushConfiguration().setPushMode(PushMode.DISABLED);
                pushSetting.setText("Push disabled, click to enable");
            }
            ComponentUtil.setData(pushSetting, Boolean.class, !data);
        });
        add(pushSetting);

        NativeButton counter = new NativeButton("Update counter now",
                event -> updateCounter());
        counter.setId("update-counter");
        add(counter);

        NativeButton updateCounterAsync = new NativeButton(
                "Update counter in 1 sec", event -> {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            ui.access(() -> updateCounter());
                        }
                    }, 1000);
                });
        add(updateCounterAsync);
        updateCounterAsync.setId("update-counter-async");
    }

    private void updateCounter() {
        counterLabel
                .setText("Counter has been updated " + counter++ + " times");
    }

}
