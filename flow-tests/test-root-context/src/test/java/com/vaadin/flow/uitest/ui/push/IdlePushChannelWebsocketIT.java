package com.vaadin.flow.uitest.ui.push;

import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testcategory.PushTests;

@Category({ PushTests.class, IgnoreOSGi.class })
public class IdlePushChannelWebsocketIT extends IdlePushChannelIT {

    @Override
    protected Class<? extends Component> getViewClass() {
        return BasicPushWebsocketView.class;
    }
}
