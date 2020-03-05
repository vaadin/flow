package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.UI;

public class ReconnectLongPollingIT extends ReconnectTest {

    @Override
    protected Class<? extends UI> getUIClass() {
        return BasicPushLongPollingUI.class;
    }

}