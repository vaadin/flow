package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.Component;

public class ReconnectLongPollingIT extends ReconnectTest {

    @Override
    protected Class<? extends Component> getViewClass() {
        return BasicPushLongPollingView.class;
    }

}
