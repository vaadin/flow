package com.vaadin.flow.uitest.ui.push;

import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.testcategory.PushTests;

@Category(PushTests.class)
public class IdlePushChannelWebsocketIT extends IdlePushChannelIT {

    @Override
    protected Class<? extends UI> getUIClass() {
        return BasicPushWebsocketUI.class;
    }
}
