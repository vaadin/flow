/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.UIsCollectedWithBeaconAPIView")
public class UIsCollectedWithBeaconAPIView extends Div {

    static int viewcount = 0;

    Div count = new Div();

    public UIsCollectedWithBeaconAPIView() {
        viewcount++;
        add(count);
        count.setId("uis");
        NativeButton showUisNumber = new NativeButton("Update",
                event -> updateCount());
        add(showUisNumber);
        updateCount();
    }

    private void updateCount() {
        count.setText("" + viewcount);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        viewcount--;
    }

}
